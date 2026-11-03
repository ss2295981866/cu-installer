package com.rblh.cu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/* loaded from: classes.dex */
public class Root {
    public static final String GAMESIR_GAME_DIR = "/data/user/0/com.xiaoji.egggame/files/Steam/steamapps/common/Casualties Unknown Demo";
    private static Boolean cached = null;
    public static String lastOut = "";

    public static boolean available() {
        if (cached != null) {
            return cached.booleanValue();
        }
        try {
            boolean z = true;
            Process processExec = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            String str = read(processExec.getInputStream());
            processExec.waitFor();
            if (str == null || !str.contains("uid=0")) {
                z = false;
            }
            cached = Boolean.valueOf(z);
        } catch (Throwable th) {
            cached = false;
        }
        return cached.booleanValue();
    }

    public static String exec(String str) {
        try {
            Process processExec = Runtime.getRuntime().exec(new String[]{"su", "-c", str});
            String str2 = read(processExec.getInputStream());
            String str3 = read(processExec.getErrorStream());
            processExec.waitFor();
            lastOut = "cmd=" + str + " | out=" + str2 + " | err=" + str3;
            return str2 == null ? "" : str2;
        } catch (Throwable th) {
            lastOut = "cmd=" + str + " | exception=" + th;
            return "";
        }
    }

    public static String execNs(String str) {
        return exec("nsenter -t 1 -m -- sh -c " + shellQuote(str));
    }

    private static String shellQuote(String str) {
        return "'" + str.replace("'", "'\\''") + "'";
    }

    private static String read(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bArr = new byte[4096];
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
            inputStream.close();
            return byteArrayOutputStream.toString("UTF-8");
        } catch (Throwable th2) {
            return "";
        }
    }

    public static boolean exists(String str) {
        return execNs("test -e " + shellQuote(str) + " && echo YES || echo NO").contains("YES");
    }

    public static String[] list(String str) {
        String strExecNs = execNs("ls " + shellQuote(str) + " 2>/dev/null");
        if (strExecNs == null || strExecNs.trim().length() == 0) {
            return new String[0];
        }
        return strExecNs.trim().split("\n");
    }

    public static boolean rename(String str, String str2) {
        return execNs("mv -f " + shellQuote(str) + " " + shellQuote(str2) + "; echo DONE").contains("DONE");
    }

    public static boolean remove(String str) {
        return execNs("rm -rf " + shellQuote(str) + "; echo DONE").contains("DONE");
    }

    public static boolean copyFile(File file, String str) {
        return execNs("mkdir -p $(dirname " + shellQuote(str) + ") && cp -f " + shellQuote(file.getAbsolutePath()) + " " + shellQuote(str) + " && chmod 644 " + shellQuote(str) + " && echo DONE").contains("DONE");
    }

    public static boolean copyDir(File file, String str) {
        String str2 = "/data/local/tmp/cui_dir_" + System.currentTimeMillis();
        return execNs("mkdir -p " + shellQuote(str2) + " && cd " + shellQuote(file.getParent()) + " && tar -cf " + shellQuote(str2 + "/a.tar") + " " + shellQuote(file.getName()) + " 2>/dev/null && mkdir -p " + shellQuote(str) + " && cd " + shellQuote(str) + " && tar -xf " + shellQuote(str2 + "/a.tar") + " 2>/dev/null; rm -rf " + shellQuote(str2) + "; echo DONE").contains("DONE");
    }

    public static boolean copyDirContents(File file, String str) {
        String str2 = "/data/local/tmp/cui_c_" + System.currentTimeMillis();
        return execNs("mkdir -p " + shellQuote(str2) + " && tar -cf " + shellQuote(str2 + "/a.tar") + " -C " + shellQuote(file.getAbsolutePath()) + " . 2>/dev/null && mkdir -p " + shellQuote(str) + " && cd " + shellQuote(str) + " && tar -xf " + shellQuote(str2 + "/a.tar") + " 2>/dev/null; rm -rf " + shellQuote(str2) + "; echo DONE").contains("DONE");
    }

    /* JADX WARN: Finally extract failed */
    private static void copyTo(File file, String str) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        FileOutputStream fileOutputStream = new FileOutputStream(str);
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
            File file2 = new File(str);
            file2.setReadable(true, false);
            file2.setExecutable(false, false);
        } catch (Throwable th2) {
            try {
                fileInputStream.close();
            } catch (Throwable th3) {
            }
            fileOutputStream.close();
            throw th2;
        }
    }
}
