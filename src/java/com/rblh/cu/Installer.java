package com.rblh.cu;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/* loaded from: classes.dex */
public class Installer {

    public interface Progress {
        void onProgress(long j, String str);
    }

    public static String detectSingleTopDir(InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream, 65536));
        String str = null;
        int i = 0;
        while (true) {
            try {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry != null) {
                    String name = nextEntry.getName();
                    if (name.startsWith("__MACOSX")) {
                        zipInputStream.closeEntry();
                    } else {
                        int iIndexOf = name.indexOf(47);
                        if (iIndexOf < 0) {
                            str = null;
                            break;
                        }
                        String strSubstring = name.substring(0, iIndexOf + 1);
                        if (str != null) {
                            if (!str.equals(strSubstring)) {
                                str = null;
                                break;
                            }
                        } else {
                            str = strSubstring;
                        }
                        i++;
                        zipInputStream.closeEntry();
                    }
                }
            } finally {
                try {
                    zipInputStream.close();
                } catch (Throwable th) {
                }
            }
        }
        if (i > 0) {
            return str;
        }
        return null;
    }

    public static long unzip(InputStream inputStream, File file, String str, Progress progress) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream, 65536));
        byte[] bArr = new byte[65536];
        long j = 0;
        while (true) {
            try {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry != null) {
                    String name = nextEntry.getName();
                    if (name.startsWith("__MACOSX")) {
                        zipInputStream.closeEntry();
                    } else {
                        if (str != null && name.startsWith(str)) {
                            name = name.substring(str.length());
                            if (name.length() == 0) {
                                zipInputStream.closeEntry();
                            }
                        }
                        File file2 = new File(file, name);
                        if (name.endsWith("/")) {
                            file2.mkdirs();
                            zipInputStream.closeEntry();
                        } else {
                            File parentFile = file2.getParentFile();
                            if (parentFile != null && !parentFile.exists()) {
                                parentFile.mkdirs();
                            }
                            FileOutputStream fileOutputStream = new FileOutputStream(file2);
                            while (true) {
                                int i = zipInputStream.read(bArr);
                                if (i <= 0) {
                                    break;
                                }
                                fileOutputStream.write(bArr, 0, i);
                                j += i;
                            }
                            fileOutputStream.close();
                            zipInputStream.closeEntry();
                            if (progress != null) {
                                progress.onProgress(j, name);
                            }
                        }
                    }
                } else {
                    try {
                        break;
                    } catch (Throwable th) {
                    }
                }
            } finally {
                try {
                    zipInputStream.close();
                } catch (Throwable th2) {
                }
            }
        }
        return j;
    }

    public static long unzip(InputStream inputStream, File file, Progress progress) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream, 65536));
        byte[] buf = new byte[65536];
        long total = 0;
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("__MACOSX")) {
                    zis.closeEntry();
                    continue;
                }
                File out = new File(file, name);
                if (name.endsWith("/")) {
                    out.mkdirs();
                    zis.closeEntry();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(out);
                try {
                    int n;
                    while ((n = zis.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                        total += n;
                    }
                } finally {
                    fos.close();
                }
                zis.closeEntry();
                if (progress != null) {
                    progress.onProgress(total, name);
                }
            }
        } finally {
            try {
                zis.close();
            } catch (Throwable th) {
            }
        }
        return total;
    }

    public static long installGame(InputStream inputStream, File file, Progress progress) throws IOException {
        return unzip(inputStream, file, progress);
    }

    public static long installBepInEx(InputStream inputStream, Progress progress) throws IOException {
        File fileGameDir = Core.gameDir();
        if (!Core.isRootPath()) {
            fileGameDir.mkdirs();
            long n = unzip(inputStream, fileGameDir, progress);
            writeMarker();
            return n;
        }
        File tmp = new File(System.getProperty("java.io.tmpdir", "/data/local/tmp"), "bep_" + System.currentTimeMillis());
        tmp.mkdirs();
        long total = unzip(inputStream, tmp, progress);
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("SRC=").append(Root.quote(tmp.getAbsolutePath())).append("; ");
            sb.append("DST=").append(Root.quote(fileGameDir.getAbsolutePath())).append("; ");
            sb.append("OWN=$(stat -c %u \"$DST\"); ");
            sb.append("cp -rf \"$SRC\"/. \"$DST\"/ || exit 2; ");
            sb.append("chown -R \"$OWN:$OWN\" \"$DST/BepInEx\" 2>/dev/null; ");
            sb.append("chmod -R u+rwX \"$DST/BepInEx\" 2>/dev/null; ");
            sb.append("chcon -R --reference=\"$DST\" \"$DST/BepInEx\" 2>/dev/null; ");
            sb.append("for f in .doorstop_version doorstop_config.ini winhttp.dll changelog.txt; do ");
            sb.append("if [ -e \"$DST/$f\" ]; then chown \"$OWN:$OWN\" \"$DST/$f\" 2>/dev/null; chmod u+rw \"$DST/$f\" 2>/dev/null; chcon --reference=\"$DST\" \"$DST/$f\" 2>/dev/null; fi; done; ");
            sb.append("echo DONE");
            String out = Root.execNs(sb.toString());
            if (out == null || !out.contains("DONE")) {
                throw new IOException("写入游戏目录失败：\n" + (out == null ? "" : out));
            }
        } catch (IOException e) {
            throw e;
        } catch (Throwable th) {
            throw new IOException("写入游戏目录失败：" + th);
        } finally {
            deleteTree(tmp);
        }
        Core.invalidate();
        writeMarker();
        return total;
    }

    private static void writeMarker() {
        try {
            File marker = new File(Core.bepinexDir(), ".cu_version");
            String content = Core.BEP_VERSION + "\n";
            if (Core.isRootPath()) {
                Root.execNs("printf '%s' " + Root.quote(content) + " > " + Root.quote(marker.getAbsolutePath()));
            } else {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(marker);
                fos.write(content.getBytes("UTF-8"));
                fos.close();
            }
            Core.invalidate();
        } catch (Throwable th) {
        }
    }

    private static void deleteTree(File f) {
        File[] list = f.listFiles();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) {
                    deleteTree(list[i]);
                } else {
                    list[i].delete();
                }
            }
        }
        f.delete();
    }

    public static void installApk(Context context, File file) throws Throwable {
        Throwable th;
        FileInputStream fileInputStream;
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        int iCreateSession = packageInstaller.createSession(new PackageInstaller.SessionParams(1));
        PackageInstaller.Session sessionOpenSession = packageInstaller.openSession(iCreateSession);
        OutputStream outputStream = null;
        try {
            long length = file.length();
            sessionOpenSession.setStagingProgress(0.0f);
            OutputStream outputStreamOpenWrite = sessionOpenSession.openWrite("base.apk", 0L, length);
            try {
                fileInputStream = new FileInputStream(file);
                try {
                    byte[] bArr = new byte[65536];
                    long j = 0;
                    while (true) {
                        int i = fileInputStream.read(bArr);
                        if (i <= 0) {
                            break;
                        }
                        outputStreamOpenWrite.write(bArr, 0, i);
                        j += i;
                        sessionOpenSession.setStagingProgress(length > 0 ? j / length : 0.0f);
                    }
                    sessionOpenSession.fsync(outputStreamOpenWrite);
                    if (outputStreamOpenWrite != null) {
                        try {
                            outputStreamOpenWrite.close();
                        } catch (Throwable th2) {
                        }
                    }
                    try {
                        fileInputStream.close();
                    } catch (Throwable th3) {
                    }
                    Intent intent = new Intent(context, (Class<?>) InstallResultReceiver.class);
                    intent.setAction(InstallResultReceiver.ACTION);
                    sessionOpenSession.commit(PendingIntent.getBroadcast(context, iCreateSession, intent, Build.VERSION.SDK_INT >= 31 ? 167772160 : 134217728).getIntentSender());
                    sessionOpenSession.close();
                } catch (Throwable th4) {
                    th = th4;
                    outputStream = outputStreamOpenWrite;
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Throwable th5) {
                        }
                    }
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                            throw th;
                        } catch (Throwable th6) {
                            throw th;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
                fileInputStream = null;
            }
        } catch (Throwable th8) {
            th = th8;
            fileInputStream = null;
        }
    }
}
