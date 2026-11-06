package com.rblh.cu;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import com.github.junrar.Archive;
import com.github.junrar.Junrar;
import com.github.junrar.rarfile.FileHeader;
import com.rblh.cu.Core;
import com.rblh.cu.Installer;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MainActivity extends Activity {
    private static final String INTRO_ENC = "penhj8+d1ImAxsHug8bdqOL/2oSL2Z/jht7OkMHil6Kb1o2Yi9/zpeH7j8aL1bSzxsHbhcP3R22Urpzai4yL2dCnzcCC97PYnI7EwMOA18yi2/7Wia3Sm+KH48Kd49+XlqbWioOM4uRJncbCl5+o16qLiMXnq/LngN2T1qaPxef0g+fjqdvS2pG+0pvVjdrPnfnElomL2rGJh9b5oPXPbXjUrJ7TlMqH4/SR9uKVppjWjK2K7+mm/+SC3LvYkbPH1+yPxviq2tzRsLA8y9LNgvjDqu761K2x0579h+jckfD7mrK1Et6oyoX//pH2w5W5oBLQqu+E+sqWzeV4ONi5k8fSwoDb2qnc6tqEi96t2IfY1JPRy5qPn9aNlYj44qzJwY/dhdeZvcbi0Y/D9avLzNeJhNKb0YTs5pbN5Q==";
    private static final int REQ_BEP_ZIP = 1002;
    private static final int REQ_EXPORT = 1007;
    private static final int REQ_GAME_ZIP = 1001;
    private static final int REQ_IMPORT_SAVE = 1006;
    private static final int REQ_LANG_ZIP = 1004;
    private static final int REQ_MOD_ZIP = 1003;
    private static final int REQ_RESTORE = 1005;
    private static final String URL_AFDIAN_ENC = "KwE5FwEIHx1XRQsLBy1bLggfHVEdREEDClRxTHheSgMIBAA=";
    private static final String URL_AUTHOR_ENC = "KwE5FwEIHx1UEVxMEjVaPgYHVWkGUg==";
    private static final String URL_NEXUS = "https://www.nexusmods.com/games/scavprototype";
    private FrameLayout shell;
    private SharedPreferences sp;
    private static final String[][] WIN_PKGS = {new String[]{"com.andromeda.androbench2"}, new String[]{"com.winlator.cmod", "com.winlator"}, new String[]{"com.xiaoji.gamehub", "com.gamesir.gamehub", "com.xiaoji.egggame"}};
    private static final String[] WIN_NAMES = {"Bionic Hub", "Winlator CN", "盖世游戏"};
    private static JSONObject langCache = null;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean wantBep = false;
    private int curTab = 0;
    private boolean bepPromptShown = false;
    private boolean pendingPermRefresh = false;
    private File srcFile = null;
    private Uri srcUri = null;
    private String srcAsset = null;
    private int selWinlator = 0;
    private HighlightList wlList = null;
    private final List<TextView> wlRadios = new ArrayList();
    private File pendingExport = null;

    private void handleIncoming(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if ("android.intent.action.VIEW".equals(action)) {
            if (intent.getData() != null) {
                importFile(intent.getData(), true);
            }
        } else if ("android.intent.action.SEND".equals(action)) {
            Parcelable parcelableExtra = intent.getParcelableExtra("android.intent.extra.STREAM");
            if (parcelableExtra instanceof Uri) {
                importFile((Uri) parcelableExtra, true);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncoming(intent);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        installCrashHandler();
        Theme.init(this);
        super.onCreate(bundle);
        this.sp = Core.prefs(this);
        Core.loadCustomDir(this);
        Ui.styleSystemBars(this);
        try {
            getWindow().setBackgroundDrawable(new ColorDrawable(Ui.BG));
        } catch (Throwable th) {
        }
        if (this.sp.getBoolean("setup_done", false)) {
            try {
                showMain(0);
            } catch (Throwable th) {
                crashPage(th);
            }
        } else {
            showHome();
        }
        handleIncoming(getIntent());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Core.isRootPath()) {
                        Root.available();
                    }
                } catch (Throwable th2) {
                }
            }
        }).start();
    }

    private void setPage(View view) {
        setPage(view, true);
        Skin.apply(this);
    }

    private void setPage(final View view, boolean z) {
        final View page = wrapPage(view);
        if (this.shell == null) {
            this.shell = new FrameLayout(this);
            this.shell.setBackground(Ui.gradient(Ui.BG2, Ui.BG, 0.0f));
            setContentView(this.shell);
        }
        this.shell.removeAllViews();
        this.shell.addView(page, new FrameLayout.LayoutParams(-1, -1));
        Ui.animateIn(page);
        Skin.applyLater(this);
    }

    private View wrapPage(View view) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.addView(view, new FrameLayout.LayoutParams(-1, -1));
        int i = getResources().getDisplayMetrics().widthPixels;
        int iDp = Ui.dp(this, 560.0f);
        if (i > iDp) {
            int i2 = (i - iDp) / 2;
            wrap.setPadding(i2, 0, i2, 0);
        }
        return wrap;
    }

    public void showHome() {
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.setGravity(1);
        TextView textViewTv = Ui.tv(this, "CU Installer", 34.0f, Theme.accent());
        textViewTv.setGravity(17);
        textViewTv.setPadding(0, Ui.dp(this, 48.0f), 0, 0);
        linearLayoutColumn.addView(textViewTv);
        TextView textViewTv2 = Ui.tv(this, "未知伤亡管理器", 15.0f, Ui.SUB);
        textViewTv2.setGravity(17);
        textViewTv2.setPadding(0, Ui.dp(this, 6.0f), 0, Ui.dp(this, 10.0f));
        linearLayoutColumn.addView(textViewTv2);
        TextView textViewTv3 = Ui.tv(this, "by 若白令狐", 12.0f, Ui.SUB2);
        textViewTv3.setGravity(17);
        textViewTv3.setLetterSpacing(0.05f);
        textViewTv3.setPadding(0, 0, 0, Ui.dp(this, 32.0f));
        linearLayoutColumn.addView(textViewTv3);
        LinearLayout linearLayoutCard = Ui.card(this);
        linearLayoutCard.addView(Ui.cardHeader(this, "本软件完全免费", null));
        linearLayoutCard.addView(Ui.desc(this, Obf.intact("本软件完全免费，不包含任何收费内容。\n\n本软件已开源（MIT 许可），可自由修改与分发；游戏本体不随软件提供，请自行合法获取。\n\n本工具仅用于协助安装与配置。\n软件版权归原作者 若白令狐 所有。\n\n若你是付费获得本软件的，请立即退款并举报。") ? "本软件完全免费，不包含任何收费内容。\n\n本软件已开源（MIT 许可），可自由修改与分发；游戏本体不随软件提供，请自行合法获取。\n\n本工具仅用于协助安装与配置。\n软件版权归原作者 若白令狐 所有。\n\n若你是付费获得本软件的，请立即退款并举报。" : "本软件内容校验失败，可能已被篡改。\n请从作者处重新获取。"));
        linearLayoutColumn.addView(linearLayoutCard);
        Button buttonBtn = Ui.btn(this, "作者主页", Ui.CARD2);
        buttonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.openUrl(Obf.d(MainActivity.URL_AUTHOR_ENC));
            }
        });
        linearLayoutColumn.addView(buttonBtn);
        Button buttonBtn2 = Ui.btn(this, "进入软件", Theme.accentDark());
        buttonBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.showPermission();
            }
        });
        linearLayoutColumn.addView(buttonBtn2);
        setPage(Ui.scroll(this, linearLayoutColumn));
    }

    public void showPermission() {
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.addView(header("授 权"));
        boolean zHasAllFiles = hasAllFiles();
        LinearLayout linearLayoutCard = Ui.card(this);
        linearLayoutCard.addView(Ui.cardHeader(this, "所有文件访问", "安装游戏要把文件解压到手机存储的 Download 目录，\n需要这个权限才能写入。\n\n只需授权一次，之后不会再问。"));
        linearLayoutCard.addView(Ui.desc(this, "安装游戏 / BepInEx 需要写入手机存储。\n只需授权一次，之后不会再问。"));
        TextView textViewTv = Ui.tv(this, zHasAllFiles ? "✅ 已授权，可以继续" : "❌ 尚未授权", 15.0f, zHasAllFiles ? Theme.accent() : Ui.DANGER);
        textViewTv.setPadding(0, Ui.dp(this, 12.0f), 0, 0);
        linearLayoutCard.addView(textViewTv);
        linearLayoutColumn.addView(linearLayoutCard);
        int iAccentDark = Theme.accentDark();
        if (!zHasAllFiles) {
            Button buttonBtn = Ui.btn(this, "去授权", iAccentDark);
            buttonBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.askAllFiles();
                }
            });
            linearLayoutCard.addView(buttonBtn);
        }
        if (!zHasAllFiles) {
            iAccentDark = Ui.CARD2;
        }
        Button buttonBtn2 = Ui.btn(this, "下一步", iAccentDark);
        buttonBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean zHasAllFiles2 = MainActivity.this.hasAllFiles();
                MainActivity mainActivity = MainActivity.this;
                if (zHasAllFiles2) {
                    mainActivity.showInstall();
                } else {
                    Ui.alert(mainActivity, "还没授权", "请先点「去授权」，在系统设置里打开「所有文件访问」开关，再回来点下一步。");
                }
            }
        });
        linearLayoutColumn.addView(buttonBtn2);
        linearLayoutColumn.addView(Ui.desc(this, "提示：不同手机这个开关的位置可能不同，\n一般在「设置 → 应用 → 特殊权限」里。"));
        setPage(Ui.scroll(this, linearLayoutColumn));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Skin.apply(this);
        if (this.sp != null && !this.sp.getBoolean("setup_done", false) && this.pendingPermRefresh) {
            this.pendingPermRefresh = false;
            if (!hasAllFiles()) {
                showPermission();
            } else {
                showInstall();
            }
        }
    }

    public void showInstall() {
        String str;
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.addView(header("安 装 游 戏"));
        LinearLayout linearLayoutCard = Ui.card(this);
        linearLayoutCard.addView(Ui.cardHeader(this, "Casualties Unknown Demo", "游戏本体需要你自己准备。\n压缩包会解压到 /storage/emulated/0/Download/ 下。"));
        boolean zHasGame = Core.hasGame();
        if (zHasGame) {
            str = "状态：✅ 已在所选目录找到游戏\n无需重新安装，直接点下面的「下一步」即可";
        } else {
            str = "状态：尚未安装\n\n本安装器不包含游戏本体。请自备游戏压缩包：放进 /Download/，或点下面的「安装」手动选择。将解压到：\n" + Core.gameDir().getAbsolutePath() + "\n\n已经装在别处了？点「已安装游戏？」选择你现有的目录。";
        }
        linearLayoutCard.addView(Ui.desc(this, str));
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText("游戏本体（需自备）");
        checkBox.setChecked(true);
        checkBox.setEnabled(false);
        checkBox.setTextColor(Ui.SUB);
        linearLayoutCard.addView(checkBox);
        linearLayoutColumn.addView(linearLayoutCard);
        LinearLayout linearLayoutCard2 = Ui.card(this);
        linearLayoutCard2.addView(Ui.cardHeader(this, "BepInEx", "安装模组必需的前置框架。\n不装只能玩原版，装了才能加载 MOD。"));
        linearLayoutCard2.addView(Ui.desc(this, "状态：" + (Core.hasBepInEx() ? "✅ 已安装" : "未安装")));
        final CheckBox checkBox2 = new CheckBox(this);
        checkBox2.setText("安装 BepInEx（可选）");
        checkBox2.setChecked(this.wantBep);
        checkBox2.setTextColor(Ui.TEXT);
        checkBox2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.wantBep = checkBox2.isChecked();
            }
        });
        linearLayoutCard2.addView(checkBox2);
        linearLayoutColumn.addView(linearLayoutCard2);
        Button buttonBtn = Ui.btn(this, "已安装游戏？", Ui.CARD2);
        buttonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.showGameDirDialog(null, false);
            }
        });
        linearLayoutColumn.addView(buttonBtn);
        Button buttonBtn2 = Ui.btn(this, zHasGame ? "下一步  →" : "安装", Theme.accentDark());
        buttonBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean zHasGame2 = Core.hasGame();
                MainActivity mainActivity = MainActivity.this;
                if (zHasGame2) {
                    mainActivity.showWinlator();
                } else {
                    mainActivity.startGameInstall();
                }
            }
        });
        linearLayoutColumn.addView(buttonBtn2);
        Button buttonBtn3 = Ui.btn(this, "跳过", Ui.CARD2);
        buttonBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this).setTitle("确认跳过？").setMessage("跳过后不会安装游戏文件，之后需要手动把游戏放到\n/Download/Casualties Unknown_7.0.1Demo/\n里，否则无法游戏。").setPositiveButton("仍要跳过", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity.this.showWinlator();
                    }
                }).setNegativeButton("返回安装", (DialogInterface.OnClickListener) null).show();
            }
        });
        linearLayoutColumn.addView(buttonBtn3);
        setPage(Ui.scroll(this, linearLayoutColumn));
    }

    public void startGameInstall() {
        if (!hasAllFiles()) {
            askAllFiles();
            return;
        }
        if (hasAsset("game.zip")) {
            this.srcAsset = "game.zip";
            this.srcFile = null;
            this.srcUri = null;
            deployGame();
            return;
        }
        File fileFindPackage = Core.findPackage(Core.GAME_ZIP_NAMES);
        if (fileFindPackage == null) {
            promptPick(REQ_GAME_ZIP, "没有找到游戏压缩包，请手动选择", "游戏本体压缩包");
            return;
        }
        this.srcAsset = null;
        this.srcFile = fileFindPackage;
        this.srcUri = null;
        deployGame();
    }

    private void promptPick(final int i, String str, String str2) {
        new AlertDialog.Builder(this).setTitle(str).setMessage("请把 " + str2 + " 下载到 /storage/emulated/0/Download/ 后重试，\n或者直接从手机里选一个 zip 文件。").setPositiveButton("选择文件", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                MainActivity.this.pickZip(i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    public void pickZip(int i) {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        try {
            startActivityForResult(intent, i);
        } catch (Throwable th) {
            Ui.alert(this, "无法打开文件选择器", String.valueOf(th));
        }
    }

    public InputStream openSrc() throws IOException {
        if (this.srcAsset != null) {
            return getAssets().open(this.srcAsset);
        }
        if (this.srcFile != null) {
            return new FileInputStream(this.srcFile);
        }
        InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(this.srcUri);
        if (inputStreamOpenInputStream == null) {
            throw new IOException("无法读取文件");
        }
        return inputStreamOpenInputStream;
    }

    private boolean hasAsset(String str) {
        try {
            InputStream inputStreamOpen = getAssets().open(str);
            if (inputStreamOpen != null) {
                try {
                    inputStreamOpen.close();
                    return true;
                } catch (Throwable th) {
                    return true;
                }
            }
            return true;
        } catch (Throwable th2) {
            return false;
        }
    }

    private void deployGame() {
        View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在解压游戏…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        TextView textView = (TextView) viewArrBuildProgressOverlay[1];
        ((ProgressBar) viewArrBuildProgressOverlay[2]).setIndeterminate(true);
        new Thread(new AnonymousClass12(Core.gameDir(), textView, viewArrBuildProgressOverlay)).start();
    }

    
    class AnonymousClass12 implements Runnable {
        final /* synthetic */ File val$dest;
        final /* synthetic */ View[] val$overlay;
        final /* synthetic */ TextView val$ptxt;

        AnonymousClass12(File file, TextView textView, View[] viewArr) {
            this.val$dest = file;
            this.val$ptxt = textView;
            this.val$overlay = viewArr;
        }

        @Override
        public void run() {
            InputStream inputStream = null;
            try {
                try {
                    this.val$dest.mkdirs();
                    InputStream inputStreamOpenSrc = MainActivity.this.openSrc();
                    try {
                        final String strDetectSingleTopDir = Installer.detectSingleTopDir(inputStreamOpenSrc);
                        try {
                            inputStreamOpenSrc.close();
                        } catch (Throwable th) {
                        }
                        InputStream inputStreamOpenSrc2 = MainActivity.this.openSrc();
                        final long[] jArr = {0};
                        Installer.unzip(inputStreamOpenSrc2, this.val$dest, strDetectSingleTopDir, new Installer.Progress() {
                            @Override
                            public void onProgress(long j, String str) {
                                String str2;
                                long jCurrentTimeMillis = System.currentTimeMillis();
                                if (jCurrentTimeMillis - jArr[0] > 300) {
                                    jArr[0] = jCurrentTimeMillis;
                                    if (strDetectSingleTopDir == null || strDetectSingleTopDir.length() > 0) {
                                        str2 = "已解压 " + Core.human(j) + "\n" + str;
                                    } else {
                                        str2 = "已解压 " + Core.human(j) + "\n" + str;
                                    }
                                    MainActivity.this.ui.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            AnonymousClass12.this.val$ptxt.setText(str2);
                                        }
                                    });
                                }
                            }
                        });
                        MainActivity.this.ui.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.afterGame();
                            }
                        });
                        if (inputStreamOpenSrc2 != null) {
                            inputStreamOpenSrc2.close();
                        }
                    } catch (Throwable th2) {
                        try {
                            inputStreamOpenSrc.close();
                        } catch (Throwable th3) {
                        }
                        throw th2;
                    }
                } catch (Throwable th4) {
                }
            } catch (Throwable th5) {
                try {
                    Log.e("CUImport", "deploy failed: " + th5, th5);
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.shell.removeView(AnonymousClass12.this.val$overlay[0]);
                            Ui.alert(MainActivity.this, "解压失败", String.valueOf(th5));
                        }
                    });
                    if (0 != 0) {
                        inputStream.close();
                    }
                } catch (Throwable th6) {
                    if (0 != 0) {
                        try {
                            inputStream.close();
                        } catch (Throwable th7) {
                        }
                    }
                    throw new RuntimeException(th6);
                }
            }
        }
    }

    public void afterGame() {
        if (this.wantBep) {
            if (hasAsset("bepinex.zip")) {
                try {
                    Installer.installBepInEx(getAssets().open("bepinex.zip"), null);
                    showWinlator();
                    return;
                } catch (Throwable th) {
                    Log.e("CUImport", "bepinex asset failed", th);
                }
            }
            File fileFindPackage = Core.findPackage(Core.BEPINEX_ZIP_NAMES);
            if (fileFindPackage == null) {
                showWinlator();
                Ui.alert(this, "BepInEx 未找到", "请把 BepInEx.zip 放到 /Download/ 后，在「模组」页面重新导入。");
                return;
            } else {
                deployBep(fileFindPackage);
                return;
            }
        }
        showWinlator();
    }

    private void deployBep(final File file) {
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在安装 BepInEx…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        ((ProgressBar) viewArrBuildProgressOverlay[2]).setIndeterminate(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Installer.installBepInEx(new FileInputStream(file), new Installer.Progress() {
                        @Override
                        public void onProgress(long j, String str) {
                        }
                    });
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.showWinlator();
                        }
                    });
                } catch (Throwable th) {
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                            Ui.alert(MainActivity.this, "BepInEx 安装失败", String.valueOf(th));
                        }
                    });
                }
            }
        }).start();
    }

    public void showWinlator() {
        showWinlator(true);
    }

    private void showWinlator(boolean z) {
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.addView(header("选 择 运 行 环 境"));
        linearLayoutColumn.addView(Ui.desc(this, "游戏需要 Winlator 才能在手机上运行。请选择一项，然后点底部「安装」："));
        String[][] strArr = {new String[]{"Bionic Hub", "基于 Winlator 的优化分支，使用安兔兔包名，性能表现更好。推荐。"}, new String[]{"Winlator CN", "原版 Winlator 汉化版，兼容性更保守。"}, new String[]{"盖世游戏", "会自动根据设备进行配置，推荐小白使用。"}};
        this.wlRadios.clear();
        this.wlList = new HighlightList(this, Ui.dp(this, 20.0f), Ui.dp(this, 2.0f));
        for (int i = 0; i < 3; i++) {
            LinearLayout linearLayoutCard = Ui.card(this);
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(0);
            linearLayout.setGravity(16);
            TextView textViewTv = Ui.tv(this, "○", 20.0f, Ui.SUB);
            textViewTv.setPadding(0, 0, Ui.dp(this, 12.0f), 0);
            textViewTv.setGravity(17);
            textViewTv.setMinWidth(Ui.dp(this, 26.0f));
            linearLayout.addView(textViewTv);
            this.wlRadios.add(textViewTv);
            TextView textViewTv2 = Ui.tv(this, strArr[i][0], 17.0f, Ui.TEXT);
            textViewTv2.setTypeface(Typeface.DEFAULT_BOLD);
            linearLayout.addView(textViewTv2, new LinearLayout.LayoutParams(0, -2, 1.0f));
            linearLayoutCard.addView(linearLayout);
            linearLayoutCard.addView(Ui.desc(this, strArr[i][1] + (installedPkg(i) != null ? "\n状态：✅ 已安装（点「下一步」继续）" : "\n状态：未安装")));
            final int si = i;
            linearLayoutCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (MainActivity.this.selWinlator == si) {
                        return;
                    }
                    MainActivity.this.selWinlator = si;
                    MainActivity.this.paintRadios(si);
                    MainActivity.this.wlList.animateTo(si, null);
                }
            });
            this.wlList.addView(linearLayoutCard);
        }
        linearLayoutColumn.addView(this.wlList);
        Button buttonBtn = Ui.btn(this, installedPkg(this.selWinlator) != null ? "下一步  →" : "安  装", Theme.accentDark());
        buttonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.chooseWinlator(MainActivity.this.selWinlator);
            }
        });
        linearLayoutColumn.addView(buttonBtn);
        Button buttonBtn2 = Ui.btn(this, "跳过", Ui.CARD2);
        buttonBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this).setTitle("确认跳过？").setMessage("没有 Winlator 就无法运行游戏。\n跳过后可以随时在「设置」里再装。").setPositiveButton("仍要跳过", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        MainActivity.this.sp.edit().putBoolean("setup_done", true).apply();
                        MainActivity.this.showMain(0);
                    }
                }).setNegativeButton("返回", (DialogInterface.OnClickListener) null).show();
            }
        });
        linearLayoutColumn.addView(buttonBtn2);
        paintRadios(this.selWinlator);
        final int i2 = this.selWinlator;
        this.wlList.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.wlList.snapTo(i2);
            }
        });
        setPage(Ui.scroll(this, linearLayoutColumn), z);
    }

    public void paintRadios(int i) {
        int i2 = 0;
        while (i2 < this.wlRadios.size()) {
            TextView textView = this.wlRadios.get(i2);
            boolean z = i2 == i;
            textView.setText(z ? "●" : "○");
            textView.setTextColor(z ? Theme.accent() : Ui.SUB);
            i2++;
        }
    }

    private void installCrashHandler() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable th) {
                try {
                    File dir = new File("/storage/emulated/0/Download/cu-logs");
                    dir.mkdirs();
                    String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(new java.util.Date());
                    File f = new File(dir, "crash-" + ts + ".txt");
                    java.io.StringWriter sw = new java.io.StringWriter();
                    th.printStackTrace(new java.io.PrintWriter(sw));
                    FileOutputStream fos = new FileOutputStream(f);
                    String head = "app: " + getPackageName() + "\n"
                            + "device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\n"
                            + "android: " + android.os.Build.VERSION.RELEASE + " (SDK " + android.os.Build.VERSION.SDK_INT + ")\n"
                            + "thread: " + thread.getName() + "\n\n";
                    fos.write((head + sw.toString()).getBytes("UTF-8"));
                    fos.close();
                } catch (Throwable ignored) {
                }
                if (prev != null) {
                    prev.uncaughtException(thread, th);
                }
            }
        });
    }

    private void crashPage(Throwable th) {
        try {
            LinearLayout col = Ui.column(this);
            col.addView(header("出 错"));
            LinearLayout card = Ui.card(this);
            card.addView(Ui.cardHeader(this, "启动时遇到问题", null));
            card.addView(Ui.desc(this, String.valueOf(th) + "\n\n已记录到 Download/cu-logs/，可反馈给开发者。"));
            col.addView(card);
            Button retry = Ui.btn(this, "重试", Theme.accentDark());
            retry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    recreate();
                }
            });
            col.addView(retry);
            Button skip = Ui.btn(this, "重置向导", Ui.CARD2);
            skip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sp.edit().clear().apply();
                    recreate();
                }
            });
            col.addView(skip);
            setPage(Ui.scroll(this, col));
        } catch (Throwable ignored) {
        }
    }

    private TextView chip(String str, int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(13.0f);
        textView.setTextColor(-1);
        textView.setGravity(17);
        textView.setIncludeFontPadding(false);
        textView.setBackground(Ui.round(i, Ui.dp(this, 18.0f)));
        textView.setPadding(Ui.dp(this, 16.0f), Ui.dp(this, 9.0f), Ui.dp(this, 16.0f), Ui.dp(this, 9.0f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.setMargins(Ui.dp(this, 10.0f), 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private String installedPkg(int i) {
        for (String str : WIN_PKGS[i]) {
            if (pkgInstalled(str)) {
                return str;
            }
        }
        return null;
    }

    public boolean launchPkg(String str) {
        try {
            Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(str);
            if (launchIntentForPackage == null) {
                return false;
            }
            startActivity(launchIntentForPackage);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private boolean pkgInstalled(String str) {
        try {
            getPackageManager().getPackageInfo(str, 0);
            return true;
        } catch (Throwable th) {
            try {
                return getPackageManager().getLaunchIntentForPackage(str) != null;
            } catch (Throwable th2) {
                return false;
            }
        }
    }

    private void showChooser(String str, String str2, String[] strArr, final Runnable[] runnableArr) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 18.0f)));
        int iDp = Ui.dp(this, 20.0f);
        linearLayout.setPadding(iDp, iDp, iDp, iDp);
        linearLayout.addView(Ui.tv(this, str, 18.0f, Ui.TEXT));
        if (str2 != null) {
            TextView textViewTv = Ui.tv(this, str2, 13.0f, Ui.SUB);
            textViewTv.setPadding(0, Ui.dp(this, 6.0f), 0, 0);
            linearLayout.addView(textViewTv);
        }
        for (int i = 0; i < strArr.length; i++) {
            TextView textViewTv2 = Ui.tv(this, strArr[i], 16.0f, Ui.TEXT);
            textViewTv2.setGravity(17);
            textViewTv2.setPadding(Ui.dp(this, 16.0f), Ui.dp(this, 15.0f), Ui.dp(this, 16.0f), Ui.dp(this, 15.0f));
            textViewTv2.setBackground(Ui.round(Ui.CARD2, Ui.dp(this, 12.0f)));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.setMargins(0, Ui.dp(this, 10.0f), 0, 0);
            textViewTv2.setLayoutParams(layoutParams);
            final int si = i;
            textViewTv2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    runnableArr[si].run();
                }
            });
            linearLayout.addView(textViewTv2);
        }
        TextView textViewTv3 = Ui.tv(this, "取消", 15.0f, Ui.SUB);
        textViewTv3.setGravity(17);
        textViewTv3.setPadding(0, Ui.dp(this, 16.0f), 0, Ui.dp(this, 4.0f));
        textViewTv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        linearLayout.addView(textViewTv3);
        dialog.setContentView(linearLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.88f), -2);
        }
        dialog.show();
    }

    public void doBackupSaves() {
        int iBackupLimit = Core.backupLimit(this);
        int size = Core.listBackups().size();
        if (size >= iBackupLimit) {
            new AlertDialog.Builder(this).setTitle("备份已达上限").setMessage("最多保留 " + iBackupLimit + " 份备份，当前已有 " + size + " 份。\n\n请先删除旧备份，或到「设置 → 存档备份上限」调高上限。").setPositiveButton("知道了", (DialogInterface.OnClickListener) null).show();
            return;
        }
        final String str = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在备份存档…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                File fileFindSlotsDir = Core.findSlotsDir();
                boolean z = false;
                if (fileFindSlotsDir != null) {
                    if (Core.isRootPath()) {
                        String absolutePath = new File(Core.backupDir(), str + ".tar.gz").getAbsolutePath();
                        if (Root.execNs("cd " + MainActivity.this.qq(fileFindSlotsDir.getParentFile().getAbsolutePath()) + " && tar -czf " + MainActivity.this.qq(absolutePath) + " slots 2>/dev/null; echo DONE").contains("DONE") && Root.exists(absolutePath)) {
                            z = true;
                        }
                    } else {
                        String strFindSaveSv = Core.findSaveSv();
                        File file = null;
                        if (strFindSaveSv != null) {
                            File file2 = new File(MainActivity.this.getCacheDir(), "save.sv");
                            Root.execNs("cp -f " + MainActivity.this.qq(strFindSaveSv) + " " + MainActivity.this.qq(file2.getAbsolutePath()) + " && chmod 644 " + MainActivity.this.qq(file2.getAbsolutePath()));
                            if (file2.exists() && file2.length() != 0) {
                                file = file2;
                            }
                        }
                        File file3 = new File(Core.backupDir(), str + ".zip");
                        boolean zZipDirWithSave = MainActivity.this.zipDirWithSave(fileFindSlotsDir, file, file3);
                        file3.getAbsolutePath();
                        if (file != null) {
                            file.delete();
                        }
                        z = zZipDirWithSave;
                    }
                }
                final boolean fz = z;
                MainActivity.this.ui.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                        boolean z2 = fz;
                                        if (z2) {
                            Ui.toast(MainActivity.this, "备份完成");
                            MainActivity.this.showMain(0);
                        } else {
                            Ui.alert(MainActivity.this, "备份失败", "找不到存档或无法写入。");
                        }
                    }
                });
            }
        }).start();
    }

    private Button tinyBtn(String str, int i) {
        Drawable drawableRipple;
        Button button = new Button(this);
        button.setText(str);
        button.setAllCaps(false);
        button.setTextSize(12.0f);
        button.setTextColor(i == 0 ? Ui.SUB : -1);
        if (i == 0) {
            drawableRipple = Ui.ripple(this, 0, Ui.dp(this, 10.0f));
        } else {
            drawableRipple = Ui.ripple(this, i, Ui.dp(this, 10.0f));
        }
        button.setBackground(drawableRipple);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, Ui.dp(this, 36.0f));
        layoutParams.setMargins(0, Ui.dp(this, 8.0f), Ui.dp(this, 6.0f), 0);
        button.setLayoutParams(layoutParams);
        button.setPadding(Ui.dp(this, 10.0f), 0, Ui.dp(this, 10.0f), 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        return button;
    }

    public String qq(String str) {
        return "'" + str.replace("'", "'\\''") + "'";
    }

    public boolean zipDirWithSave(File file, File file2, File file3) {
        FileInputStream fileInputStream;
        if (!file.exists()) {
            return false;
        }
        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file3));
            try {
                byte[] bArr = new byte[65536];
                ArrayList arrayList = new ArrayList();
                arrayList.add(file);
                while (!arrayList.isEmpty()) {
                    File[] fileArrListFiles = ((File) arrayList.remove(0)).listFiles();
                    if (fileArrListFiles != null) {
                        for (File file4 : fileArrListFiles) {
                            if (file4.isDirectory()) {
                                arrayList.add(file4);
                            } else {
                                zipOutputStream.putNextEntry(new ZipEntry(file4.getAbsolutePath().substring(file.getParentFile().getAbsolutePath().length() + 1).replace('\\', '/')));
                                fileInputStream = new FileInputStream(file4);
                                while (true) {
                                    try {
                                        int i = fileInputStream.read(bArr);
                                        if (i > 0) {
                                            zipOutputStream.write(bArr, 0, i);
                                        } else {
                                            try {
                                                break;
                                            } catch (Throwable th) {
                                            }
                                        }
                                    } finally {
                                    }
                                }
                                fileInputStream.close();
                                zipOutputStream.closeEntry();
                            }
                        }
                    }
                }
                if (file2 != null && file2.exists()) {
                    zipOutputStream.putNextEntry(new ZipEntry("save/save.sv"));
                    fileInputStream = new FileInputStream(file2);
                    while (true) {
                        try {
                            int i2 = fileInputStream.read(bArr);
                            if (i2 > 0) {
                                zipOutputStream.write(bArr, 0, i2);
                            } else {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                }
                            }
                        } finally {
                        }
                    }
                    fileInputStream.close();
                    zipOutputStream.closeEntry();
                }
                zipOutputStream.close();
                return file3.length() > 0;
            } catch (Throwable th3) {
                zipOutputStream.close();
                throw th3;
            }
        } catch (Throwable th4) {
            Log.e("CUImport", "zipDirWithSave failed", th4);
            return false;
        }
    }

    public boolean zipDir(File file, File file2) {
        if (!file.exists()) {
            return false;
        }
        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file2));
            try {
                ArrayList arrayList = new ArrayList();
                arrayList.add(file);
                byte[] bArr = new byte[65536];
                while (!arrayList.isEmpty()) {
                    File[] fileArrListFiles = ((File) arrayList.remove(0)).listFiles();
                    if (fileArrListFiles != null) {
                        for (File file3 : fileArrListFiles) {
                            if (file3.isDirectory()) {
                                arrayList.add(file3);
                            } else {
                                zipOutputStream.putNextEntry(new ZipEntry(file3.getAbsolutePath().substring(file.getParentFile().getAbsolutePath().length() + 1).replace('\\', '/')));
                                FileInputStream fileInputStream = new FileInputStream(file3);
                                while (true) {
                                    try {
                                        int i = fileInputStream.read(bArr);
                                        if (i > 0) {
                                            zipOutputStream.write(bArr, 0, i);
                                        } else {
                                            try {
                                                break;
                                            } catch (Throwable th) {
                                            }
                                        }
                                    } catch (Throwable th2) {
                                        try {
                                            fileInputStream.close();
                                        } catch (Throwable th3) {
                                        }
                                        throw th2;
                                    }
                                }
                                fileInputStream.close();
                                zipOutputStream.closeEntry();
                            }
                        }
                    }
                }
                zipOutputStream.close();
                return file2.length() > 0;
            } catch (Throwable th4) {
                zipOutputStream.close();
                throw th4;
            }
        } catch (Throwable th5) {
            Log.e("CUImport", "zipDir failed", th5);
            return false;
        }
    }

    public void exportBackup(File file) {
        String str = ".zip";
        this.pendingExport = file;
        try {
            String name = file.getName();
            String strNoteOf = Core.noteOf(file);
            if (strNoteOf != null && strNoteOf.trim().length() > 0) {
                String strReplaceAll = strNoteOf.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
                if (strReplaceAll.length() > 40) {
                    strReplaceAll = strReplaceAll.substring(0, 40);
                }
                if (name.endsWith(".tar.gz")) {
                    name = name.substring(0, name.length() - 7);
                    str = ".tar.gz";
                } else if (name.endsWith(".zip")) {
                    name = name.substring(0, name.length() - 4);
                } else {
                    str = "";
                }
                name = name + "_" + strReplaceAll + str;
            }
            Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
            intent.addCategory("android.intent.category.OPENABLE");
            intent.setType("application/gzip");
            intent.putExtra("android.intent.extra.TITLE", name);
            startActivityForResult(intent, REQ_EXPORT);
        } catch (Throwable th) {
            this.pendingExport = null;
            Ui.alert(this, "无法导出", String.valueOf(th));
        }
    }

    private void doImportSave(final Uri uri) {
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在导入…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                String str;
                FileOutputStream fileOutputStream;
                InputStream inputStreamOpenInputStream;
                long jCurrentTimeMillis;
                try {
                    String strQueryName = MainActivity.this.queryName(uri);
                    String lowerCase = strQueryName.toLowerCase();
                    byte[] bArr = new byte[4];
                    InputStream inputStreamOpenInputStream2 = MainActivity.this.getContentResolver().openInputStream(uri);
                    try {
                        int i = inputStreamOpenInputStream2.read(bArr);
                        try {
                            inputStreamOpenInputStream2.close();
                        } catch (Throwable th) {
                        }
                        boolean zZipDir = false;
                        boolean z = i >= 4 && bArr[0] == 80 && bArr[1] == 75;
                        boolean z2 = i >= 4 && bArr[0] == 82 && bArr[1] == 97;
                        boolean z3 = i >= 2 && (bArr[0] & 255) == 31 && (bArr[1] & 255) == 139;
                        String str2 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        str = "已加入备份列表";
                        if (z || z3) {
                            long j = 0;
                            File file = new File(Core.backupDir(), str2 + (z3 ? ".tar.gz" : ".zip"));
                            fileOutputStream = new FileOutputStream(file);
                            inputStreamOpenInputStream = MainActivity.this.getContentResolver().openInputStream(uri);
                            try {
                                byte[] bArr2 = new byte[65536];
                                while (true) {
                                    int i2 = inputStreamOpenInputStream.read(bArr2);
                                    if (i2 > 0) {
                                        fileOutputStream.write(bArr2, 0, i2);
                                    } else {
                                        try {
                                            break;
                                        } catch (Throwable th2) {
                                        }
                                    }
                                }
                                inputStreamOpenInputStream.close();
                                fileOutputStream.close();
                                boolean z4 = file.length() > j;
                                String strNoteFromName = Core.noteFromName(strQueryName);
                                if (z4 && strNoteFromName.length() > 0) {
                                    Core.setNote(file, strNoteFromName);
                                }
                                str = strNoteFromName.length() > 0 ? "已加入备份列表（备注：" + strNoteFromName + "）" : "已加入备份列表";
                                zZipDir = z4;
                                final boolean fzZipDir = zZipDir;
                                final String fstr = str;
                                MainActivity.this.ui.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                                        boolean z5 = fzZipDir;
                                                                        if (z5) {
                                            Ui.toast(MainActivity.this, fstr);
                                            MainActivity.this.showMain(0);
                                        } else {
                                            Ui.alert(MainActivity.this, "导入存档", fstr);
                                        }
                                    }
                                });
                            } finally {
                            }
                        }
                        if (z2) {
                            File file2 = new File(MainActivity.this.getCacheDir(), "imp_" + System.currentTimeMillis() + ".rar");
                            fileOutputStream = new FileOutputStream(file2);
                            inputStreamOpenInputStream = MainActivity.this.getContentResolver().openInputStream(uri);
                            try {
                                byte[] bArr3 = new byte[65536];
                                while (true) {
                                    int i3 = inputStreamOpenInputStream.read(bArr3);
                                    if (i3 > 0) {
                                        fileOutputStream.write(bArr3, 0, i3);
                                    } else {
                                        try {
                                            break;
                                        } catch (Throwable th3) {
                                        }
                                    }
                                }
                                inputStreamOpenInputStream.close();
                                fileOutputStream.close();
                                File file3 = new File(MainActivity.this.getCacheDir(), "impr_" + System.currentTimeMillis());
                                file3.mkdirs();
                                Junrar.extract(file2, file3);
                                file2.delete();
                                zZipDir = MainActivity.this.zipDir(file3, new File(Core.backupDir(), str2 + ".zip"));
                                MainActivity.this.deleteRec(file3);
                            } finally {
                            }
                        } else if (lowerCase.endsWith(".sv")) {
                            String[] strArrSplit = strQueryName.substring(0, strQueryName.length() - 3).split("-");
                            String str3 = strArrSplit.length > 0 ? strArrSplit[0] : "0";
                            try {
                                jCurrentTimeMillis = Long.parseLong(strArrSplit[strArrSplit.length - 1]);
                            } catch (Throwable th4) {
                                jCurrentTimeMillis = 0;
                            }
                            if (jCurrentTimeMillis <= 0) {
                                jCurrentTimeMillis = System.currentTimeMillis() / 1000;
                            }
                            long j2 = jCurrentTimeMillis * 1000;
                            String str4 = new SimpleDateFormat("yyyy-MM-dd").format(new Date(j2));
                            File file4 = new File(Core.backupDir(), str2 + ".zip");
                            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file4));
                            try {
                                String str5 = "slots/" + str3 + "/" + str4 + "/";
                                zipOutputStream.putNextEntry(new ZipEntry(str5 + strQueryName));
                                inputStreamOpenInputStream2 = MainActivity.this.getContentResolver().openInputStream(uri);
                                try {
                                    byte[] bArr4 = new byte[65536];
                                    while (true) {
                                        int i4 = inputStreamOpenInputStream2.read(bArr4);
                                        if (i4 > 0) {
                                            zipOutputStream.write(bArr4, 0, i4);
                                        } else {
                                            try {
                                                break;
                                            } catch (Throwable th5) {
                                            }
                                        }
                                    }
                                    inputStreamOpenInputStream2.close();
                                    zipOutputStream.closeEntry();
                                    zipOutputStream.putNextEntry(new ZipEntry(str5 + strQueryName + ".json"));
                                    zipOutputStream.write(("{\"nickname\":\"\",\"pinned\":false,\"isAuto\":false,\"biome\":0,\"runId\":" + str3 + ",\"lastPlayedAtUnixMs\":" + j2 + ",\"hasPlayerPos\":false,\"qolSeed\":0,\"worldEngine\":\"self\",\"mpWorldEngine\":\"\",\"posMode\":\"lastPos\",\"fixedX\":0,\"fixedY\":0,\"isMultiplayer\":false,\"activeLayerModifierIndex\":-1}").getBytes("UTF-8"));
                                    zipOutputStream.closeEntry();
                                    zipOutputStream.close();
                                    zZipDir = file4.length() > 0;
                                } finally {
                                }
                            } catch (Throwable th6) {
                                zipOutputStream.close();
                                throw th6;
                            }
                        } else {
                            str = "不认识的格式，请选 .sv / .zip / .rar / .tar.gz";
                        }
                        final boolean fzZipDir = zZipDir;
                        final String fstr = str;
                        MainActivity.this.ui.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                                boolean z5 = fzZipDir;
                                                        if (z5) {
                                    Ui.toast(MainActivity.this, fstr);
                                    MainActivity.this.showMain(0);
                                } else {
                                    Ui.alert(MainActivity.this, "导入存档", fstr);
                                }
                            }
                        });
                    } finally {
                    }
                } catch (Throwable th7) {
                    Log.e("CUImport", "import failed", th7);
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                            Ui.alert(MainActivity.this, "导入失败", String.valueOf(th7));
                        }
                    });
                }
            }
        }).start();
    }

    private void copyRootDirToLocal(String str, File file) {
        for (String str2 : Root.list(str)) {
            if (str2 != null && str2.trim().length() != 0) {
                String str3 = str + "/" + str2;
                File file2 = new File(file, str2);
                if (Root.exists(str3) && isDirInRoot(str3)) {
                    file2.mkdirs();
                    copyRootDirToLocal(str3, file2);
                } else {
                    String str4 = "/data/local/tmp/cu_one_" + System.currentTimeMillis();
                    Root.execNs("cp -f " + qq(str3) + " " + qq(str4) + " && chmod 666 " + qq(str4));
                    try {
                        FileInputStream fileInputStream = new FileInputStream(str4);
                        FileOutputStream fileOutputStream = new FileOutputStream(file2);
                        byte[] bArr = new byte[65536];
                        while (true) {
                            int i = fileInputStream.read(bArr);
                            if (i <= 0) {
                                break;
                            } else {
                                fileOutputStream.write(bArr, 0, i);
                            }
                        }
                        fileInputStream.close();
                        fileOutputStream.close();
                    } catch (Throwable th) {
                    }
                    Root.execNs("rm -f " + qq(str4));
                }
            }
        }
    }

    private boolean isDirInRoot(String str) {
        return Root.execNs("test -d " + qq(str) + " && echo Y || echo N").contains("Y");
    }

    public void deleteRec(File file) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                deleteRec(file2);
            }
        }
        file.delete();
    }

    private boolean copyDirTo(File file, File file2) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return false;
        }
        boolean z = true;
        for (File file3 : fileArrListFiles) {
            File file4 = new File(file2, file3.getName());
            if (file3.isDirectory()) {
                file4.mkdirs();
                z = copyDirTo(file3, file4) && z;
            } else {
                try {
                    FileInputStream fileInputStream = new FileInputStream(file3);
                    FileOutputStream fileOutputStream = new FileOutputStream(file4);
                    try {
                        byte[] bArr = new byte[65536];
                        while (true) {
                            int i = fileInputStream.read(bArr);
                            if (i > 0) {
                                fileOutputStream.write(bArr, 0, i);
                            } else {
                                try {
                                    break;
                                } catch (Throwable th) {
                                }
                            }
                        }
                        fileInputStream.close();
                        fileOutputStream.close();
                    } catch (Throwable th2) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable th3) {
                        }
                        fileOutputStream.close();
                        throw th2;
                    }
                } catch (Throwable th4) {
                    z = false;
                }
            }
        }
        return z;
    }

    public List<Map.Entry<String, byte[]>> readBackupEntries(File file) throws Exception {
        ArrayList arrayList = new ArrayList();
        if (file.getName().endsWith(".zip")) {
            ZipFile zipFile = new ZipFile(file);
            try {
                Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
                while (enumerationEntries.hasMoreElements()) {
                    ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                    if (!zipEntryNextElement.isDirectory()) {
                        arrayList.add(new AbstractMap.SimpleEntry(zipEntryNextElement.getName(), readAll(zipFile.getInputStream(zipEntryNextElement))));
                    }
                }
            } finally {
                zipFile.close();
            }
        } else {
            File file2 = new File(getCacheDir(), "bk_" + System.currentTimeMillis());
            file2.mkdirs();
            Runtime.getRuntime().exec(new String[]{"tar", "-xzf", file.getAbsolutePath(), "-C", file2.getAbsolutePath()}).waitFor();
            collectEntries(file2, "", arrayList);
            deleteRec(file2);
        }
        return arrayList;
    }

    private void collectEntries(File file, String str, List<Map.Entry<String, byte[]>> list) throws Exception {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file2 : fileArrListFiles) {
            if (file2.isDirectory()) {
                collectEntries(file2, str + file2.getName() + "/", list);
            } else {
                list.add(new AbstractMap.SimpleEntry(str + file2.getName(), readAll(new FileInputStream(file2))));
            }
        }
    }

    public void showSaveData(final File file, final String str) {
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在解析…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                String strExplainTmp;
                try {
                    byte[] bArr = null;
                    byte[] bArr2 = null;
                    for (Map.Entry entry : MainActivity.this.readBackupEntries(file)) {
                        if (((String) entry.getKey()).equals(str)) {
                            bArr = (byte[]) entry.getValue();
                        }
                        if (((String) entry.getKey()).equals(str + ".json")) {
                            bArr2 = (byte[]) entry.getValue();
                        }
                    }
                    strExplainTmp = MainActivity.this.explainSave(bArr, bArr2);
                } catch (Throwable th) {
                    strExplainTmp = "读取失败：" + th;
                }
                final String strExplainSave = strExplainTmp;
                MainActivity.this.ui.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                        TextView textViewTv = Ui.tv(MainActivity.this, strExplainSave, 13.0f, Ui.TEXT);
                        textViewTv.setTextIsSelectable(true);
                        MainActivity.this.showDarkDialog("详细数据", textViewTv);
                    }
                });
            }
        }).start();
    }

    public void extractOneSave(final File file, final String str, final boolean z) {
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay(z ? "正在导出…" : "正在备份…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                String str2;
                File file2;
                try {
                    List<Map.Entry<String, byte[]>> backupEntries = MainActivity.this.readBackupEntries(file);
                    String str3 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    if (z) {
                        file2 = new File(MainActivity.this.getCacheDir(), "one_" + str3 + ".zip");
                    } else {
                        file2 = new File(Core.backupDir(), str3 + ".zip");
                    }
                    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file2));
                    try {
                        for (Map.Entry entry : backupEntries) {
                            String str4 = (String) entry.getKey();
                            if (str4.equals(str) || str4.equals(str + ".json")) {
                                zipOutputStream.putNextEntry(new ZipEntry(str4));
                                zipOutputStream.write((byte[]) entry.getValue());
                                zipOutputStream.closeEntry();
                            }
                        }
                        zipOutputStream.close();
                        if (z) {
                            MainActivity.this.pendingExport = null;
                            str2 = "EXPORT:" + file2.getAbsolutePath();
                        } else {
                            str2 = "已生成单存档备份：" + file2.getName();
                        }
                    } catch (Throwable th) {
                        zipOutputStream.close();
                        throw th;
                    }
                } catch (Throwable th2) {
                    str2 = "失败：" + th2;
                }
                final String fstr2 = str2;
                MainActivity.this.ui.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                        boolean zStartsWith = fstr2.startsWith("EXPORT:");
                                        if (zStartsWith) {
                            MainActivity.this.exportBackup(new File(fstr2.substring(7)));
                        } else {
                            Ui.toast(MainActivity.this, fstr2);
                            MainActivity.this.showMain(0);
                        }
                    }
                });
            }
        }).start();
    }

    public void removeSaveFromBackup(final File file, final String str) {
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在删除…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                String str2;
                try {
                    if (file.getName().endsWith(".zip")) {
                        List<Map.Entry<String, byte[]>> backupEntries = MainActivity.this.readBackupEntries(file);
                        File file2 = new File(MainActivity.this.getCacheDir(), "rm_" + System.currentTimeMillis() + ".zip");
                        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file2));
                        try {
                            int i = 0;
                            for (Map.Entry entry : backupEntries) {
                                String str3 = (String) entry.getKey();
                                if (!str3.equals(str) && !str3.equals(str + ".json")) {
                                    zipOutputStream.putNextEntry(new ZipEntry(str3));
                                    zipOutputStream.write((byte[]) entry.getValue());
                                    zipOutputStream.closeEntry();
                                    i++;
                                }
                            }
                            zipOutputStream.close();
                            FileInputStream fileInputStream = new FileInputStream(file2);
                            FileOutputStream fileOutputStream = new FileOutputStream(file);
                            byte[] bArr = new byte[65536];
                            while (true) {
                                int i2 = fileInputStream.read(bArr);
                                if (i2 <= 0) {
                                    break;
                                } else {
                                    fileOutputStream.write(bArr, 0, i2);
                                }
                            }
                            fileInputStream.close();
                            fileOutputStream.close();
                            file2.delete();
                            str2 = "已删除，剩余 " + i + " 个文件";
                        } catch (Throwable th) {
                            zipOutputStream.close();
                            throw th;
                        }
                    } else {
                        str2 = "tar.gz 备份暂不支持单独删除存档";
                    }
                } catch (Throwable th2) {
                    str2 = "失败：" + th2;
                }
                final String fstr2 = str2;
                MainActivity.this.ui.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                        Ui.toast(MainActivity.this, fstr2);
                        MainActivity.this.showMain(0);
                    }
                });
            }
        }).start();
    }

    public String explainSave(byte[] bArr, byte[] bArr2) throws Exception {
        if (bArr == null) {
            return "找不到这个存档。";
        }
        if (bArr.length > 2 && (bArr[0] & 255) == 31 && (bArr[1] & 255) == 139) {
            bArr = readAll(new GZIPInputStream(new ByteArrayInputStream(bArr)));
        }
        JSONObject jSONObject = new JSONObject(new String(bArr, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        sb.append("【基础】\n");
        sb.append("存档 ID   ").append(jSONObject.optInt("cId")).append("\n");
        sb.append("格式版本  ").append(jSONObject.optInt("cVer")).append("\n");
        sb.append("年龄      ").append(jSONObject.optInt("cAge")).append("    身高  ").append(jSONObject.optInt("cHeight")).append("\n");
        sb.append("生态域    ").append(jSONObject.optInt("biome")).append("\n");
        sb.append("游玩时长  ").append(Math.round(jSONObject.optDouble("runTime", 0.0d))).append(" 秒\n");
        if (bArr2 != null) {
            try {
                JSONObject jSONObject2 = new JSONObject(new String(bArr2, "UTF-8"));
                if (jSONObject2.optBoolean("hasPlayerPos")) {
                    sb.append("位置      X ").append(fmt(jSONObject2.optDouble("playerX"))).append("   Y ").append(fmt(jSONObject2.optDouble("playerY"))).append("\n");
                }
                String strOptString = jSONObject2.optString("nickname", "");
                if (strOptString.length() > 0) {
                    sb.append("备注名    ").append(strOptString).append("\n");
                }
            } catch (Throwable th) {
            }
        }
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("body");
        if (jSONObjectOptJSONObject != null) {
            sb.append("\n【身体】\n");
            sb.append("血量      ").append(fmt(jSONObjectOptJSONObject.optDouble("bloodVolume"))).append("\n");
            sb.append("含氧      ").append(fmt(jSONObjectOptJSONObject.optDouble("bloodOxygen"))).append("\n");
            sb.append("心率      ").append(fmt(jSONObjectOptJSONObject.optDouble("heartRate"))).append("\n");
            sb.append("血压      ").append(fmt(jSONObjectOptJSONObject.optDouble("bloodPressure"))).append("\n");
            sb.append("体温      ").append(fmt(jSONObjectOptJSONObject.optDouble("temperature"))).append("\n");
            sb.append("饥饿      ").append(fmt(jSONObjectOptJSONObject.optDouble("hunger"))).append("\n");
            sb.append("口渴      ").append(fmt(jSONObjectOptJSONObject.optDouble("thirst"))).append("\n");
            sb.append("耐力      ").append(fmt(jSONObjectOptJSONObject.optDouble("stamina"))).append("\n");
            sb.append("精力      ").append(fmt(jSONObjectOptJSONObject.optDouble("energy"))).append("\n");
            sb.append("免疫      ").append(fmt(jSONObjectOptJSONObject.optDouble("immunity"))).append("\n");
            sb.append("脑健康    ").append(fmt(jSONObjectOptJSONObject.optDouble("brainHealth"))).append("\n");
            sb.append("意识      ").append(fmt(jSONObjectOptJSONObject.optDouble("consciousness"))).append("\n");
            JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("skills");
            if (jSONObjectOptJSONObject2 != null) {
                sb.append("\n【技能】\n");
                sb.append("力量  ").append(jSONObjectOptJSONObject2.optInt("STR")).append("   (").append(jSONObjectOptJSONObject2.optInt("expSTR")).append("/").append(jSONObjectOptJSONObject2.optInt("maxSTR")).append(")\n");
                sb.append("抗性  ").append(jSONObjectOptJSONObject2.optInt("RES")).append("   (").append(jSONObjectOptJSONObject2.optInt("expRES")).append("/").append(jSONObjectOptJSONObject2.optInt("maxRES")).append(")\n");
                sb.append("智力  ").append(jSONObjectOptJSONObject2.optInt("INT")).append("   (").append(jSONObjectOptJSONObject2.optInt("expINT")).append("/").append(jSONObjectOptJSONObject2.optInt("maxINT")).append(")\n");
            }
        }
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("items");
        if (jSONArrayOptJSONArray != null) {
            sb.append("\n【背包】共 ").append(jSONArrayOptJSONArray.length()).append(" 件\n");
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject3 = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject3 != null) {
                    String strOptString2 = jSONObjectOptJSONObject3.optString("id");
                    String strItemName = itemName(strOptString2);
                    sb.append(i + 1).append(". ").append(strItemName);
                    if (!strItemName.equals(strOptString2)) {
                        sb.append("  (").append(strOptString2).append(")");
                    }
                    double dOptDouble = jSONObjectOptJSONObject3.optDouble("condition", 1.0d);
                    if (dOptDouble < 0.999d) {
                        sb.append("   完好 ").append(Math.round(dOptDouble * 100.0d)).append("%");
                    }
                    sb.append("\n");
                }
            }
        }
        JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("limbs");
        if (jSONArrayOptJSONArray2 != null) {
            int i2 = 0;
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            for (int i6 = 0; i6 < jSONArrayOptJSONArray2.length(); i6++) {
                JSONObject jSONObjectOptJSONObject4 = jSONArrayOptJSONArray2.optJSONObject(i6);
                if (jSONObjectOptJSONObject4 != null) {
                    if (jSONObjectOptJSONObject4.optBoolean("broken")) {
                        i2++;
                    }
                    if (jSONObjectOptJSONObject4.optBoolean("infected")) {
                        i3++;
                    }
                    if (jSONObjectOptJSONObject4.optDouble("bleedAmount", 0.0d) > 0.0d) {
                        i4++;
                    }
                    if (jSONObjectOptJSONObject4.optBoolean("splinted")) {
                        i5++;
                    }
                }
            }
            sb.append("\n【伤情】\n");
            sb.append("骨折 ").append(i2).append("   感染 ").append(i3).append("   出血 ").append(i4).append("   已固定 ").append(i5).append("\n");
        }
        return sb.toString();
    }

    public void showDarkDialog(String str, View view) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 16.0f)));
        int iDp = Ui.dp(this, 16.0f);
        linearLayoutColumn.setPadding(iDp, iDp, iDp, iDp);
        TextView textViewTv = Ui.tv(this, str, 17.0f, Ui.TEXT);
        textViewTv.setTypeface(Typeface.DEFAULT_BOLD);
        linearLayoutColumn.addView(textViewTv);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        layoutParams.setMargins(0, Ui.dp(this, 10.0f), 0, Ui.dp(this, 10.0f));
        scrollView.setLayoutParams(layoutParams);
        scrollView.addView(view);
        linearLayoutColumn.addView(scrollView);
        Button buttonBtn = Ui.btn(this, "关闭", Ui.CARD2);
        buttonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                dialog.dismiss();
            }
        });
        linearLayoutColumn.addView(buttonBtn);
        dialog.setContentView(linearLayoutColumn);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            attributes.height = -2;
            window.setAttributes(attributes);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(lp2(window), (int) (getResources().getDisplayMetrics().heightPixels * 0.85f));
        }
    }

    private int lp2(Window window) {
        return (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
    }

    private Button dialogBtn(String str, int i) {
        Button button = new Button(this);
        button.setText(str);
        button.setAllCaps(false);
        button.setTextSize(12.0f);
        button.setTextColor(str.equals("删除") ? Ui.DANGER : Ui.TEXT);
        button.setBackground(Ui.ripple(this, i, Ui.dp(this, 8.0f)));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 38.0f), 1.0f);
        layoutParams.setMargins(Ui.dp(this, 3.0f), Ui.dp(this, 6.0f), Ui.dp(this, 3.0f), 0);
        button.setLayoutParams(layoutParams);
        button.setPadding(Ui.dp(this, 4.0f), 0, Ui.dp(this, 4.0f), 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        return button;
    }

    public void showBackupSaves(final File file) {
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在读取…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] bArr;
                final ArrayList arrayList = new ArrayList();
                try {
                    List<Map.Entry<String, byte[]>> backupEntries = MainActivity.this.readBackupEntries(file);
                    for (Map.Entry entry : backupEntries) {
                        String str = (String) entry.getKey();
                        if (str.endsWith(".sv")) {
                            Iterator it = backupEntries.iterator();
                            while (true) {
                                if (!it.hasNext()) {
                                    bArr = null;
                                    break;
                                }
                                Map.Entry entry2 = (Map.Entry) it.next();
                                if (((String) entry2.getKey()).equals(str + ".json")) {
                                    bArr = (byte[]) entry2.getValue();
                                    break;
                                }
                            }
                            String[] strArrSummarizeSave = MainActivity.this.summarizeSave(str, (byte[]) entry.getValue(), bArr);
                            if (strArrSummarizeSave != null) {
                                arrayList.add(strArrSummarizeSave);
                            }
                        }
                    }
                } catch (Throwable th) {
                    Log.e("CUImport", "list saves failed", th);
                }
                MainActivity.this.ui.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                        MainActivity.this.showSavesDialog(file, arrayList);
                    }
                });
            }
        }).start();
    }

    public String[] summarizeSave(String str, byte[] bArr, byte[] bArr2) {
        String strReplace;
        byte[] all = bArr;
        try {
            if (all.length > 2 && (all[0] & 255) == 31 && (all[1] & 255) == 139) {
                try {
                    all = readAll(new GZIPInputStream(new ByteArrayInputStream(all)));
                } catch (Throwable th) {
                    return null;
                }
            }
            JSONObject jSONObject = new JSONObject(new String(all, "UTF-8"));
            String strSubstring = str.substring(str.lastIndexOf(47) + 1);
            if (strSubstring.endsWith(".sv")) {
                strSubstring = strSubstring.substring(0, strSubstring.length() - 3);
            }
            String str2 = strSubstring;
            try {
                strReplace = new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date(Long.parseLong(str2.substring(str2.lastIndexOf(45) + 1)) * 1000));
            } catch (Throwable th2) {
                strReplace = str.substring(str.lastIndexOf("/2026") + 1).replace("-", "/");
            }
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("body") != null ? jSONObject.optJSONObject("body").optJSONObject("skills") : null;
            return new String[]{str2, strReplace, String.valueOf(jSONObject.optInt("cId")), String.valueOf(jSONObject.optInt("cAge")), String.valueOf(jSONObject.optInt("cHeight")), String.valueOf(Math.round(jSONObject.optDouble("runTime", 0.0d))), jSONObjectOptJSONObject == null ? "" : "力量" + jSONObjectOptJSONObject.optInt("STR") + " 抗性" + jSONObjectOptJSONObject.optInt("RES") + " 智力" + jSONObjectOptJSONObject.optInt("INT"), String.valueOf(jSONObject.optJSONArray("items") == null ? 0 : jSONObject.optJSONArray("items").length()), str};
        } catch (Throwable th3) {
        }
        return null;
    }

    public void showSavesDialog(final File file, List<String[]> list) {
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.setPadding(0, Ui.dp(this, 8.0f), 0, 0);
        if (list.isEmpty()) {
            TextView textViewTv = Ui.tv(this, "这份备份里没有找到可读的存档。", 13.0f, Ui.SUB);
            textViewTv.setPadding(Ui.dp(this, 16.0f), Ui.dp(this, 16.0f), Ui.dp(this, 16.0f), Ui.dp(this, 16.0f));
            linearLayoutColumn.addView(textViewTv);
        }
        int i = 0;
        while (i < list.size()) {
            final String[] strArr = list.get(i);
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(1);
            int iDp = Ui.dp(this, 14.0f);
            linearLayout.setPadding(iDp, iDp, iDp, iDp);
            int i2 = i + 1;
            TextView textViewTv2 = Ui.tv(this, "存档 " + i2 + "   " + strArr[1], 15.0f, Ui.TEXT);
            textViewTv2.setTypeface(Typeface.DEFAULT_BOLD);
            linearLayout.addView(textViewTv2);
            TextView textViewTv3 = Ui.tv(this, "年龄 " + strArr[3] + " · 身高 " + strArr[4] + " · " + strArr[5] + " 秒 · " + strArr[6] + "\n物品 " + strArr[7] + " 件 · 存档 ID " + strArr[2], 12.0f, Ui.SUB);
            textViewTv3.setPadding(0, Ui.dp(this, 4.0f), 0, 0);
            linearLayout.addView(textViewTv3);
            LinearLayout linearLayout2 = new LinearLayout(this);
            linearLayout2.setOrientation(0);
            Button buttonDialogBtn = dialogBtn("详细数据", Theme.accentDark());
            buttonDialogBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.showSaveData(file, strArr[8]);
                }
            });
            linearLayout2.addView(buttonDialogBtn);
            Button buttonDialogBtn2 = dialogBtn("备份", Ui.CARD2);
            buttonDialogBtn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.extractOneSave(file, strArr[8], false);
                }
            });
            linearLayout2.addView(buttonDialogBtn2);
            Button buttonDialogBtn3 = dialogBtn("导出", Ui.CARD2);
            buttonDialogBtn3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.extractOneSave(file, strArr[8], true);
                }
            });
            linearLayout2.addView(buttonDialogBtn3);
            Button buttonDialogBtn4 = dialogBtn("删除", Ui.CARD2);
            buttonDialogBtn4.setTextColor(Ui.DANGER);
            buttonDialogBtn4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("从这份备份里删除该存档？").setMessage(strArr[1] + "\n\n只影响这个备份包，不动游戏里的存档。").setPositiveButton("删除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i3) {
                            MainActivity.this.removeSaveFromBackup(file, strArr[8]);
                        }
                    }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
                }
            });
            linearLayout2.addView(buttonDialogBtn4);
            linearLayout.addView(linearLayout2);
            linearLayoutColumn.addView(linearLayout);
            if (i < list.size() - 1) {
                View view = new View(this);
                view.setBackgroundColor(-14010812);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 1.0f));
                layoutParams.setMargins(Ui.dp(this, 14.0f), 0, Ui.dp(this, 14.0f), 0);
                view.setLayoutParams(layoutParams);
                linearLayoutColumn.addView(view);
            }
            i = i2;
        }
        linearLayoutColumn.setBackgroundColor(0);
        showDarkDialog("存档列表（共 " + list.size() + " 个）", linearLayoutColumn);
    }

    private void showBackupDetail(final File file) {
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在读取存档…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String backup = MainActivity.this.parseBackup(file);
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                            ScrollView scrollView = new ScrollView(MainActivity.this);
                            TextView textViewTv = Ui.tv(MainActivity.this, backup, 13.0f, -15066598);
                            int iDp = Ui.dp(MainActivity.this, 16.0f);
                            textViewTv.setPadding(iDp, iDp, iDp, iDp);
                            textViewTv.setTextIsSelectable(true);
                            scrollView.addView(textViewTv);
                            new AlertDialog.Builder(MainActivity.this).setTitle("存档详情").setView(scrollView).setPositiveButton("关闭", (DialogInterface.OnClickListener) null).show();
                        }
                    });
                } catch (Throwable th) {
                    Log.e("CUImport", "detail failed", th);
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                            Ui.alert(MainActivity.this, "读取失败", String.valueOf(th));
                        }
                    });
                }
            }
        }).start();
    }

    private String itemName(String str) {
        if (langCache == null) {
            String[] strArr = {"CasualtiesUnknown_Data/Lang/zh-CN.json", "Lang/zh-CN.json", "CasualtiesUnknown_Data/Lang/EN.json", "Lang/EN.json"};
            for (int i = 0; i < 4; i++) {
                File file = new File(Core.gameDir(), strArr[i]);
                if (file.exists()) {
                    try {
                        langCache = new JSONObject(new String(readAll(new FileInputStream(file)), "UTF-8"));
                        break;
                    } catch (Throwable th) {
                    }
                }
            }
        }
        if (langCache != null) {
            String[] strArr2 = {"main", "buildings", "moodles", "other", "character"};
            for (int i2 = 0; i2 < 5; i2++) {
                JSONObject jSONObjectOptJSONObject = langCache.optJSONObject(strArr2[i2]);
                if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.has(str)) {
                    String strOptString = jSONObjectOptJSONObject.optString(str, "");
                    if (strOptString.length() > 0) {
                        return strOptString.replaceAll("<[^>]*>", "");
                    }
                }
            }
            if (langCache.has(str)) {
                String strOptString2 = langCache.optString(str, "");
                if (strOptString2.length() > 0) {
                    return strOptString2.replaceAll("<[^>]*>", "");
                }
            }
        }
        return str;
    }

    public String parseBackup(File file) throws Exception {
        byte[] all;
        String name = file.getName();
        byte[] all2 = null;
        if (name.endsWith(".zip")) {
            ZipFile zipFile = new ZipFile(file);
            try {
                Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
                all = null;
                while (enumerationEntries.hasMoreElements()) {
                    ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                    String name2 = zipEntryNextElement.getName();
                    if (name2.endsWith(".sv") || name2.endsWith(".sv.json")) {
                        byte[] all3 = readAll(zipFile.getInputStream(zipEntryNextElement));
                        if (name2.endsWith(".sv.json")) {
                            if (all == null) {
                                all = all3;
                            }
                        } else if (all2 == null) {
                            all2 = all3;
                        }
                    }
                }
            } finally {
                zipFile.close();
            }
        } else if (!name.endsWith(".tar.gz")) {
            all = null;
        } else {
            File file2 = new File(getCacheDir(), "dtl_" + System.currentTimeMillis());
            file2.mkdirs();
            Runtime.getRuntime().exec(new String[]{"tar", "-xzf", file.getAbsolutePath(), "-C", file2.getAbsolutePath()}).waitFor();
            ArrayList<File> arrayList = new ArrayList<File>();
            collectSv(file2, arrayList);
            all = null;
            for (File file3 : arrayList) {
                if (file3.getName().endsWith(".sv")) {
                    if (all2 == null) {
                        all2 = readAll(new FileInputStream(file3));
                    }
                } else if (file3.getName().endsWith(".sv.json") && all == null) {
                    all = readAll(new FileInputStream(file3));
                }
            }
            deleteRec(file2);
        }
        if (all2 == null) {
            return "这个备份里没有找到存档文件。";
        }
        if (all2.length > 2 && (all2[0] & 255) == 31 && (all2[1] & 255) == 139) {
            all2 = readAll(new GZIPInputStream(new ByteArrayInputStream(all2)));
        }
        JSONObject jSONObject = new JSONObject(new String(all2, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        sb.append("【基础】\n");
        sb.append("存档 ID   ").append(jSONObject.optInt("cId")).append("\n");
        sb.append("格式版本  ").append(jSONObject.optInt("cVer")).append("\n");
        sb.append("年龄      ").append(jSONObject.optInt("cAge")).append("    身高  ").append(jSONObject.optInt("cHeight")).append("\n");
        sb.append("生态域    ").append(jSONObject.optInt("biome")).append("\n");
        sb.append("游玩时长  ").append(Math.round(jSONObject.optDouble("runTime", 0.0d))).append(" 秒\n");
        sb.append("已移动    ").append(Math.round(jSONObject.optDouble("totalTraveled", 0.0d))).append("\n");
        if (all != null) {
            try {
                JSONObject jSONObject2 = new JSONObject(new String(all, "UTF-8"));
                if (jSONObject2.optBoolean("hasPlayerPos")) {
                    sb.append("位置      X ").append(fmt(jSONObject2.optDouble("playerX"))).append("   Y ").append(fmt(jSONObject2.optDouble("playerY"))).append("\n");
                }
            } catch (Throwable th) {
            }
        }
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("body");
        if (jSONObjectOptJSONObject != null) {
            sb.append("\n【身体】\n");
            sb.append("血量      ").append(fmt(jSONObjectOptJSONObject.optDouble("bloodVolume"))).append("\n");
            sb.append("含氧      ").append(fmt(jSONObjectOptJSONObject.optDouble("bloodOxygen"))).append("\n");
            sb.append("心率      ").append(fmt(jSONObjectOptJSONObject.optDouble("heartRate"))).append("\n");
            sb.append("血压      ").append(fmt(jSONObjectOptJSONObject.optDouble("bloodPressure"))).append("\n");
            sb.append("体温      ").append(fmt(jSONObjectOptJSONObject.optDouble("temperature"))).append("\n");
            sb.append("饥饿      ").append(fmt(jSONObjectOptJSONObject.optDouble("hunger"))).append("\n");
            sb.append("口渴      ").append(fmt(jSONObjectOptJSONObject.optDouble("thirst"))).append("\n");
            sb.append("耐力      ").append(fmt(jSONObjectOptJSONObject.optDouble("stamina"))).append("\n");
            sb.append("精力      ").append(fmt(jSONObjectOptJSONObject.optDouble("energy"))).append("\n");
            sb.append("免疫      ").append(fmt(jSONObjectOptJSONObject.optDouble("immunity"))).append("\n");
            sb.append("脑健康    ").append(fmt(jSONObjectOptJSONObject.optDouble("brainHealth"))).append("\n");
            sb.append("意识      ").append(fmt(jSONObjectOptJSONObject.optDouble("consciousness"))).append("\n");
            JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("skills");
            if (jSONObjectOptJSONObject2 != null) {
                sb.append("\n【技能】\n");
                sb.append("力量  ").append(jSONObjectOptJSONObject2.optInt("STR")).append("   (").append(jSONObjectOptJSONObject2.optInt("expSTR")).append("/").append(jSONObjectOptJSONObject2.optInt("maxSTR")).append(")\n");
                sb.append("抗性  ").append(jSONObjectOptJSONObject2.optInt("RES")).append("   (").append(jSONObjectOptJSONObject2.optInt("expRES")).append("/").append(jSONObjectOptJSONObject2.optInt("maxRES")).append(")\n");
                sb.append("智力  ").append(jSONObjectOptJSONObject2.optInt("INT")).append("   (").append(jSONObjectOptJSONObject2.optInt("expINT")).append("/").append(jSONObjectOptJSONObject2.optInt("maxINT")).append(")\n");
            }
        }
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("items");
        if (jSONArrayOptJSONArray != null) {
            sb.append("\n【背包】共 ").append(jSONArrayOptJSONArray.length()).append(" 件\n");
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject3 = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject3 != null) {
                    String strOptString = jSONObjectOptJSONObject3.optString("id");
                    String strItemName = itemName(strOptString);
                    sb.append(i + 1).append(". ").append(strItemName);
                    if (!strItemName.equals(strOptString)) {
                        sb.append("  (").append(strOptString).append(")");
                    }
                    double dOptDouble = jSONObjectOptJSONObject3.optDouble("condition", 1.0d);
                    if (dOptDouble < 0.999d) {
                        sb.append("   完好 ").append(Math.round(dOptDouble * 100.0d)).append("%");
                    }
                    sb.append("\n");
                }
            }
        }
        JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("limbs");
        if (jSONArrayOptJSONArray2 != null) {
            int i2 = 0;
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            for (int i6 = 0; i6 < jSONArrayOptJSONArray2.length(); i6++) {
                JSONObject jSONObjectOptJSONObject4 = jSONArrayOptJSONArray2.optJSONObject(i6);
                if (jSONObjectOptJSONObject4 != null) {
                    if (jSONObjectOptJSONObject4.optBoolean("broken")) {
                        i2++;
                    }
                    if (jSONObjectOptJSONObject4.optBoolean("infected")) {
                        i3++;
                    }
                    if (jSONObjectOptJSONObject4.optDouble("bleedAmount", 0.0d) > 0.0d) {
                        i4++;
                    }
                    if (jSONObjectOptJSONObject4.optBoolean("splinted")) {
                        i5++;
                    }
                }
            }
            sb.append("\n【伤情】\n");
            sb.append("骨折 ").append(i2).append("   感染 ").append(i3).append("   出血 ").append(i4).append("   已固定 ").append(i5).append("\n");
        }
        return sb.toString();
    }

    private String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(Math.round(d * 100.0d) / 100.0d);
    }

    private byte[] readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byte[] bArr = new byte[65536];
            while (true) {
                int i = inputStream.read(bArr);
                if (i > 0) {
                    byteArrayOutputStream.write(bArr, 0, i);
                } else {
                    try {
                        break;
                    } catch (Throwable th) {
                    }
                }
            }
            return byteArrayOutputStream.toByteArray();
        } finally {
            try {
                inputStream.close();
            } catch (Throwable th2) {
            }
        }
    }

    private void collectSv(File file, List<File> list) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file2 : fileArrListFiles) {
            if (file2.isDirectory()) {
                collectSv(file2, list);
            } else {
                list.add(file2);
            }
        }
    }

    public void editNote(final File file) {
        final EditText editText = new EditText(this);
        editText.setText(Core.noteOf(file));
        editText.setHint("例如：通关前 / 二周目");
        editText.setSingleLine(true);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        editText.setHint("最多 40 字");
        int iDp = Ui.dp(this, 16.0f);
        editText.setPadding(iDp, iDp, iDp, iDp);
        new AlertDialog.Builder(this).setTitle("备注").setView(editText).setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Core.setNote(file, editText.getText().toString());
                MainActivity.this.showMain(0);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    public void doRestore(final Uri uri) {
        final View[] viewArrBuildProgressOverlay = buildProgressOverlay("正在恢复存档…");
        this.shell.addView(viewArrBuildProgressOverlay[0], new FrameLayout.LayoutParams(-1, -1));
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                String message = "";
                File tmp = new File(getCacheDir(), "restore_" + System.currentTimeMillis());
                File cache = new File(getCacheDir(), "restore_src_" + System.currentTimeMillis());
                try {
                    InputStream in = getContentResolver().openInputStream(uri);
                    FileOutputStream out = new FileOutputStream(cache);
                    byte[] b = new byte[65536];
                    int n;
                    while ((n = in.read(b)) > 0) {
                        out.write(b, 0, n);
                    }
                    out.close();
                    in.close();
                    tmp.mkdirs();
                    Packer.extract(cache, tmp, null);
                    File src = findSlotsIn(tmp);
                    if (src == null) {
                        throw new java.io.IOException("备份里没有找到存档（slots）");
                    }
                    File dest = Core.findSlotsDir();
                    if (dest == null) {
                        dest = Core.slotsDir();
                    }
                    dest.mkdirs();
                    copyDir(src, dest);
                    ok = true;
                } catch (Throwable th) {
                    message = String.valueOf(th);
                } finally {
                    cache.delete();
                    deleteDir(tmp);
                }
                final boolean fok = ok;
                final String fmsg = message;
                MainActivity.this.ui.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.shell.removeView(viewArrBuildProgressOverlay[0]);
                        if (fok) {
                            Ui.toast(MainActivity.this, "存档已恢复");
                            MainActivity.this.showMain(0);
                        } else {
                            Ui.alert(MainActivity.this, "恢复失败", fmsg);
                        }
                    }
                });
            }
        }).start();
    }

    private File findSlotsIn(File dir) {
        File direct = new File(dir, "slots");
        if (direct.isDirectory()) {
            return direct;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return null;
        }
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                File found = findSlotsIn(children[i]);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void copyDir(File src, File dest) throws Exception {
        File[] children = src.listFiles();
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            File target = new File(dest, child.getName());
            if (child.isDirectory()) {
                target.mkdirs();
                copyDir(child, target);
            } else {
                InputStream in = new FileInputStream(child);
                FileOutputStream out = new FileOutputStream(target);
                byte[] b = new byte[65536];
                int n;
                while ((n = in.read(b)) > 0) {
                    out.write(b, 0, n);
                }
                out.close();
                in.close();
            }
        }
    }

    private void deleteDir(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                if (children[i].isDirectory()) {
                    deleteDir(children[i]);
                } else {
                    children[i].delete();
                }
            }
        }
        dir.delete();
    }

    public void enterWith(int i, String str, String str2) {
        if (i == 2) {
            showGamesirHint(str2);
        } else {
            launchPkg(str2);
            Ui.toast(this, "已打开 " + str + "，点击容器即可进入游戏");
        }
    }

    private void showGamesirHint(final String str) {
        if (Core.prefs(this).getBoolean("gamesir_no_hint", false)) {
            launchPkg(str);
            Ui.toast(this, "已打开 盖世游戏");
            return;
        }
        final CheckBox checkBox = new CheckBox(this);
        int iDp = Ui.dp(this, 16.0f);
        checkBox.setPadding(iDp, Ui.dp(this, 4.0f), iDp, 0);
        checkBox.setText("不再提醒");
        checkBox.setTextColor(-12303292);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("准备用 盖世游戏 运行").setMessage("打开后请按下面操作把游戏加进去：\n\n① 登录自己的账号\n\n② 选择 游戏库 → 导入游戏\n\n③ 选择启动程序(.exe) → Download → Casualties Unknown_7.0.1Demo → CasualtiesUnknown.exe\n\n④ 启动游戏：\nCasualtiesUnknown.exe\n\n即可").setView(checkBox).setPositiveButton("打开", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (checkBox.isChecked()) {
                    Core.prefs(MainActivity.this).edit().putBoolean("gamesir_no_hint", true).apply();
                }
                MainActivity.this.launchPkg(str);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                AlertDialog alertDialog = (AlertDialog) dialogInterface;
                alertDialog.getButton(-1).setAllCaps(false);
                alertDialog.getButton(-2).setAllCaps(false);
            }
        });
        alertDialogCreate.show();
    }

    public void enterGame() {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        for (int i = 0; i < WIN_PKGS.length; i++) {
            String strInstalledPkg = installedPkg(i);
            if (strInstalledPkg != null) {
                arrayList.add(WIN_NAMES[i]);
                arrayList2.add(strInstalledPkg);
                arrayList3.add(Integer.valueOf(i));
            }
        }
        if (arrayList2.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("还没有安装 Winlator").setMessage("游戏需要 Winlator 才能运行。\n要现在去安装吗？").setPositiveButton("去安装", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    MainActivity.this.showWinlator();
                }
            }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
            return;
        }
        if (arrayList2.size() == 1) {
            enterWith(((Integer) arrayList3.get(0)).intValue(), (String) arrayList.get(0), (String) arrayList2.get(0));
            return;
        }
        Runnable[] runnableArr = new Runnable[arrayList2.size()];
        for (int i2 = 0; i2 < arrayList2.size(); i2++) {
            final String str = (String) arrayList2.get(i2);
            final String str2 = (String) arrayList.get(i2);
            final int iIntValue = ((Integer) arrayList3.get(i2)).intValue();
            runnableArr[i2] = new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.enterWith(iIntValue, str2, str);
                }
            };
        }
        showChooser("检测到多个运行环境", "选择用哪个打开游戏：", (String[]) arrayList.toArray(new String[0]), runnableArr);
    }

    public void chooseWinlator(int i) {
        try {
            chooseWinlator0(i);
        } catch (Throwable th) {
            Log.e("CU", "chooseWinlator failed", th);
            crashPage(th);
        }
    }

    private void chooseWinlator0(int i) {
        String[] strArr;
        String str;
        if (installedPkg(i) != null) {
            this.sp.edit().putBoolean("setup_done", true).apply();
            showMain(0);
            return;
        }
        if (i != 2) {
            this.sp.edit().putBoolean("setup_done", true).apply();
            if (i == 0) {
                strArr = new String[]{"Bionic Hub V3.9.9Beta.1_AndroBench.apk", "Bionic Hub_v3.9.9Beta.1.apk", "bionichub.apk"};
            } else {
                strArr = new String[]{"winlator-cn.apk", "WinlatorCN.apk", "winlator.cn.apk"};
            }
            File fileFindPackage = Core.findPackage(strArr);
            if (fileFindPackage == null) {
                if (i == 0) {
                    str = "https://pan.quark.cn/s/98269f1f7dbe#/list/share";
                } else {
                    str = "http://52emu.cn/wp/376.html";
                }
                new AlertDialog.Builder(this).setTitle("还没有 " + (i == 0 ? "Bionic Hub" : "Winlator CN")).setMessage("可以到下面这个地址获取：\n\n" + str + "\n\n要现在打开吗？").setPositiveButton("打开链接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        MainActivity.this.openUrl(str);
                    }
                }).setNeutralButton("复制链接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        ((ClipboardManager) MainActivity.this.getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("link", str));
                        Ui.toast(MainActivity.this, "链接已复制");
                    }
                }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
                return;
            }
            try {
                Installer.installApk(this, fileFindPackage);
                showMain(0);
                return;
            } catch (Throwable th) {
                showMain(0);
                Ui.alert(this, "安装失败", String.valueOf(th));
                return;
            }
        }
        new AlertDialog.Builder(this).setTitle("还没有 盖世游戏").setMessage("会自动根据设备进行配置，推荐小白使用。\n\n可以到下面这个地址获取：\n\nhttps://gamehub.xiaoji.com/zh-cn/\n\n要现在打开吗？").setPositiveButton("打开链接", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                MainActivity.this.openUrl("https://gamehub.xiaoji.com/zh-cn/");
            }
        }).setNeutralButton("复制链接", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                ((ClipboardManager) MainActivity.this.getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("link", "https://gamehub.xiaoji.com/zh-cn/"));
                Ui.toast(MainActivity.this, "链接已复制");
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    public void showMain(int i) {
        View viewTabLang;
        if (i != 1) {
            this.bepPromptShown = false;
        }
        this.curTab = i;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(Ui.BG);
        FrameLayout frameLayout = new FrameLayout(this);
        linearLayout.addView(frameLayout, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        if (i == 0) {
            viewTabLang = tabGame();
        } else if (i == 1) {
            viewTabLang = tabMod();
        } else {
            viewTabLang = i == 2 ? tabLang() : tabSet();
        }
        frameLayout.addView(viewTabLang, new FrameLayout.LayoutParams(-1, -1));
        linearLayout.addView(buildNav());
        setPage(linearLayout);
    }

    private LinearLayout buildNav() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setBackgroundColor(Ui.CARD);
        String[] strArr = {"游戏", "模组", "语言", "设置"};
        int i = 0;
        while (i < 4) {
            boolean z = i == this.curTab;
            LinearLayout linearLayout2 = new LinearLayout(this);
            linearLayout2.setOrientation(1);
            linearLayout2.setGravity(17);
            linearLayout2.setPadding(0, Ui.dp(this, 12.0f), 0, Ui.dp(this, 10.0f));
            linearLayout2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            linearLayout2.setBackground(Ui.ripple(this, 0, 0.0f));
            boolean z2 = i == 1 && !Core.hasBepInEx();
            int iAccent = Theme.accent();
            int i2 = -10854296;
            if (z) {
                if (!z2) {
                    i2 = iAccent;
                }
            } else if (!z2) {
                i2 = Ui.SUB;
            }
            TextView textViewTv = Ui.tv(this, strArr[i], 14.0f, i2);
            textViewTv.setGravity(17);
            linearLayout2.addView(textViewTv);
            View view = new View(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Ui.dp(this, 18.0f), Ui.dp(this, 3.0f));
            layoutParams.setMargins(0, Ui.dp(this, 7.0f), 0, 0);
            view.setLayoutParams(layoutParams);
            if (!z) {
                iAccent = 0;
            }
            view.setBackground(Ui.round(iAccent, Ui.dp(this, 2.0f)));
            if (z) {
                view.setScaleX(0.0f);
                view.animate().scaleX(1.0f).setDuration(280L).setInterpolator(new OvershootInterpolator(2.0f)).start();
            }
            linearLayout2.addView(view);
            Ui.pressable(linearLayout2);
            final int si = i;
            linearLayout2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    if (si != MainActivity.this.curTab) {
                        MainActivity.this.showMain(si);
                    }
                }
            });
            linearLayout.addView(linearLayout2);
            i++;
        }
        return linearLayout;
    }

    private View tabGame() {
        String strSubstring;
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.addView(header("游 戏"));
        boolean zHasGame = Core.hasGame();
        LinearLayout linearLayoutCard = Ui.card(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        TextView textViewTv = Ui.tv(this, "Casualties Unknown Demo", 17.0f, Ui.TEXT);
        textViewTv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(textViewTv);
        int iAccentDark = Theme.accentDark();
        if (zHasGame) {
            TextView textViewChip = chip("进入游戏", iAccentDark);
            textViewChip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.enterGame();
                }
            });
            linearLayout.addView(textViewChip);
        }
        linearLayoutCard.addView(linearLayout);
        long size = Core.size(Core.gameDir());
        linearLayoutCard.addView(Ui.desc(this, "安装状态：" + (zHasGame ? "准备就绪" : "数据缺失") + "\n占用空间：" + (size > 0 ? Core.human(size) : "—") + "\n已装模组：" + Core.scanMods().size() + " 个\n路径：" + Core.gameDir().getAbsolutePath()));
        if (!zHasGame) {
            Button buttonSmallBtn = Ui.smallBtn(this, "重新安装", iAccentDark);
            buttonSmallBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.showInstall();
                }
            });
            linearLayoutCard.addView(buttonSmallBtn);
        }
        linearLayoutColumn.addView(linearLayoutCard);
        LinearLayout linearLayoutCard2 = Ui.card(this);
        boolean zHasSaveMod = Core.hasSaveMod();
        boolean zSaveDirExists = Core.saveDirExists();
        linearLayoutCard2.addView(Ui.cardHeader(this, "存档", null));
        if (!zHasSaveMod) {
            linearLayoutCard2.setAlpha(0.45f);
            linearLayoutCard2.addView(Ui.desc(this, "需要安装「存档管理模组」（如 CuSaveManager）后才能在此管理存档。\n请到「模组」页面安装。"));
            linearLayoutColumn.addView(linearLayoutCard2);
            return Ui.scroll(this, linearLayoutColumn);
        }
        if (zSaveDirExists) {
            linearLayoutCard2.addView(Ui.desc(this, "存档数量：" + Core.saveCount() + " 个\n想备份最新进度？先在游戏里用存档模组的「立即保存」存一次。"));
        }
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        Button buttonSmallBtn2 = Ui.smallBtn(this, "＋ 备份", iAccentDark);
        buttonSmallBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.doBackupSaves();
            }
        });
        linearLayout2.addView(buttonSmallBtn2);
        Button buttonSmallBtn3 = Ui.smallBtn(this, "导入存档", Ui.CARD2);
        buttonSmallBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.pickZip(MainActivity.REQ_IMPORT_SAVE);
            }
        });
        linearLayout2.addView(buttonSmallBtn3);
        linearLayoutCard2.addView(linearLayout2);
        List<File> listListBackups = Core.listBackups();
        if (listListBackups.isEmpty()) {
            linearLayoutCard2.addView(Ui.desc(this, "还没有备份。点上面的「备份」保存一份。"));
        } else {
            linearLayoutCard2.addView(Ui.desc(this, "已有备份：" + listListBackups.size() + " 份"));
        }
        linearLayoutColumn.addView(linearLayoutCard2);
        for (final File file : listListBackups) {
            LinearLayout linearLayoutCard3 = Ui.card(this);
            String name = file.getName();
            if (name.endsWith(".tar.gz")) {
                strSubstring = name.substring(0, name.length() - 7);
            } else {
                if (name.endsWith(".zip")) {
                    name = name.substring(0, name.length() - 4);
                }
                strSubstring = name;
            }
            try {
                strSubstring = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new SimpleDateFormat("yyyyMMdd_HHmmss").parse(strSubstring));
            } catch (Throwable th) {
            }
            String strNoteOf = Core.noteOf(file);
            if (strNoteOf.length() > 0) {
                linearLayoutCard3.addView(Ui.tv(this, strNoteOf, 16.0f, Theme.accent()));
                TextView textViewTv2 = Ui.tv(this, strSubstring, 12.0f, Ui.SUB);
                textViewTv2.setPadding(0, Ui.dp(this, 4.0f), 0, 0);
                linearLayoutCard3.addView(textViewTv2);
            } else {
                linearLayoutCard3.addView(Ui.tv(this, "备份 " + strSubstring, 15.0f, Ui.TEXT));
                TextView textViewTv3 = Ui.tv(this, "（无备注）", 12.0f, Ui.SUB);
                textViewTv3.setPadding(0, Ui.dp(this, 4.0f), 0, 0);
                linearLayoutCard3.addView(textViewTv3);
            }
            TextView textViewTv4 = Ui.tv(this, Core.human(file.length()), 11.0f, Ui.SUB);
            textViewTv4.setPadding(0, Ui.dp(this, 2.0f), 0, 0);
            linearLayoutCard3.addView(textViewTv4);
            LinearLayout linearLayout3 = new LinearLayout(this);
            linearLayout3.setOrientation(0);
            Button buttonTinyBtn = tinyBtn("恢复存档", iAccentDark);
            buttonTinyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder title = new AlertDialog.Builder(MainActivity.this).setTitle("恢复这份备份？");
                    StringBuilder sbAppend = new StringBuilder().append("会用「");
                    int length = Core.noteOf(file).length();
                    File file2 = file;
                    title.setMessage(sbAppend.append(length > 0 ? Core.noteOf(file2) : file2.getName()).append("」完全替换当前存档。\n\n当前 ").append(Core.saveCount()).append(" 个存档会全部删除，只保留备份里的。\n建议先备份一次当前存档。").toString()).setPositiveButton("恢复", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.this.doRestore(Uri.fromFile(file));
                        }
                    }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
                }
            });
            linearLayout3.addView(buttonTinyBtn);
            Button buttonTinyBtn2 = tinyBtn("导出存档", Ui.CARD2);
            buttonTinyBtn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.exportBackup(file);
                }
            });
            linearLayout3.addView(buttonTinyBtn2);
            Button buttonTinyBtn3 = tinyBtn("备注", Ui.CARD2);
            buttonTinyBtn3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.editNote(file);
                }
            });
            linearLayout3.addView(buttonTinyBtn3);
            Button buttonTinyBtn4 = tinyBtn("详情", Ui.CARD2);
            buttonTinyBtn4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.showBackupSaves(file);
                }
            });
            linearLayout3.addView(buttonTinyBtn4);
            View view = new View(this);
            view.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1.0f));
            linearLayout3.addView(view);
            Button buttonTinyBtn5 = tinyBtn("删除", 0);
            buttonTinyBtn5.setTextColor(Ui.DANGER);
            buttonTinyBtn5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("删除这份备份？").setMessage(file.getName() + "\n\n删除后无法恢复。").setPositiveButton("删除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Core.noteFile(file).delete();
                            Ui.toast(MainActivity.this, file.delete() ? "已删除" : "删除失败");
                            MainActivity.this.showMain(0);
                        }
                    }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
                }
            });
            linearLayout3.addView(buttonTinyBtn5);
            linearLayoutCard3.addView(linearLayout3);
            linearLayoutColumn.addView(linearLayoutCard3);
        }
        return Ui.scroll(this, linearLayoutColumn);
    }

    public void showBepInExPrompt() {
        if (this.bepPromptShown) {
            return;
        }
        this.bepPromptShown = true;
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("未检测到 BepInEx").setMessage("安装模组需要 BepInEx 前置框架。\n\n是否现在安装？\n（约 0.6 MB，安装完模组页会自动解锁）").setPositiveButton("安装", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.installBepFromAsset();
            }
        }).setNeutralButton("重新检测", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                boolean zHasBepInEx = Core.hasBepInEx();
                MainActivity mainActivity = MainActivity.this;
                if (zHasBepInEx) {
                    Ui.toast(mainActivity, "已检测到 BepInEx，模组页已解锁");
                    MainActivity.this.showMain(1);
                } else {
                    Ui.toast(mainActivity, "仍未检测到 BepInEx");
                    MainActivity.this.bepPromptShown = false;
                    MainActivity.this.showMain(1);
                }
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                AlertDialog alertDialog = (AlertDialog) dialogInterface;
                alertDialog.getButton(-1).setAllCaps(false);
                alertDialog.getButton(-2).setAllCaps(false);
                alertDialog.getButton(-3).setAllCaps(false);
            }
        });
        alertDialogCreate.show();
    }

    public void installBepFromAsset() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Installer.installBepInEx(MainActivity.this.getAssets().open("bepinex.zip"), null);
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Ui.toast(MainActivity.this, "BepInEx 安装完成，模组页已解锁");
                            MainActivity.this.showMain(1);
                        }
                    });
                } catch (Throwable th) {
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Ui.alert(MainActivity.this, "安装失败", String.valueOf(th));
                        }
                    });
                }
            }
        }).start();
    }

    private View tabMod() {
        float f;
        if (!Core.hasBepInEx()) {
            LinearLayout linearLayoutColumn = Ui.column(this);
            linearLayoutColumn.addView(header("模 组"));
            LinearLayout linearLayoutCard = Ui.card(this);
            linearLayoutCard.addView(Ui.cardHeader(this, "🔒 模组功能未解锁", "需要先安装 BepInEx 前置框架。"));
            linearLayoutCard.addView(Ui.desc(this, "BepInEx 是加载模组的基础框架。\n安装之后这里才能看到、开关模组。"));
            Button buttonSmallBtn = Ui.smallBtn(this, "安装 BepInEx", Ui.CARD2);
            buttonSmallBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.this.bepPromptShown = false;
                    MainActivity.this.showBepInExPrompt();
                }
            });
            linearLayoutCard.addView(buttonSmallBtn);
            linearLayoutColumn.addView(linearLayoutCard);
            LinearLayout linearLayoutLogCard = Ui.card(this);
            linearLayoutLogCard.addView(Ui.cardHeader(this, "运行日志", "解析 BepInEx/LogOutput.log"));
            linearLayoutLogCard.addView(Ui.desc(this, ModLog.summary(this)));
            final Button buttonLogBtn = Ui.smallBtn(this, "查看详情", Ui.CARD2);
            buttonLogBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LogSheet.show(MainActivity.this);
                }
            });
            linearLayoutLogCard.addView(buttonLogBtn);
            linearLayoutColumn.addView(linearLayoutLogCard);
            linearLayoutColumn.setAlpha(0.45f);
            ScrollView scrollViewScroll = Ui.scroll(this, linearLayoutColumn);
            scrollViewScroll.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.showBepInExPrompt();
                }
            });
            return scrollViewScroll;
        }
        LinearLayout linearLayoutColumn2 = Ui.column(this);
        linearLayoutColumn2.addView(header("模 组"));
        LinearLayout linearLayoutCard2 = Ui.card(this);
        linearLayoutCard2.addView(Ui.cardHeader(this, "MOD 工具", "模组必须装在 BepInEx/plugins 下。\n关闭某个模组只是改名，不会删除文件。"));
        LinearLayout linearLayout = new LinearLayout(this);
        int i = 0;
        linearLayout.setOrientation(0);
        Button buttonSmallBtn2 = Ui.smallBtn(this, "N 网模组页", Theme.accentDark());
        buttonSmallBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.openUrl(MainActivity.URL_NEXUS);
            }
        });
        linearLayout.addView(buttonSmallBtn2);
        Button buttonSmallBtn3 = Ui.smallBtn(this, "本地导入", Ui.CARD2);
        buttonSmallBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.pickZip(MainActivity.REQ_MOD_ZIP);
            }
        });
        linearLayout.addView(buttonSmallBtn3);
        linearLayoutCard2.addView(linearLayout);
        linearLayoutColumn2.addView(linearLayoutCard2);
        final List<Core.Mod> listScanMods = Core.scanMods();
        int i2 = 3;
        final TextView[] textViewArr = new TextView[3];
        int i3 = 1;
        String[] strArr = {"已安装", "已启用", "已禁用"};
        int[] iArr = {Ui.TEXT, Theme.accent(), Ui.SUB};
        LinearLayout linearLayoutCard3 = Ui.card(this);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        int i4 = 0;
        while (true) {
            f = 1.0f;
            if (i4 >= i2) {
                break;
            }
            LinearLayout linearLayout3 = new LinearLayout(this);
            linearLayout3.setOrientation(i3);
            linearLayout3.setGravity(17);
            linearLayout3.setLayoutParams(new LinearLayout.LayoutParams(i, -2, 1.0f));
            TextView textViewTv = Ui.tv(this, "0", 26.0f, iArr[i4]);
            textViewTv.setGravity(17);
            textViewArr[i4] = textViewTv;
            linearLayout3.addView(textViewTv);
            TextView textViewTv2 = Ui.tv(this, strArr[i4], 12.0f, Ui.SUB);
            textViewTv2.setGravity(17);
            textViewTv2.setPadding(0, Ui.dp(this, 4.0f), 0, 0);
            linearLayout3.addView(textViewTv2);
            linearLayout2.addView(linearLayout3);
            i4++;
            i2 = 3;
            i3 = 1;
            i = 0;
        }
        linearLayoutCard3.addView(linearLayout2);
        linearLayoutColumn2.addView(linearLayoutCard3);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Iterator it = listScanMods.iterator();
                int i5 = 0;
                while (it.hasNext()) {
                    if (((Core.Mod) it.next()).enabled) {
                        i5++;
                    }
                }
                textViewArr[0].setText(String.valueOf(listScanMods.size()));
                textViewArr[1].setText(String.valueOf(i5));
                textViewArr[2].setText(String.valueOf(listScanMods.size() - i5));
            }
        };
        runnable.run();
        if (listScanMods.isEmpty()) {
            LinearLayout linearLayoutCard4 = Ui.card(this);
            linearLayoutCard4.addView(Ui.desc(this, "还没有安装任何模组。\n可以先装 BepInEx，再从 N 网下载模组导入。"));
            linearLayoutColumn2.addView(linearLayoutCard4);
        }
        for (final Core.Mod mod : listScanMods) {
            LinearLayout linearLayoutCard5 = Ui.card(this);
            LinearLayout linearLayout4 = new LinearLayout(this);
            linearLayout4.setOrientation(0);
            linearLayout4.setGravity(16);
            TextView textViewTv3 = Ui.tv(this, mod.name, 16.0f, Ui.TEXT);
            textViewTv3.setLayoutParams(new LinearLayout.LayoutParams(0, -2, f));
            linearLayout4.addView(textViewTv3);
            final TextView textViewTv4 = Ui.tv(this, (mod.isDir ? "目录" : "DLL") + " · " + (mod.enabled ? "已启用" : "已禁用"), 13.0f, mod.enabled ? Theme.accent() : Ui.SUB);
            Switch r13 = new Switch(this);
            r13.setChecked(mod.enabled);
            linearLayout4.addView(r13);
            linearLayoutCard5.addView(linearLayout4);
            final TextView textViewTv5 = Ui.tv(this, mod.path.getParentFile().getName() + "/" + mod.path.getName(), 12.0f, Ui.SUB);
            textViewTv5.setPadding(0, Ui.dp(this, 6.0f), 0, 0);
            linearLayoutCard5.addView(textViewTv4);
            linearLayoutCard5.addView(textViewTv5);
            r13.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                    if (z == mod.enabled) {
                        return;
                    }
                    if (Core.toggleMod(mod)) {
                        textViewTv4.setText((mod.isDir ? "目录" : "DLL") + " · " + (mod.enabled ? "已启用" : "已禁用"));
                        textViewTv4.setTextColor(mod.enabled ? Theme.accent() : Ui.SUB);
                        textViewTv5.setText(mod.path.getParentFile().getName() + "/" + mod.path.getName());
                        runnable.run();
                        return;
                    }
                    Ui.toast(MainActivity.this, "操作失败");
                    compoundButton.setChecked(mod.enabled);
                }
            });
            linearLayoutColumn2.addView(linearLayoutCard5);
            f = 1.0f;
        }
        LinearLayout linearLayoutLog = Ui.card(this);
        linearLayoutLog.addView(Ui.cardHeader(this, "运行日志", "解析 BepInEx/LogOutput.log"));
        linearLayoutLog.addView(Ui.desc(this, ModLog.summary(this)));
        final Button buttonLog = Ui.smallBtn(this, "查看详情", Ui.CARD2);
        buttonLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogSheet.show(MainActivity.this);
            }
        });
        linearLayoutLog.addView(buttonLog);
        linearLayoutColumn2.addView(linearLayoutLog);
        return Ui.scroll(this, linearLayoutColumn2);
    }

    private View tabLang() {
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.addView(header("语 言"));
        LinearLayout linearLayoutCard = Ui.card(this);
        linearLayoutCard.addView(Ui.cardHeader(this, "语言包", "语言文件位于游戏目录的 Data/Lang/ 下。\n导入后在游戏内「设置 → 语言」切换。"));
        Button buttonSmallBtn = Ui.smallBtn(this, "导入语言包", Theme.accentDark());
        buttonSmallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.pickZip(MainActivity.REQ_LANG_ZIP);
            }
        });
        linearLayoutCard.addView(buttonSmallBtn);
        linearLayoutColumn.addView(linearLayoutCard);
        List<String> listScanLangs = Core.scanLangs();
        LinearLayout linearLayoutCard2 = Ui.card(this);
        linearLayoutCard2.addView(Ui.cardHeader(this, "已安装 " + listScanLangs.size() + " 个", null));
        if (listScanLangs.isEmpty()) {
            linearLayoutCard2.addView(Ui.desc(this, "未检测到语言包。"));
        } else {
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = listScanLangs.iterator();
            while (it.hasNext()) {
                sb.append("· ").append(it.next()).append("\n");
            }
            linearLayoutCard2.addView(Ui.desc(this, sb.toString()));
        }
        linearLayoutColumn.addView(linearLayoutCard2);
        return Ui.scroll(this, linearLayoutColumn);
    }

    private View tabSet() {
        LinearLayout linearLayoutColumn = Ui.column(this);
        linearLayoutColumn.addView(header("设 置"));
        LinearLayout linearLayoutCard = Ui.card(this);
        linearLayoutCard.addView(Ui.cardHeader(this, "游戏目录", "游戏本体所在的位置。\n如果已经装在别的地方，可以在这里改。"));
        final TextView textViewTv = Ui.tv(this, Core.gameDir().getAbsolutePath(), 13.0f, Ui.SUB);
        textViewTv.setPadding(0, Ui.dp(this, 8.0f), 0, 0);
        linearLayoutCard.addView(textViewTv);
        boolean z = Core.customGameDirRaw() != null;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        Button buttonSmallBtn = Ui.smallBtn(this, "修改", Theme.accentDark());
        buttonSmallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.showGameDirDialog(textViewTv, true);
            }
        });
        linearLayout.addView(buttonSmallBtn);
        if (z) {
            Button buttonSmallBtn2 = Ui.smallBtn(this, "恢复默认", Ui.CARD2);
            buttonSmallBtn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Core.setCustomDir(MainActivity.this, null);
                    textViewTv.setText(Core.gameDir().getAbsolutePath());
                    textViewTv.setTextColor(Ui.SUB);
                    Ui.toast(MainActivity.this, "已恢复默认目录");
                }
            });
            linearLayout.addView(buttonSmallBtn2);
        }
        linearLayoutCard.addView(linearLayout);
        if (z) {
            TextView textViewTv2 = Ui.tv(this, "（自定义）", 12.0f, Theme.accent());
            textViewTv2.setPadding(0, Ui.dp(this, 4.0f), 0, 0);
            linearLayoutCard.addView(textViewTv2);
        }
        linearLayoutColumn.addView(linearLayoutCard);
        LinearLayout linearLayoutCard2 = Ui.card(this);
        linearLayoutCard2.addView(Ui.cardHeader(this, "存档备份上限", "备份太多会占空间。\n达到上限后将无法再备份，需要先删除旧的。\n\n范围 1 ~ 255，默认 10。"));
        TextView textViewTv3 = Ui.tv(this, "当前：最多保留 " + Core.backupLimit(this) + " 份", 13.0f, Ui.SUB);
        textViewTv3.setPadding(0, Ui.dp(this, 8.0f), 0, 0);
        linearLayoutCard2.addView(textViewTv3);
        Button buttonSmallBtn3 = Ui.smallBtn(this, "修改", Ui.CARD2);
        buttonSmallBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText editText = new EditText(MainActivity.this);
                editText.setText(String.valueOf(Core.backupLimit(MainActivity.this)));
                editText.setInputType(2);
                editText.setSingleLine(true);
                int iDp = Ui.dp(MainActivity.this, 16.0f);
                editText.setPadding(iDp, iDp, iDp, iDp);
                new AlertDialog.Builder(MainActivity.this).setTitle("备份上限").setMessage("范围 1 ~ 255").setView(editText).setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int i2;
                        try {
                            i2 = Integer.parseInt(editText.getText().toString().trim());
                        } catch (Throwable th) {
                            i2 = 10;
                        }
                        if (i2 < 1) {
                            i2 = 1;
                        }
                        if (i2 > 255) {
                            i2 = 255;
                        }
                        Core.setBackupLimit(MainActivity.this, i2);
                        Core.pruneBackups(MainActivity.this);
                        Ui.toast(MainActivity.this, "已设为 " + i2 + " 份");
                        MainActivity.this.showMain(3);
                    }
                }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
            }
        });
        linearLayoutCard2.addView(buttonSmallBtn3);
        linearLayoutColumn.addView(linearLayoutCard2);
        LinearLayout linearLayoutCard3 = Ui.card(this);
        linearLayoutCard3.addView(Ui.cardHeader(this, "重新开始", "清除安装记录，重新进入安装向导。\n不会删除已安装的文件。"));
        Button buttonSmallBtn4 = Ui.smallBtn(this, "重置向导", Ui.CARD2);
        buttonSmallBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.sp.edit().clear().apply();
                MainActivity.this.showHome();
            }
        });
        linearLayoutCard3.addView(buttonSmallBtn4);
        linearLayoutColumn.addView(linearLayoutCard3);
        LinearLayout linearLayoutCard4 = Ui.card(this);
        linearLayoutCard4.addView(Ui.cardHeader(this, "关于", null));
        linearLayoutCard4.addView(Ui.desc(this, "CU Installer v" + appVersion() + "\n作者 若白令狐\n\n免费开源软件（MIT 许可），可自由修改与分发。\n名称、图标与署名归作者所有，未经许可不得用于衍生分发。"));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        int iDp = Ui.dp(this, 44.0f);
        Button buttonSmallBtn5 = Ui.smallBtn(this, "作者主页", Ui.CARD2);
        buttonSmallBtn5.setMinHeight(0);
        buttonSmallBtn5.setMinimumHeight(0);
        buttonSmallBtn5.setHeight(iDp);
        buttonSmallBtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.openUrl(Obf.d(MainActivity.URL_AUTHOR_ENC));
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, iDp, 1.0f);
        layoutParams.rightMargin = Ui.dp(this, 12.0f);
        linearLayout2.addView(buttonSmallBtn5, layoutParams);
        Button buttonSmallBtn6 = Ui.smallBtn(this, "赞助作者", -7050010);
        buttonSmallBtn6.setMinHeight(0);
        buttonSmallBtn6.setMinimumHeight(0);
        buttonSmallBtn6.setHeight(iDp);
        buttonSmallBtn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.showSponsorDialog();
            }
        });
        linearLayout2.addView(buttonSmallBtn6, new LinearLayout.LayoutParams(0, iDp, 1.0f));
        LinkRowStyle(linearLayout2);
        linearLayoutCard4.addView(linearLayout2);
        linearLayoutColumn.addView(linearLayoutCard4);
        Skin.attach(this, linearLayoutColumn);
        Tools.attach(this, linearLayoutColumn);
        Instances.attach(this, linearLayoutColumn);
        return Ui.scroll(this, linearLayoutColumn);
    }

    private void LinkRowStyle(LinearLayout linearLayout) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dp(this, 12.0f);
        linearLayout.setLayoutParams(layoutParams);
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(-14537672);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 1.0f));
        layoutParams.topMargin = Ui.dp(this, 16.0f);
        layoutParams.bottomMargin = Ui.dp(this, 4.0f);
        view.setLayoutParams(layoutParams);
        return view;
    }

    private View infoRow(String str, String str2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(0, Ui.dp(this, 10.0f), 0, Ui.dp(this, 10.0f));
        TextView textViewTv = Ui.tv(this, str, 13.0f, Ui.SUB);
        textViewTv.setMinWidth(Ui.dp(this, 64.0f));
        linearLayout.addView(textViewTv);
        TextView textViewTv2 = Ui.tv(this, str2, 13.5f, Ui.TEXT);
        textViewTv2.setGravity(8388613);
        linearLayout.addView(textViewTv2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        return linearLayout;
    }

    public void showSponsorDialog() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int iDp = Ui.dp(this, 24.0f);
        linearLayout.setPadding(iDp, Ui.dp(this, 8.0f), iDp, 0);
        TextView textViewTv = Ui.tv(this, "你的投喂就是作者最大的动力", 17.0f, Ui.TEXT);
        textViewTv.setGravity(17);
        textViewTv.setTypeface(Typeface.DEFAULT_BOLD);
        textViewTv.setLineSpacing(Ui.dp(this, 5.0f), 1.0f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = Ui.dp(this, 4.0f);
        linearLayout.addView(textViewTv, layoutParams);
        TextView textViewTv2 = Ui.tv(this, "5块钱可以让作者开心很久的！", 14.0f, Ui.SUB);
        textViewTv2.setGravity(17);
        textViewTv2.setLineSpacing(Ui.dp(this, 4.0f), 1.0f);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = Ui.dp(this, 10.0f);
        linearLayout.addView(textViewTv2, layoutParams2);
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(linearLayout).create();
        alertDialogCreate.show();
        if (alertDialogCreate.getWindow() != null) {
            alertDialogCreate.getWindow().setBackgroundDrawable(Ui.round(Ui.CARD, Ui.dp(this, 22.0f)));
        }
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = Ui.dp(this, 22.0f);
        layoutParams3.bottomMargin = Ui.dp(this, 20.0f);
        linearLayout2.setLayoutParams(layoutParams3);
        Button buttonBtn = Ui.btn(this, "投  喂", -7050010);
        buttonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialogCreate.dismiss();
                MainActivity.this.openUrl(Obf.d(MainActivity.URL_AFDIAN_ENC));
            }
        });
        Button buttonBtn2 = Ui.btn(this, "下次一定", Ui.CARD2);
        buttonBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialogCreate.dismiss();
            }
        });
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams4.rightMargin = Ui.dp(this, 12.0f);
        linearLayout2.addView(buttonBtn, layoutParams4);
        linearLayout2.addView(buttonBtn2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(linearLayout2);
    }

    public void showGameDirDialog(final TextView textView, final boolean z) {
        final EditText editText = new EditText(this);
        editText.setText(Core.gameDir().getAbsolutePath());
        editText.setSingleLine(false);
        editText.setTextSize(13.0f);
        int iDp = Ui.dp(this, 16.0f);
        editText.setPadding(iDp, iDp, iDp, iDp);
        new AlertDialog.Builder(this).setTitle("修改游戏目录").setMessage("输入游戏所在文件夹的完整路径。\n留空则恢复默认。").setView(editText).setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String strTrim = editText.getText().toString().trim();
                if (strTrim.length() == 0) {
                    Core.setCustomDir(MainActivity.this, null);
                } else {
                    if (!strTrim.startsWith("/")) {
                        Ui.alert(MainActivity.this, "路径无效", "请填写以 / 开头的完整路径，例如：\n/storage/emulated/0/Download/Casualties Unknown_7.0.1Demo");
                        return;
                    }
                    File file = new File(strTrim);
                    boolean zStartsWith = strTrim.startsWith("/data/");
                    if (!(zStartsWith ? Root.exists(strTrim) : file.exists())) {
                        if (zStartsWith) {
                            Ui.alert(MainActivity.this, "找不到目录", "找不到这个位置，请检查路径是否正确。");
                            return;
                        } else if (!file.mkdirs()) {
                            Ui.alert(MainActivity.this, "无法创建目录", "该路径无法创建，请检查父目录是否存在、是否有写入权限。");
                            return;
                        }
                    }
                    Core.setCustomDir(MainActivity.this, strTrim);
                }
                if (textView != null) {
                    textView.setText(Core.gameDir().getAbsolutePath());
                    textView.setTextColor(Ui.SUB);
                }
                Ui.toast(MainActivity.this, "已保存");
                boolean z2 = z;
                MainActivity mainActivity = MainActivity.this;
                if (z2) {
                    mainActivity.showMain(3);
                } else {
                    mainActivity.showInstall();
                }
            }
        }).setNeutralButton("用默认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Core.setCustomDir(MainActivity.this, null);
                Ui.toast(MainActivity.this, "已恢复默认目录");
                boolean z2 = z;
                MainActivity mainActivity = MainActivity.this;
                if (z2) {
                    mainActivity.showMain(3);
                } else {
                    mainActivity.showInstall();
                }
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable th) {
            return "?";
        }
    }

    private View header(String str) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(Ui.CARD);
        int iDp = Ui.dp(this, 18.0f);
        linearLayout.setPadding(iDp, Ui.dp(this, 22.0f), iDp, iDp);
        linearLayout.addView(Ui.tv(this, str, 20.0f, Ui.TEXT));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(-Ui.dp(this, 14.0f), -Ui.dp(this, 14.0f), -Ui.dp(this, 14.0f), Ui.dp(this, 8.0f));
        linearLayout.setLayoutParams(layoutParams);
        return linearLayout;
    }

    private View[] buildProgressOverlay(String str) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setBackgroundColor(-300870116);
        linearLayout.setPadding(Ui.dp(this, 32.0f), Ui.dp(this, 32.0f), Ui.dp(this, 32.0f), Ui.dp(this, 32.0f));
        TextView textViewTv = Ui.tv(this, str, 18.0f, Ui.TEXT);
        textViewTv.setGravity(17);
        linearLayout.addView(textViewTv);
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 8.0f));
        layoutParams.setMargins(0, Ui.dp(this, 20.0f), 0, Ui.dp(this, 12.0f));
        progressBar.setLayoutParams(layoutParams);
        linearLayout.addView(progressBar);
        TextView textViewTv2 = Ui.tv(this, "", 13.0f, Ui.SUB);
        textViewTv2.setGravity(17);
        linearLayout.addView(textViewTv2);
        return new View[]{linearLayout, textViewTv2, progressBar};
    }

    public boolean hasAllFiles() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                return Environment.isExternalStorageManager();
            }
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    public void askAllFiles() {
        new AlertDialog.Builder(this).setTitle("需要存储权限").setMessage("安装游戏需要「所有文件访问」权限。\n点击「去授权」后在系统设置里打开开关。").setPositiveButton("去授权", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    MainActivity.this.pendingPermRefresh = true;
                    Intent intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                    intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                    MainActivity.this.startActivity(intent);
                } catch (Throwable th) {
                    try {
                        MainActivity.this.startActivity(new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"));
                    } catch (Throwable th2) {
                        Ui.alert(MainActivity.this, "打不开设置", String.valueOf(th2));
                    }
                }
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    public void openUrl(String str) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
        } catch (Throwable th) {
            Ui.toast(this, "无法打开链接");
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        Skin.onResult(this, i, i2, intent);
        if (i == 9201) {
            Instances.onResult(this, i, i2, intent);
            return;
        }
        if (i == 9202) {
            Instances.onResult(this, i, i2, intent);
            return;
        }
        if (i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        Uri data = intent.getData();
        try {
            if (i == REQ_GAME_ZIP) {
                if (hasAllFiles()) {
                    deployFromUri(data);
                    return;
                } else {
                    askAllFiles();
                    return;
                }
            }
            if (i == REQ_BEP_ZIP) {
                deployBepFromUri(data);
                return;
            }
            if (i != REQ_MOD_ZIP && i != REQ_LANG_ZIP) {
                if (i == REQ_RESTORE) {
                    doRestore(data);
                    return;
                }
                if (i == REQ_IMPORT_SAVE) {
                    doImportSave(data);
                    return;
                }
                if (i == REQ_EXPORT && this.pendingExport != null) {
                    try {
                        FileInputStream fileInputStream = new FileInputStream(this.pendingExport);
                        OutputStream outputStreamOpenOutputStream = getContentResolver().openOutputStream(data);
                        byte[] bArr = new byte[65536];
                        while (true) {
                            int i3 = fileInputStream.read(bArr);
                            if (i3 <= 0) {
                                break;
                            } else {
                                outputStreamOpenOutputStream.write(bArr, 0, i3);
                            }
                        }
                        fileInputStream.close();
                        outputStreamOpenOutputStream.close();
                        int iCleanupDuplicates = Core.cleanupDuplicates();
                        Ui.toast(this, iCleanupDuplicates > 0 ? "已导出（清理了 " + iCleanupDuplicates + " 个重复副本）" : "已导出");
                        showMain(0);
                    } catch (Throwable th) {
                        Ui.alert(this, "导出失败", String.valueOf(th));
                    }
                    this.pendingExport = null;
                    return;
                }
                return;
            }
            importFile(data, false);
        } catch (Throwable th2) {
            Ui.alert(this, "导入失败", String.valueOf(th2));
        }
    }

    private void deployFromUri(Uri uri) {
        this.srcUri = uri;
        this.srcFile = null;
        deployGame();
    }

    private void deployBepFromUri(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Installer.installBepInEx(MainActivity.this.getContentResolver().openInputStream(uri), null);
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Ui.toast(MainActivity.this, "BepInEx 已安装");
                        }
                    });
                } catch (Throwable th) {
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Ui.alert(MainActivity.this, "安装失败", String.valueOf(th));
                        }
                    });
                }
            }
        }).start();
    }

    public String queryName(Uri uri) {
        Uri uri2;
        int columnIndex;
        try {
            uri2 = uri;
            try {
                Cursor cursorQuery = getContentResolver().query(uri2, null, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst() && (columnIndex = cursorQuery.getColumnIndex("_display_name")) >= 0) {
                            return cursorQuery.getString(columnIndex);
                        }
                        cursorQuery.close();
                    } finally {
                        cursorQuery.close();
                    }
                }
            } catch (Throwable th) {
            }
        } catch (Throwable th2) {
            uri2 = uri;
        }
        String path = uri2.getPath();
        return path == null ? "unknown" : path.substring(path.lastIndexOf(47) + 1);
    }

    public boolean zipContains(Uri uri, String str) {
        ZipEntry nextEntry;
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(uri)));
            do {
                try {
                    nextEntry = zipInputStream.getNextEntry();
                    if (nextEntry == null) {
                        zipInputStream.close();
                        return false;
                    }
                } catch (Throwable th) {
                    zipInputStream.close();
                    throw th;
                }
            } while (!nextEntry.getName().startsWith(str));
            zipInputStream.close();
            return true;
        } catch (Throwable th2) {
            return false;
        }
    }

    private void importFile(final Uri uri, final boolean z) {
        try {
            importFile0(uri, z);
        } catch (Throwable t) {
            Log.e("CUImport", "import failed: " + t, t);
            final String msg = String.valueOf(t);
            this.ui.post(new Runnable() {
                @Override
                public void run() {
                    Ui.alert(MainActivity.this, "导入失败", msg);
                    MainActivity.this.showMain(1);
                }
            });
        }
    }

    private void importFile0(final Uri uri, final boolean z) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                String strQueryName;
                String lowerCase;
                boolean zEndsWith;
                boolean z2;
                boolean z3;
                boolean z4;
                boolean z5;
                boolean z6;
                String str;
                FileOutputStream fileOutputStream;
                InputStream inputStreamOpenInputStream;
                boolean z7;
                Archive archive = null;
                boolean z8;
                File fileGameDir;
                try {
                    strQueryName = MainActivity.this.queryName(uri);
                    lowerCase = strQueryName.toLowerCase();
                    byte[] bArr = new byte[4];
                    InputStream inputStreamOpenInputStream2 = MainActivity.this.getContentResolver().openInputStream(uri);
                    try {
                        int i = inputStreamOpenInputStream2.read(bArr);
                        try {
                            inputStreamOpenInputStream2.close();
                        } catch (Throwable th) {
                        }
                        boolean z9 = i >= 4 && bArr[0] == 80 && bArr[1] == 75;
                        boolean z10 = i >= 4 && bArr[0] == 82 && bArr[1] == 97 && bArr[2] == 114 && bArr[3] == 33;
                        boolean z11 = i >= 2 && bArr[0] == 77 && bArr[1] == 90;
                        zEndsWith = lowerCase.endsWith(".json");
                        z2 = z10;
                        z3 = z11;
                        z4 = z9;
                    } finally {
                    }
                } catch (Throwable th2) {
                    Log.e("CUImport", "import failed: " + th2, th2);
                    MainActivity.this.ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Ui.alert(MainActivity.this, "导入失败", "请查看日志或重试。");
                            if (z) {
                                MainActivity.this.showMain(1);
                            }
                        }
                    });
                    return;
                }
                if (Core.isRootPath()) {
                    z5 = zEndsWith;
                    File file = new File(MainActivity.this.getCacheDir(), "imp_" + System.currentTimeMillis());
                    file.mkdirs();
                    if (z4) {
                        fileGameDir = MainActivity.this.zipContains(uri, "BepInEx/") || MainActivity.this.zipContains(uri, "BepInEx\\") ? Core.gameDir() : lowerCase.contains("lang") || MainActivity.this.zipContains(uri, "Lang/") ? Core.langDir() : Core.pluginsDir();
                        Installer.unzip(MainActivity.this.getContentResolver().openInputStream(uri), file, null);
                    } else {
                        if (z2) {
                            File file2 = new File(file, "a.rar");
                            fileOutputStream = new FileOutputStream(file2);
                            inputStreamOpenInputStream = MainActivity.this.getContentResolver().openInputStream(uri);
                            try {
                                byte[] bArr2 = new byte[65536];
                                while (true) {
                                    int i2 = inputStreamOpenInputStream.read(bArr2);
                                    if (i2 > 0) {
                                        fileOutputStream.write(bArr2, 0, i2);
                                    } else {
                                        try {
                                            break;
                                        } catch (Throwable th3) {
                                        }
                                    }
                                }
                                inputStreamOpenInputStream.close();
                                fileOutputStream.close();
                                try {
                                    Archive archive2 = new Archive(file2);
                                    try {
                                        Iterator<FileHeader> it = archive2.getFileHeaders().iterator();
                                        while (true) {
                                            if (!it.hasNext()) {
                                                z8 = false;
                                                break;
                                            }
                                            String fileName = it.next().getFileName();
                                            if (fileName != null && fileName.replace("\\", "/").startsWith("BepInEx/")) {
                                                z8 = true;
                                                break;
                                            }
                                        }
                                        try {
                                            archive2.close();
                                        } catch (Throwable th4) {
                                        }
                                    } catch (Throwable th5) {
                                        archive2.close();
                                        throw th5;
                                    }
                                } catch (Throwable th6) {
                                    z8 = false;
                                }
                                fileGameDir = z8 ? Core.gameDir() : Core.pluginsDir();
                                Junrar.extract(file2, file);
                                file2.delete();
                            } finally {
                            }
                        } else {
                            if (z3) {
                                if (!lowerCase.endsWith(".dll")) {
                                    strQueryName = strQueryName + ".dll";
                                }
                                File file3 = new File(file, strQueryName);
                                FileOutputStream fileOutputStream2 = new FileOutputStream(file3);
                                InputStream inputStreamOpenInputStream3 = MainActivity.this.getContentResolver().openInputStream(uri);
                                try {
                                    byte[] bArr3 = new byte[65536];
                                    while (true) {
                                        int i3 = inputStreamOpenInputStream3.read(bArr3);
                                        if (i3 > 0) {
                                            fileOutputStream2.write(bArr3, 0, i3);
                                        } else {
                                            try {
                                                break;
                                            } catch (Throwable th7) {
                                            }
                                        }
                                    }
                                    inputStreamOpenInputStream3.close();
                                    fileOutputStream2.close();
                                    final String str2 = Root.copyFile(file3, new StringBuilder().append(Core.pluginsDir().getAbsolutePath()).append("/").append(strQueryName).toString()) ? "已安装 " + strQueryName : "安装失败";
                                    MainActivity.this.ui.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Ui.toast(MainActivity.this, str2);
                                            MainActivity.this.showMain(1);
                                        }
                                    });
                                    return;
                                } finally {
                                }
                            }
                            if (z5) {
                                File file4 = new File(file, strQueryName);
                                FileOutputStream fileOutputStream3 = new FileOutputStream(file4);
                                InputStream inputStreamOpenInputStream4 = MainActivity.this.getContentResolver().openInputStream(uri);
                                try {
                                    byte[] bArr4 = new byte[65536];
                                    while (true) {
                                        int i4 = inputStreamOpenInputStream4.read(bArr4);
                                        if (i4 > 0) {
                                            fileOutputStream3.write(bArr4, 0, i4);
                                        } else {
                                            try {
                                                break;
                                            } catch (Throwable th8) {
                                            }
                                        }
                                    }
                                    inputStreamOpenInputStream4.close();
                                    fileOutputStream3.close();
                                    str = Root.copyFile(file4, new StringBuilder().append(Core.langDir().getAbsolutePath()).append("/").append(strQueryName).toString()) ? "已导入语言包 " + strQueryName : "导入失败";
                                    MainActivity.this.ui.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Ui.toast(MainActivity.this, str);
                                            MainActivity.this.showMain(2);
                                        }
                                    });
                                    return;
                                } finally {
                                    try {
                                        inputStreamOpenInputStream4.close();
                                    } catch (Throwable th9) {
                                    }
                                    fileOutputStream3.close();
                                }
                            }
                            throw new IOException("无法识别的文件类型：" + strQueryName);
                        }
                        MainActivity.this.ui.post(new Runnable() {
                            @Override
                            public void run() {
                                Ui.alert(MainActivity.this, "导入失败", "请查看日志或重试。");
                                if (z) {
                                    MainActivity.this.showMain(1);
                                }
                            }
                        });
                        return;
                    }
                    str = Root.copyDirContents(file, fileGameDir.getAbsolutePath()) ? "已导入到 " + fileGameDir.getName() + "/" : "导入失败";
                    z6 = false;
                } else {
                    z5 = zEndsWith;
                    if (z4) {
                        File fileGameDir2 = MainActivity.this.zipContains(uri, "BepInEx/") || MainActivity.this.zipContains(uri, "BepInEx\\") ? Core.gameDir() : lowerCase.contains("lang") || MainActivity.this.zipContains(uri, "Lang/") ? Core.langDir() : Core.pluginsDir();
                        fileGameDir2.mkdirs();
                        Installer.unzip(MainActivity.this.getContentResolver().openInputStream(uri), fileGameDir2, null);
                        str = "已解压到 " + fileGameDir2.getName() + "/";
                        z6 = false;
                    } else if (z3) {
                        File filePluginsDir = Core.pluginsDir();
                        filePluginsDir.mkdirs();
                        if (!lowerCase.endsWith(".dll")) {
                            strQueryName = strQueryName + ".dll";
                        }
                        FileOutputStream fileOutputStream4 = new FileOutputStream(new File(filePluginsDir, strQueryName));
                        InputStream inputStreamOpenInputStream5 = MainActivity.this.getContentResolver().openInputStream(uri);
                        try {
                            byte[] bArr5 = new byte[65536];
                            while (true) {
                                int i5 = inputStreamOpenInputStream5.read(bArr5);
                                if (i5 > 0) {
                                    fileOutputStream4.write(bArr5, 0, i5);
                                } else {
                                    try {
                                        break;
                                    } catch (Throwable th10) {
                                    }
                                }
                            }
                            inputStreamOpenInputStream5.close();
                            fileOutputStream4.close();
                            str = "已安装 " + strQueryName;
                            z6 = false;
                        } finally {
                            try {
                                inputStreamOpenInputStream5.close();
                            } catch (Throwable th11) {
                            }
                            fileOutputStream4.close();
                        }
                    } else if (z2) {
                        File file5 = new File(MainActivity.this.getCacheDir(), "imp_" + System.currentTimeMillis() + ".rar");
                        fileOutputStream = new FileOutputStream(file5);
                        inputStreamOpenInputStream = MainActivity.this.getContentResolver().openInputStream(uri);
                        try {
                            byte[] bArr6 = new byte[65536];
                            while (true) {
                                int i6 = inputStreamOpenInputStream.read(bArr6);
                                if (i6 > 0) {
                                    fileOutputStream.write(bArr6, 0, i6);
                                } else {
                                    try {
                                        break;
                                    } catch (Throwable th12) {
                                    }
                                }
                            }
                            inputStreamOpenInputStream.close();
                            fileOutputStream.close();
                            try {
                                archive = new Archive(file5);
                            } catch (Throwable th13) {
                                z7 = false;
                            }
                            try {
                                Iterator<FileHeader> it2 = archive.getFileHeaders().iterator();
                                while (true) {
                                    if (!it2.hasNext()) {
                                        z7 = false;
                                        break;
                                    }
                                    String fileName2 = it2.next().getFileName();
                                    if (fileName2 != null && fileName2.replace('\\', '/').startsWith("BepInEx/")) {
                                        z7 = true;
                                        break;
                                    }
                                }
                                File fileGameDir3 = z7 ? Core.gameDir() : Core.pluginsDir();
                                fileGameDir3.mkdirs();
                                Junrar.extract(file5, fileGameDir3);
                                file5.delete();
                                str = "已解压 rar 到 " + fileGameDir3.getName() + "/";
                                z6 = false;
                            } finally {
                                archive.close();
                            }
                        } finally {
                            try {
                                inputStreamOpenInputStream.close();
                            } catch (Throwable th14) {
                            }
                            fileOutputStream.close();
                        }
                    } else if (z5) {
                        File fileLangDir = Core.langDir();
                        fileLangDir.mkdirs();
                        FileOutputStream fileOutputStream5 = new FileOutputStream(new File(fileLangDir, strQueryName));
                        InputStream inputStreamOpenInputStream6 = MainActivity.this.getContentResolver().openInputStream(uri);
                        try {
                            byte[] bArr7 = new byte[65536];
                            while (true) {
                                int i7 = inputStreamOpenInputStream6.read(bArr7);
                                if (i7 <= 0) {
                                    break;
                                } else {
                                    fileOutputStream5.write(bArr7, 0, i7);
                                }
                            }
                            z6 = false;
                            try {
                                inputStreamOpenInputStream6.close();
                            } catch (Throwable th15) {
                            }
                            fileOutputStream5.close();
                            str = "已导入语言包 " + strQueryName;
                        } finally {
                            try {
                                inputStreamOpenInputStream6.close();
                            } catch (Throwable th16) {
                            }
                            fileOutputStream5.close();
                        }
                    } else {
                        throw new IOException("无法识别的文件类型：" + strQueryName);
                    }
                }
                if (z5 || lowerCase.contains("lang")) {
                    z6 = true;
                }
                final String fstr = str;
                final boolean fz6 = z6;
                MainActivity.this.ui.post(new Runnable() {
                    @Override
                    public void run() {
                        Ui.toast(MainActivity.this, fstr);
                        MainActivity.this.showMain(fz6 ? 2 : 1);
                    }
                });
                } catch (Throwable t) {
                    android.util.Log.e("CUImport", "import failed: " + t, t);
                }
            }
        }).start();
    }
}
