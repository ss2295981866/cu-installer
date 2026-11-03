package com.rblh.cu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.rblh.cu.Packer;
import com.rblh.cu.Skin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class Instances {
    public static final int REQ = 9201;
    public static final int REQ_DIR = 9202;

    public static void attach(final Activity activity, LinearLayout linearLayout) {
        try {
            LinearLayout linearLayoutCard = Ui.card(activity);
            linearLayoutCard.addView(Ui.cardHeader(activity, "游戏实例", "切换实例、选择已有目录或安装新游戏。"));
            linearLayoutCard.addView(Ui.desc(activity, summary()));
            linearLayoutCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Instances.manage(activity);
                }
            });
            linearLayout.addView(linearLayoutCard);
        } catch (Throwable th) {
        }
    }

    static String summary() {
        List<File> listScan = scan();
        File fileGameDir = Core.gameDir();
        return "当前：" + (fileGameDir == null ? "?" : fileGameDir.getName()) + "\u3000共 " + listScan.size() + " 个实例";
    }

    static boolean hasExe(File file) {
        if (new File(file, "CasualtiesUnknown.exe").isFile()) {
            return true;
        }
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return false;
        }
        for (int i = 0; i < fileArrListFiles.length; i++) {
            if (fileArrListFiles[i].isFile() && fileArrListFiles[i].getName().equalsIgnoreCase("CasualtiesUnknown.exe")) {
                return true;
            }
        }
        return false;
    }

    static void add(List<File> list, File file) {
        String absolutePath = file.getAbsolutePath();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getAbsolutePath().equals(absolutePath)) {
                return;
            }
        }
        list.add(file);
    }

    static List<File> scan() {
        ArrayList arrayList = new ArrayList();
        String[] strArr = {Core.SD, Core.SD + "/Download", Core.SD + "/Documents", Core.SD + "/Games", Core.SD + "/PC", Core.SD + "/游戏"};
        for (int i = 0; i < 6; i++) {
            File file = new File(strArr[i]);
            if (file.isDirectory()) {
                if (hasExe(file)) {
                    add(arrayList, file);
                }
                File[] fileArrListFiles = file.listFiles();
                if (fileArrListFiles != null) {
                    for (int i2 = 0; i2 < fileArrListFiles.length; i2++) {
                        if (fileArrListFiles[i2].isDirectory() && hasExe(fileArrListFiles[i2])) {
                            add(arrayList, fileArrListFiles[i2]);
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    static int activeIndex(List<File> list) {
        File fileGameDir = Core.gameDir();
        if (fileGameDir == null) {
            return -1;
        }
        String absolutePath = fileGameDir.getAbsolutePath();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getAbsolutePath().equals(absolutePath)) {
                return i;
            }
        }
        return -1;
    }

    static void manage(final Activity activity) {
        final List<File> listScan = scan();
        int iActiveIndex = activeIndex(listScan);
        String[] strArr = new String[listScan.size() + 2];
        int i = 0;
        while (i < listScan.size()) {
            StringBuilder sb = new StringBuilder();
            sb.append(listScan.get(i).getName());
            sb.append(i == iActiveIndex ? "（当前）" : "");
            strArr[i] = sb.toString();
            i++;
        }
        strArr[listScan.size()] = "＋ 选择游戏目录…";
        strArr[listScan.size() + 1] = "＋ 安装新游戏…";
        Skin.modal(activity, "游戏实例", strArr, -1, new Skin.Picker() {
            @Override
            public void pick(int i2) {
                if (i2 != listScan.size()) {
                    if (i2 != listScan.size() + 1) {
                        if (i2 < 0 || i2 >= listScan.size()) {
                            return;
                        }
                        Instances.switchTo(activity, (File) listScan.get(i2));
                        return;
                    }
                    Instances.pickArchive(activity);
                    return;
                }
                Instances.pickDir(activity);
            }
        });
    }

    static void switchTo(final Activity activity, final File file) {
        new AlertDialog.Builder(activity).setTitle("切换实例").setMessage("把当前游戏目录切换到：\n" + file.getAbsolutePath()).setPositiveButton("切换", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Core.setCustomDir(activity, file.getAbsolutePath());
                activity.recreate();
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    static void pickDir(Activity activity) {
        try {
            Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
            intent.addFlags(1);
            activity.startActivityForResult(intent, REQ_DIR);
        } catch (Throwable th) {
            toast(activity, "打不开目录选择器：" + th);
        }
    }

    static String treePath(Uri uri) {
        String lastPathSegment;
        String str;
        try {
            lastPathSegment = DocumentsContract.getTreeDocumentId(uri);
        } catch (Throwable th) {
            lastPathSegment = null;
        }
        if (lastPathSegment == null) {
            lastPathSegment = uri.getLastPathSegment();
        }
        if (lastPathSegment == null) {
            return null;
        }
        int iIndexOf = lastPathSegment.indexOf(58);
        String strSubstring = iIndexOf >= 0 ? lastPathSegment.substring(0, iIndexOf) : "primary";
        String strSubstring2 = iIndexOf >= 0 ? lastPathSegment.substring(iIndexOf + 1) : "";
        if ("primary".equals(strSubstring)) {
            str = Core.SD;
        } else {
            str = "/storage/" + strSubstring;
        }
        if (strSubstring2.length() == 0) {
            return str;
        }
        return str + "/" + strSubstring2;
    }

    static void pickArchive(Activity activity) {
        try {
            Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
            intent.addCategory("android.intent.category.OPENABLE");
            intent.setType("*/*");
            activity.startActivityForResult(intent, REQ);
        } catch (Throwable th) {
            toast(activity, "打不开文件选择器：" + th);
        }
    }

    public static void onResult(final Activity activity, int i, int i2, Intent intent) {
        if (i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        final Uri data = intent.getData();
        if (i == 9202) {
            String strTreePath = treePath(data);
            if (strTreePath == null) {
                toast(activity, "无法识别该目录");
                return;
            }
            File fileFindGame = findGame(new File(strTreePath));
            if (fileFindGame == null) {
                toast(activity, "这个目录里没有找到 CasualtiesUnknown.exe");
                return;
            }
            Core.setCustomDir(activity, fileFindGame.getAbsolutePath());
            toast(activity, "已选择：" + fileFindGame.getAbsolutePath());
            activity.recreate();
            return;
        }
        String strDisplayName = displayName(activity, data);
        if (strDisplayName == null || strDisplayName.length() == 0) {
            strDisplayName = "game.zip";
        }
        final String strSanitize = sanitize(strDisplayName);
        toast(activity, "正在复制压缩包…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File file = new File(activity.getCacheDir(), "import-" + System.currentTimeMillis() + ".bin");
                try {
                    InputStream inputStreamOpenInputStream = activity.getContentResolver().openInputStream(data);
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    byte[] bArr = new byte[65536];
                    while (true) {
                        int i3 = inputStreamOpenInputStream.read(bArr);
                        if (i3 <= 0) {
                            fileOutputStream.close();
                            inputStreamOpenInputStream.close();
                            final File fileUniqueDir = Instances.uniqueDir(new File("/storage/emulated/0/Download"), Instances.stripExt(strSanitize));
                            fileUniqueDir.mkdirs();
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Instances.extract(activity, file, fileUniqueDir, strSanitize);
                                }
                            });
                            return;
                        }
                        fileOutputStream.write(bArr, 0, i3);
                    }
                } catch (Throwable th) {
                    Log.e("Instances", "import failed", th);
                    file.delete();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Instances.toast(activity, "导入失败：" + th);
                        }
                    });
                }
            }
        }).start();
    }

    static void extract(Activity activity, File file, File file2, String str) {
        Log.i("Instances", "extract start " + file.getAbsolutePath() + " -> " + file2.getAbsolutePath() + " (" + file.length() + " bytes)");
        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        TextView textView = new TextView(activity);
        textView.setText("0%");
        textView.setTextColor(-1);
        textView.setTextSize(14.0f);
        TextView textView2 = new TextView(activity);
        textView2.setText(str);
        textView2.setTextColor(-1711276033);
        textView2.setTextSize(12.0f);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(1);
        int iDp = dp(activity, 20);
        linearLayout.setPadding(iDp, dp(activity, 10), iDp, dp(activity, 6));
        try {
            progressBar.setProgressTintList(ColorStateList.valueOf(Ui.ACCENT));
        } catch (Throwable th) {
        }
        linearLayout.addView(textView);
        linearLayout.addView(progressBar);
        linearLayout.addView(textView2);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(activity).setTitle("正在解压 " + str).setView(linearLayout).setCancelable(false).create();
        alertDialogCreate.show();
        try {
            alertDialogCreate.getWindow().setBackgroundDrawable(new ColorDrawable(-233169628));
        } catch (Throwable th2) {
        }
        new Thread(new AnonymousClass5(file, file2, activity, progressBar, textView, textView2, alertDialogCreate)).start();
    }

    
    static class AnonymousClass5 implements Runnable {
        final /* synthetic */ Activity val$a;
        final /* synthetic */ File val$dest;
        final /* synthetic */ AlertDialog val$dlg;
        final /* synthetic */ ProgressBar val$pb;
        final /* synthetic */ File val$src;
        final /* synthetic */ TextView val$sub;
        final /* synthetic */ TextView val$tv;

        AnonymousClass5(File file, File file2, Activity activity, ProgressBar progressBar, TextView textView, TextView textView2, AlertDialog alertDialog) {
            this.val$src = file;
            this.val$dest = file2;
            this.val$a = activity;
            this.val$pb = progressBar;
            this.val$tv = textView;
            this.val$sub = textView2;
            this.val$dlg = alertDialog;
        }

        @Override
        public void run() {
            try {
                Packer.extract(this.val$src, this.val$dest, new Packer.Progress() {
                    @Override
                    public void on(final int i, final String str) {
                        AnonymousClass5.this.val$a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AnonymousClass5.this.val$pb.setProgress(i);
                                AnonymousClass5.this.val$tv.setText(i + "%");
                                AnonymousClass5.this.val$sub.setText(str);
                            }
                        });
                    }
                });
                Log.i("Instances", "extract done, scanning " + this.val$dest.getAbsolutePath());
                final File fileFindGame = Instances.findGame(this.val$dest);
                if (fileFindGame == null) {
                    throw new IOException("压缩包里没找到 CasualtiesUnknown.exe");
                }
                Core.setCustomDir(this.val$a, fileFindGame.getAbsolutePath());
                this.val$src.delete();
                this.val$a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AnonymousClass5.this.val$dlg.dismiss();
                        Instances.toast(AnonymousClass5.this.val$a, "已安装：" + fileFindGame.getAbsolutePath());
                        AnonymousClass5.this.val$a.recreate();
                    }
                });
            } catch (Throwable th) {
                Log.e("Instances", "extract failed", th);
                this.val$a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AnonymousClass5.this.val$dlg.dismiss();
                        Instances.toast(AnonymousClass5.this.val$a, "解压失败：" + th.getMessage());
                    }
                });
            }
        }
    }

    static File findGame(File file) {
        if (hasExe(file)) {
            return file;
        }
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (int i = 0; i < fileArrListFiles.length; i++) {
                if (fileArrListFiles[i].isDirectory() && hasExe(fileArrListFiles[i])) {
                    return fileArrListFiles[i];
                }
            }
            return null;
        }
        return null;
    }

    static File uniqueDir(File file, String str) {
        File file2 = new File(file, str);
        if (!file2.exists()) {
            return file2;
        }
        for (int i = 2; i < 1000; i++) {
            File file3 = new File(file, str + "-" + i);
            if (!file3.exists()) {
                return file3;
            }
        }
        return new File(file, str + "-" + System.currentTimeMillis());
    }

    static String stripExt(String str) {
        String lowerCase = str.toLowerCase();
        String[] strArr = {".tar.gz", ".tgz", ".zip", ".rar", ".7z"};
        for (int i = 0; i < 5; i++) {
            if (lowerCase.endsWith(strArr[i])) {
                return str.substring(0, str.length() - strArr[i].length());
            }
        }
        return str;
    }

    static String sanitize(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '/' || cCharAt == '\\' || cCharAt == ':' || cCharAt == '*' || cCharAt == '?' || cCharAt == '\"' || cCharAt == '<' || cCharAt == '>' || cCharAt == '|') {
                sb.append('_');
            } else {
                sb.append(cCharAt);
            }
        }
        String strTrim = sb.toString().trim();
        return strTrim.length() == 0 ? "instance" : strTrim;
    }

    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    static java.lang.String displayName(android.app.Activity r7, android.net.Uri r8) {
        /*
            r0 = 0
            android.content.ContentResolver r1 = r7.getContentResolver()     // Catch: java.lang.Throwable -> L2c
            java.lang.String r7 = "_display_name"
            java.lang.String[] r3 = new java.lang.String[]{r7}     // Catch: java.lang.Throwable -> L2c
            r4 = 0
            r5 = 0
            r6 = 0
            r2 = r8
            android.database.Cursor r7 = r1.query(r2, r3, r4, r5, r6)     // Catch: java.lang.Throwable -> L2c
            if (r7 == 0) goto L29
            boolean r1 = r7.moveToFirst()     // Catch: java.lang.Throwable -> L27
            if (r1 == 0) goto L29
            r1 = 0
            java.lang.String r8 = r7.getString(r1)     // Catch: java.lang.Throwable -> L27
            if (r7 == 0) goto L26
            r7.close()
        L26:
            return r8
        L27:
            r1 = move-exception
            goto L2e
        L29:
            if (r7 == 0) goto L33
            goto L30
        L2c:
            r7 = move-exception
            r7 = r0
        L2e:
            if (r7 == 0) goto L33
        L30:
            r7.close()
        L33:
            java.lang.String r7 = r8.getPath()
            if (r7 != 0) goto L3a
            goto L43
        L3a:
            java.io.File r8 = new java.io.File
            r8.<init>(r7)
            java.lang.String r0 = r8.getName()
        L43:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.rblh.cu.Instances.displayName(android.app.Activity, android.net.Uri):java.lang.String");
    }

    static int dp(Activity activity, int i) {
        return (int) ((i * activity.getResources().getDisplayMetrics().density) + 0.5f);
    }

    static void toast(Activity activity, String str) {
        Toast.makeText(activity, str, 1).show();
    }
}
