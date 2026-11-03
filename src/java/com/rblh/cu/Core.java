package com.rblh.cu;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* loaded from: classes.dex */
public class Core {
    public static final String PKG_GAME_DIR = "Casualties Unknown_7.0.1Demo";
    public static final String SD = "/storage/emulated/0";
    public static volatile boolean gamesirReady = false;
    public static volatile String gamesirDir = null;
    public static volatile int gamesirSaves = 0;
    private static String customGameDir = null;
    public static final String[] GAME_ZIP_NAMES = {"Casualties Unknown_7.0.1Demo.zip", "game.zip", "游戏.zip"};
    public static final String[] BEPINEX_ZIP_NAMES = {"BepInEx.zip", "bepinex.zip"};

    public static class Mod {
        public boolean enabled;
        public boolean isDir;
        public String name;
        public File path;
    }

    public static void loadCustomDir(Context context) {
        try {
            customGameDir = prefs(context).getString("game_dir", null);
        } catch (Throwable th) {
        }
    }

    public static String defaultGameDirPath() {
        return new File(new File(SD, "Download"), PKG_GAME_DIR).getAbsolutePath();
    }

    public static String customGameDirRaw() {
        return customGameDir;
    }

    public static void setCustomDir(Context context, String str) {
        if (str == null || str.trim().length() == 0) {
            customGameDir = null;
            prefs(context).edit().remove("game_dir").apply();
        } else {
            customGameDir = str.trim();
            prefs(context).edit().putString("game_dir", customGameDir).apply();
        }
    }

    public static File gameDir() {
        if (customGameDir != null && customGameDir.trim().length() > 0) {
            return new File(customGameDir.trim());
        }
        return Locate.defaultDir();
    }

    public static File downloadDir() {
        return new File(SD, "Download");
    }

    public static File bepinexDir() {
        return new File(gameDir(), "BepInEx");
    }

    public static File pluginsDir() {
        return new File(bepinexDir(), "plugins");
    }

    public static File langDir() {
        return new File(gameDir(), "CasualtiesUnknown_Data/Lang");
    }

    public static File slotsDir() {
        return new File(pluginsDir(), "slots");
    }

    public static File backupDir() {
        File file = new File(SD, "Download/CU存档备份");
        file.mkdirs();
        return file;
    }

    public static List<File> listBackups() {
        ArrayList arrayList = new ArrayList();
        File[] fileArrListFiles = backupDir().listFiles();
        if (fileArrListFiles == null) {
            return arrayList;
        }
        for (File file : fileArrListFiles) {
            String name = file.getName();
            if (name.endsWith(".tar.gz") || name.endsWith(".zip")) {
                arrayList.add(file);
            }
        }
        Collections.sort(arrayList, new Comparator<File>() {
            @Override
            public int compare(File file2, File file3) {
                return file3.getName().compareTo(file2.getName());
            }
        });
        return arrayList;
    }

    public static int cleanupDuplicates() {
        File[] fileArrListFiles = backupDir().listFiles();
        if (fileArrListFiles == null) {
            return 0;
        }
        int i = 0;
        for (File file : fileArrListFiles) {
            String name = file.getName();
            if (name.matches(".*\\(\\d+\\).*")) {
                File file2 = new File(file.getParentFile(), name.replaceAll("\\(\\d+\\)", ""));
                if (file2.exists() && file2.length() == file.length()) {
                    noteFile(file).delete();
                    if (file.delete()) {
                        i++;
                    }
                }
            }
        }
        return i;
    }

    public static String noteFromName(String str) {
        if (str.endsWith(".tar.gz")) {
            str = str.substring(0, str.length() - 7);
        } else if (str.endsWith(".zip")) {
            str = str.substring(0, str.length() - 4);
        }
        if (str.length() > 16 && str.charAt(15) == '_') {
            return str.substring(16).replace('_', ' ').trim();
        }
        return "";
    }

