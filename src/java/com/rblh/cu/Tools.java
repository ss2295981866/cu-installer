package com.rblh.cu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.rblh.cu.Skin;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class Tools {
    private static final String API = "https://api.github.com/repos/ss2295981866/cu-installer/releases/latest";
    private static final String REPO = "ss2295981866/cu-installer";

    public static void attach(Context context, LinearLayout linearLayout) {
        try {
            LinearLayout linearLayoutCard = Ui.card(context);
            linearLayoutCard.addView(Ui.cardHeader(context, "更新与诊断", "检查新版本、导出运行日志。"));
            linearLayoutCard.addView(Ui.desc(context, "当前版本 " + versionName(context) + "\u3000仓库 " + REPO));
            linearLayoutCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Tools.menu((Activity) view.getContext());
                }
            });
            linearLayout.addView(linearLayoutCard);
        } catch (Throwable th) {
        }
    }

    public static void menu(final Activity activity) {
        Skin.modal(activity, "更新与诊断", new String[]{"检查更新", "导出日志"}, -1, new Skin.Picker() {
            @Override
            public void pick(int i) {
                if (i != 0) {
                    Tools.exportLog(activity);
                } else {
                    Tools.checkUpdate(activity);
                }
            }
        });
    }

    static String versionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable th) {
            return "?";
        }
    }

    
    static class AnonymousClass3 implements Runnable {
        final /* synthetic */ Activity val$a;
        final /* synthetic */ String val$cur;

        AnonymousClass3(String str, Activity activity) {
            this.val$cur = str;
            this.val$a = activity;
        }

        @Override
        public void run() {
            try {
                JSONObject jSONObject = new JSONObject(Tools.http(Tools.API));
                final String strOptString = jSONObject.optString("tag_name", "");
                final String strOptString2 = jSONObject.optString("body", "");
                final String strFindApkUrl = Tools.findApkUrl(jSONObject);
                final int[] vLatest = Tools.ver(strOptString);
                final int[] vCurrent = Tools.ver(this.val$cur);
                this.val$a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Tools.cmpVer(vLatest, vCurrent) > 0 && strFindApkUrl != null) {
                            AlertDialog.Builder title = new AlertDialog.Builder(AnonymousClass3.this.val$a).setTitle("发现新版本 " + strOptString);
                            String str = strOptString2;
                            title.setMessage((str == null || str.trim().length() == 0) ? "是否下载并安装？" : strOptString2).setPositiveButton("下载并安装", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Tools.download(AnonymousClass3.this.val$a, strFindApkUrl);
                                }
                            }).setNegativeButton("打开页面", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Tools.openUrl(AnonymousClass3.this.val$a, "https://github.com/ss2295981866/cu-installer/releases");
                                }
                            }).setNeutralButton("取消", (DialogInterface.OnClickListener) null).show();
                            return;
                        }
                        AlertDialog.Builder title2 = new AlertDialog.Builder(AnonymousClass3.this.val$a).setTitle("已是最新版本");
                        StringBuilder sb = new StringBuilder();
                        sb.append("当前 ");
                        sb.append(AnonymousClass3.this.val$cur);
                        sb.append("，仓库最新 ");
                        sb.append(strOptString.length() == 0 ? "?" : strOptString);
                        title2.setMessage(sb.toString()).setPositiveButton("好", (DialogInterface.OnClickListener) null).show();
                    }
                });
            } catch (Throwable th) {
                Log.e("Tools", "check failed", th);
                this.val$a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Tools.toast(AnonymousClass3.this.val$a, "检查失败：" + th);
                    }
                });
            }
        }
    }

    static void checkUpdate(Activity activity) {
        new Thread(new AnonymousClass3(versionName(activity), activity)).start();
    }

    static int[] ver(String str) {
        int[] v = new int[3];
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if ((c >= '0' && c <= '9') || c == '.') {
                    sb.append(c);
                }
            }
            String[] parts = sb.toString().split("\\.");
            for (int i = 0; i < 3 && i < parts.length; i++) {
                try {
                    v[i] = Integer.parseInt(parts[i]);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return v;
    }

    static int cmpVer(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return a[i] - b[i];
            }
        }
        return 0;
    }

    static String findApkUrl(JSONObject jSONObject) {
        JSONArray jSONArrayOptJSONArray = null;
        try {
            jSONArrayOptJSONArray = jSONObject.optJSONArray("assets");
            if (jSONArrayOptJSONArray == null) {
                return null;
            }
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i);
                if (jSONObject2.optString("name", "").endsWith(".apk")) {
                    return jSONObject2.optString("browser_download_url");
                }
            }
        } catch (Throwable th) {
        }
        return null;
    }

    static String http(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(15000);
        httpURLConnection.setRequestProperty("User-Agent", "cu-installer");
        httpURLConnection.setRequestProperty("Accept", "application/vnd.github+json");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null) {
                bufferedReader.close();
                return sb.toString();
            }
            sb.append(line);
        }
    }

    static void download(final Activity activity, final String str) {
        toast(activity, "开始下载更新…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
                    httpURLConnection.setConnectTimeout(10000);
                    httpURLConnection.setReadTimeout(60000);
                    httpURLConnection.setRequestProperty("User-Agent", "cu-installer");
                    httpURLConnection.setInstanceFollowRedirects(true);
                    File file = new File(activity.getCacheDir(), "update.apk");
                    InputStream inputStream = httpURLConnection.getInputStream();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    byte[] bArr = new byte[65536];
                    while (true) {
                        int i = inputStream.read(bArr);
                        if (i <= 0) {
                            fileOutputStream.close();
                            inputStream.close();
                            if (file.length() < 50000) {
                                throw new java.io.IOException("安装包不完整");
                            }
                            Installer.installApk(activity, file);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Tools.toast(activity, "已发起安装，请按系统提示操作");
                                }
                            });
                            return;
                        }
                        fileOutputStream.write(bArr, 0, i);
                    }
                } catch (Throwable th) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Tools.toast(activity, "下载失败：" + th);
                        }
                    });
                }
            }
        }).start();
    }

    static void exportLog(final Activity activity) {
        toast(activity, "正在导出日志…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String absolutePath;
                try {
                    String str = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
                    File file = new File("/storage/emulated/0/Download/cu-logs");
                    file.mkdirs();
                    File file2 = new File(file, "cu-log-" + str + ".txt");
                    StringBuilder sb = new StringBuilder();
                    sb.append("app: ");
                    sb.append(activity.getPackageName());
                    sb.append(" v");
                    sb.append(Tools.versionName(activity));
                    sb.append("\n");
                    sb.append("device: ");
                    sb.append(Build.MANUFACTURER);
                    sb.append(" ");
                    sb.append(Build.MODEL);
                    sb.append("\n");
                    sb.append("android: ");
                    sb.append(Build.VERSION.RELEASE);
                    sb.append(" (SDK ");
                    sb.append(Build.VERSION.SDK_INT);
                    sb.append(")\n");
                    sb.append("gameDir: ");
                    sb.append(Core.gameDir().getAbsolutePath());
                    sb.append(" exists=");
                    sb.append(Core.gameDir().exists());
                    sb.append("\n");
                    sb.append("\n--- prefs ---\n");
                    String[] strArr = {"cu_installer", "cu_theme", "cu_skin"};
                    int i = 0;
                    for (int i2 = 0; i2 < 3; i2++) {
                        SharedPreferences sharedPreferences = activity.getSharedPreferences(strArr[i2], 0);
                        sb.append(strArr[i2]);
                        sb.append(": ");
                        sb.append(sharedPreferences.getAll().toString());
                        sb.append("\n");
                    }
                    sb.append("\n--- logcat ---\n");
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-t", "2000"}).getInputStream()));
                        while (true) {
                            String line = bufferedReader.readLine();
                            if (line == null || i >= 3000) {
                                break;
                            }
                            sb.append(line);
                            sb.append("\n");
                            i++;
                        }
                        bufferedReader.close();
                    } catch (Throwable th) {
                        sb.append("logcat failed: ");
                        sb.append(th);
                        sb.append("\n");
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file2);
                    fileOutputStream.write(sb.toString().getBytes("UTF-8"));
                    fileOutputStream.close();
                    absolutePath = file2.getAbsolutePath();
                } catch (Throwable th2) {
                    absolutePath = null;
                }
                final String absolutePathFinal = absolutePath;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String str2;
                        Activity activity2 = activity;
                        if (absolutePathFinal != null) {
                            str2 = "已导出：" + absolutePathFinal;
                        } else {
                            str2 = "导出失败";
                        }
                        Tools.toast(activity2, str2);
                    }
                });
            }
        }).start();
    }

    static void openUrl(Context context, String str) {
        try {
            context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)).addFlags(268435456));
        } catch (Throwable th) {
        }
    }

    static void toast(Context context, String str) {
        try {
            Toast.makeText(context, str, 1).show();
        } catch (Throwable th) {
        }
    }
}
