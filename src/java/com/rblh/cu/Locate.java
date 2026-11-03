package com.rblh.cu;

import java.io.File;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class Locate {
    private static final String DEF = "/storage/emulated/0/Download/Casualties Unknown_7.0.1Demo";
    private static final String EXE = "CasualtiesUnknown.exe";
    private static String cached;
    private static long stamp;

    public static File defaultDir() {
        File fileFind = find();
        return fileFind != null ? fileFind : new File(DEF);
    }

    public static File find() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis - stamp < 8000) {
            if (cached == null) {
                return null;
            }
            File file = new File(cached);
            if (hasExe(file)) {
                return file;
            }
        }
        stamp = jCurrentTimeMillis;
        String strScan = scan();
        cached = strScan;
        if (strScan != null) {
            return new File(cached);
        }
        return null;
    }

    private static String scan() {
        File[] fileArrListFiles;
        ArrayList arrayList = new ArrayList();
        arrayList.add(new File(Core.SD, "Download"));
        arrayList.add(new File(Core.SD, "Downloads"));
        arrayList.add(new File(Core.SD, "Documents"));
        arrayList.add(new File(Core.SD, "Games"));
        arrayList.add(new File(Core.SD, "游戏"));
        arrayList.add(new File(Core.SD, "PC"));
        arrayList.add(new File(Core.SD));
        File[] fileArrListFiles2 = new File("/storage").listFiles();
        if (fileArrListFiles2 != null) {
            for (int i = 0; i < fileArrListFiles2.length; i++) {
                String name = fileArrListFiles2[i].getName();
                if (!name.equals("emulated") && !name.equals("self")) {
                    arrayList.add(fileArrListFiles2[i]);
                    arrayList.add(new File(fileArrListFiles2[i], "Download"));
                }
            }
        }
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            File file = (File) arrayList.get(i2);
            if (file.isDirectory()) {
                if (hasExe(file)) {
                    return file.getAbsolutePath();
                }
                File[] fileArrListFiles3 = file.listFiles();
                if (fileArrListFiles3 == null) {
                    continue;
                } else {
                    for (File file2 : fileArrListFiles3) {
                        if (file2.isDirectory() && hasExe(file2)) {
                            return file2.getAbsolutePath();
                        }
                    }
                    for (File file3 : fileArrListFiles3) {
                        if (file3.isDirectory()) {
                            String lowerCase = file3.getName().toLowerCase();
                            if ((lowerCase.contains("casualt") || lowerCase.contains("unknown") || lowerCase.equals("cu")) && (fileArrListFiles = file3.listFiles()) != null) {
                                for (File file4 : fileArrListFiles) {
                                    if (file4.isDirectory() && hasExe(file4)) {
                                        return file4.getAbsolutePath();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasExe(File file) {
        if (new File(file, EXE).isFile()) {
            return true;
        }
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return false;
        }
        for (int i = 0; i < fileArrListFiles.length; i++) {
            if (fileArrListFiles[i].isFile() && fileArrListFiles[i].getName().equalsIgnoreCase(EXE)) {
                return true;
            }
        }
        return false;
    }
}