    public static File noteFile(File file) {
        String name = file.getName();
        if (name.endsWith(".tar.gz")) {
            name = name.substring(0, name.length() - 7);
        } else if (name.endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        return new File(file.getParentFile(), name + ".note");
    }

    public static int backupLimit(Context context) {
        int i = prefs(context).getInt("backup_limit", 10);
        if (i < 1) {
            i = 1;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }

    public static void setBackupLimit(Context context, int i) {
        if (i < 1) {
            i = 1;
        }
        if (i > 255) {
            i = 255;
        }
        prefs(context).edit().putInt("backup_limit", i).apply();
    }

    public static int pruneBackups(Context context) {
        List<File> listListBackups = listBackups();
        int i = 0;
        for (int iBackupLimit = backupLimit(context); iBackupLimit < listListBackups.size(); iBackupLimit++) {
            File file = listListBackups.get(iBackupLimit);
            noteFile(file).delete();
            if (file.delete()) {
                i++;
            }
        }
        return i;
    }

    public static String noteOf(File file) {
        File fileNoteFile = noteFile(file);
        if (!fileNoteFile.exists()) {
            return "";
        }
        try {
            byte[] bArr = new byte[(int) fileNoteFile.length()];
            FileInputStream fileInputStream = new FileInputStream(fileNoteFile);
            fileInputStream.read(bArr);
            fileInputStream.close();
            return new String(bArr, "UTF-8").trim();
        } catch (Throwable th) {
            return "";
        }
    }

    public static void setNote(File file, String str) {
        try {
            File fileNoteFile = noteFile(file);
            if (str != null && str.trim().length() != 0) {
                String strTrim = str.trim();
                if (strTrim.length() > 40) {
                    strTrim = strTrim.substring(0, 40);
                }
                FileOutputStream fileOutputStream = new FileOutputStream(fileNoteFile);
                fileOutputStream.write(strTrim.trim().getBytes("UTF-8"));
                fileOutputStream.close();
                return;
            }
            fileNoteFile.delete();
        } catch (Throwable th) {
        }
    }

    public static String findSaveSv() {
        String[] strArr = {"/data/user/0/com.xiaoji.egggame/files/usr/home/virtual_containers/*/drive_c/users/*/AppData/LocalLow/Orsoniks/CasualtiesUnknown/save.sv", "/data/user/0/com.andromeda.androbench2/files/imagefs/home/*/.wine/drive_c/users/*/AppData/LocalLow/Orsoniks/CasualtiesUnknown/save.sv", "/data/user/0/com.winlator/files/imagefs/home/*/.wine/drive_c/users/*/AppData/LocalLow/Orsoniks/CasualtiesUnknown/save.sv", "/sdcard/Download/Winlator/*/drive_c/users/*/AppData/LocalLow/Orsoniks/CasualtiesUnknown/save.sv"};
        for (int i = 0; i < 4; i++) {
            String strExecNs = Root.execNs("ls -1 " + strArr[i] + " 2>/dev/null | head -1");
            if (strExecNs != null && strExecNs.trim().length() > 0) {
                return strExecNs.trim();
            }
        }
        return null;
    }

    public static File findSlotsDir() {
        File fileGameDir = gameDir();
        File file = new File(fileGameDir, "BepInEx/plugins/slots");
        return exists(file) ? file : findDirNamed(fileGameDir, "slots", 0);
    }

    private static File findDirNamed(File file, String str, int i) {
        if (i > 5) {
            return null;
        }
        String[] strArrLs = ls(file);
        for (String str2 : strArrLs) {
            if (str2.equalsIgnoreCase(str)) {
                File file2 = new File(file, str2);
                if (exists(file2)) {
                    return file2;
                }
            }
        }
        for (String str3 : strArrLs) {
            File fileFindDirNamed = findDirNamed(new File(file, str3), str, i + 1);
            if (fileFindDirNamed != null) {
                return fileFindDirNamed;
            }
        }
        return null;
    }

    public static int saveCount() {
        File fileFindSlotsDir = findSlotsDir();
        if (fileFindSlotsDir == null) {
            return 0;
        }
        if (isRootPath()) {
            try {
                return Integer.parseInt(Root.execNs("find " + q(fileFindSlotsDir.getAbsolutePath()) + " -name \"*.sv\" 2>/dev/null | wc -l").trim());
            } catch (Throwable th) {
                return 0;
            }
        }
        return countSv(fileFindSlotsDir);
    }

    private static int countSv(File file) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return 0;
        }
        int iCountSv = 0;
        for (File file2 : fileArrListFiles) {
            if (file2.isDirectory()) {
                iCountSv += countSv(file2);
            } else if (file2.getName().endsWith(".sv")) {
                iCountSv++;
            }
        }
        return iCountSv;
    }

    public static boolean saveDirExists() {
        return findSlotsDir() != null;
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("cu_installer", 0);
    }

    public static boolean hasGame() {
        return exists(new File(gameDir(), "CasualtiesUnknown.exe"));
    }

    public static boolean hasBepInEx() {
        return exists(new File(bepinexDir(), "core/BepInEx.dll"));
    }

    public static boolean hasSaveMod() {
        return containsSaveMod(pluginsDir(), 0);
    }

    private static boolean containsSaveMod(File file, int i) {
        if (i > 3) {
            return false;
        }
        for (String str : ls(file)) {
            String lowerCase = str.toLowerCase();
            if (lowerCase.contains("savemanager") || lowerCase.contains("savemgr") || lowerCase.contains("存档管理") || containsSaveMod(new File(file, str), i + 1)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSave() {
        File fileSlotsDir = slotsDir();
        return fileSlotsDir.exists() && fileSlotsDir.listFiles() != null && fileSlotsDir.listFiles().length > 0;
    }

    public static boolean isRootPath() {
        return gameDir().getAbsolutePath().startsWith("/data/");
    }

    public static String[] ls(File file) {
        if (isRootPath()) {
            return Root.list(file.getAbsolutePath());
        }
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return new String[0];
        }
        String[] strArr = new String[fileArrListFiles.length];
        for (int i = 0; i < fileArrListFiles.length; i++) {
            strArr[i] = fileArrListFiles[i].getName();
        }
        return strArr;
    }

    public static boolean exists(File file) {
        return isRootPath() ? Root.exists(file.getAbsolutePath()) : file.exists();
    }

    public static long size(File file) {
        if (isRootPath()) {
            try {
                String[] strArrSplit = Root.execNs("/system/bin/du -sb " + q(file.getAbsolutePath()) + " 2>/dev/null").trim().split("\\s+");
                if (strArrSplit.length > 0 && strArrSplit[0].length() > 0) {
                    return Long.parseLong(strArrSplit[0].trim());
                }
                return 0L;
            } catch (Throwable th) {
                return 0L;
            }
        }
        return dirSize(file);
    }

    private static String q(String str) {
        return "'" + str.replace("'", "'\\''") + "'";
    }

    public static boolean mv(File file, File file2) {
        if (isRootPath()) {
            return Root.rename(file.getAbsolutePath(), file2.getAbsolutePath());
        }
        return file.renameTo(file2);
    }

    public static long dirSize(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }
        long[] jArr = {0};
        walk(file, jArr);
        return jArr[0];
    }

    private static void walk(File file, long[] jArr) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file2 : fileArrListFiles) {
            if (file2.isDirectory()) {
                walk(file2, jArr);
            } else {
                jArr[0] = jArr[0] + file2.length();
            }
        }
    }

    public static String human(long j) {
        return j < 1024 ? j + " B" : j < 1048576 ? String.format("%.1f KB", Double.valueOf(j / 1024.0d)) : j < 1073741824 ? String.format("%.1f MB", Double.valueOf(j / 1048576.0d)) : String.format("%.2f GB", Double.valueOf(j / 1.073741824E9d));
    }

    public static List<Mod> scanMods() {
        ArrayList arrayList = new ArrayList();
        File filePluginsDir = pluginsDir();
        for (String str : ls(filePluginsDir)) {
            File file = new File(filePluginsDir, str);
            if (!str.startsWith(".")) {
                Mod mod = new Mod();
                mod.isDir = file.isDirectory();
                if (str.endsWith(".dll.disabled")) {
                    mod.enabled = false;
                    mod.name = str.substring(0, str.length() - ".dll.disabled".length());
                } else if (str.endsWith(".disabled")) {
                    mod.enabled = false;
                    mod.name = str.substring(0, str.length() - ".disabled".length());
                } else {
                    boolean z = true;
                    if (str.endsWith(".dll")) {
                        mod.enabled = true;
                        mod.name = str.substring(0, str.length() - 4);
                    } else if (file.isDirectory() || (isRootPath() && str.indexOf(".") < 0)) {
                        mod.isDir = true;
                        if (!isRootPath() && !hasEnabledDll(file) && hasAnyDll(file)) {
                            z = false;
                        }
                        mod.enabled = z;
                        mod.name = str;
                    }
                }
                mod.path = file;
                arrayList.add(mod);
            }
        }
        Collections.sort(arrayList, new Comparator<Mod>() {
            @Override
            public int compare(Mod mod2, Mod mod3) {
                return mod2.name.compareToIgnoreCase(mod3.name);
            }
        });
        return arrayList;
    }

    public static boolean hasEnabledDll(File file) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return false;
        }
        for (File file2 : fileArrListFiles) {
            String lowerCase = file2.getName().toLowerCase();
            if (file2.isDirectory()) {
                if (hasEnabledDll(file2)) {
                    return true;
                }
            } else if (lowerCase.endsWith(".dll")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAnyDll(File file) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return false;
        }
        for (File file2 : fileArrListFiles) {
            String lowerCase = file2.getName().toLowerCase();
            if (file2.isDirectory()) {
                if (hasAnyDll(file2)) {
                    return true;
                }
            } else if (lowerCase.endsWith(".dll") || lowerCase.endsWith(".dll.disabled")) {
                return true;
            }
        }
        return false;
    }

    public static boolean setDirDlls(File file, boolean z) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return false;
        }
        boolean z2 = true;
        for (File file2 : fileArrListFiles) {
            String name = file2.getName();
            String lowerCase = name.toLowerCase();
            if (file2.isDirectory()) {
                z2 = setDirDlls(file2, z) && z2;
            } else if (z && lowerCase.endsWith(".dll.disabled")) {
                z2 = file2.renameTo(new File(file2.getParentFile(), name.substring(0, name.length() + (-9)))) && z2;
            } else if (!z && lowerCase.endsWith(".dll")) {
                z2 = file2.renameTo(new File(file2.getParentFile(), new StringBuilder().append(name).append(".disabled").toString())) && z2;
            }
        }
        return z2;
    }

    public static boolean toggleMod(Mod mod) {
        try {
            boolean z = !mod.enabled;
            if (mod.isDir) {
                if (isRootPath()) {
                    String absolutePath = z ? mod.path.getAbsolutePath() : mod.path.getAbsolutePath() + ".disabled";
                    if (!mv(mod.path, new File(absolutePath))) {
                        return false;
                    }
                    mod.path = new File(absolutePath);
                    mod.enabled = z;
                    return true;
                }
                if (hasAnyDll(mod.path)) {
                    if (!setDirDlls(mod.path, z)) {
                        return false;
                    }
                    mod.enabled = z;
                    return true;
                }
                File file = new File(mod.path.getParentFile(), z ? mod.name : mod.name + ".disabled");
                if (!mod.path.renameTo(file)) {
                    return false;
                }
                mod.path = file;
                mod.enabled = z;
                return true;
            }
            File file2 = new File(mod.path.getParentFile(), (z ? new StringBuilder().append(mod.name).append(".dll") : new StringBuilder().append(mod.name).append(".dll.disabled")).toString());
            if (!mv(mod.path, file2)) {
                return false;
            }
            mod.path = file2;
            mod.enabled = z;
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    public static List<String> scanLangs() {
        ArrayList arrayList = new ArrayList();
        for (String str : ls(langDir())) {
            if (str.toLowerCase().endsWith(".json")) {
                arrayList.add(str);
            }
        }
        Collections.sort(arrayList);
        return arrayList;
    }

    public static File findPackage(String[] strArr) {
        File[] fileArr = {downloadDir(), new File(SD, "工程"), new File(SD, "Download/123云盘")};
        for (String str : strArr) {
            for (int i = 0; i < 3; i++) {
                File file = new File(fileArr[i], str);
                if (file.exists() && file.length() > 1024) {
                    return file;
                }
            }
        }
        return null;
    }
}
