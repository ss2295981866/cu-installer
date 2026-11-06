package com.rblh.cu;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* loaded from: classes.dex */
public class Packer {

    public interface Progress {
        void on(int i, String str);
    }

    public static void extract(File file, File file2, Progress progress) throws Exception {
        byte[] bArrMagic = magic(file);
        String lowerCase = file.getName().toLowerCase();
        if (lowerCase.endsWith(".zip") || isZip(bArrMagic)) {
            extractZip(file, file2, progress);
            return;
        }
        if (lowerCase.endsWith(".tar.gz") || lowerCase.endsWith(".tgz") || isGz(bArrMagic)) {
            extractTarGz(file, file2, progress);
            return;
        }
        if (lowerCase.endsWith(".rar") || isRar(bArrMagic)) {
            extractRar(file, file2, progress);
            return;
        }
        if (lowerCase.endsWith(".7z") || is7z(bArrMagic)) {
            extract7z(file, file2, progress);
            return;
        }
        throw new IOException("不认识的压缩格式");
    }

    static byte[] magic(File file) throws IOException {
        byte[] bArr = new byte[8];
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        try {
            int i = randomAccessFile.read(bArr);
            if (i <= 0) {
                return new byte[0];
            }
            if (i == 8) {
                return bArr;
            }
            byte[] bArr2 = new byte[i];
            System.arraycopy(bArr, 0, bArr2, 0, i);
            return bArr2;
        } finally {
            randomAccessFile.close();
        }
    }

    static boolean isGz(byte[] bArr) {
        return bArr.length >= 3 && bArr[0] == 31 && bArr[1] == -117;
    }

    static boolean isZip(byte[] bArr) {
        return bArr.length >= 4 && bArr[0] == 80 && bArr[1] == 75;
    }

    static boolean isRar(byte[] bArr) {
        return bArr.length >= 4 && bArr[0] == 82 && bArr[1] == 97 && bArr[2] == 114 && bArr[3] == 33;
    }

    static boolean is7z(byte[] bArr) {
        return bArr.length >= 6 && bArr[0] == 55 && bArr[1] == 122 && bArr[2] == -68 && bArr[3] == -81 && bArr[4] == 39 && bArr[5] == 28;
    }

    static void extractZip(File file, File file2, Progress progress) throws Exception {
        ZipFile zipFile = new ZipFile(file);
        try {
            int size = zipFile.size();
            int i = 0;
            Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
            while (enumerationEntries.hasMoreElements()) {
                ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                i++;
                String name = zipEntryNextElement.getName();
                if (name.indexOf("__MACOSX") >= 0) {
                    if (progress != null) {
                        progress.on((i * 100) / Math.max(1, size), name);
                    }
                } else {
                    File file3 = new File(file2, name);
                    if (file3.getCanonicalPath().startsWith(file2.getCanonicalPath())) {
                        if (zipEntryNextElement.isDirectory()) {
                            file3.mkdirs();
                        } else {
                            File parentFile = file3.getParentFile();
                            if (parentFile != null) {
                                parentFile.mkdirs();
                            }
                            InputStream inputStream = zipFile.getInputStream(zipEntryNextElement);
                            FileOutputStream fileOutputStream = new FileOutputStream(file3);
                            copy(inputStream, fileOutputStream);
                            fileOutputStream.close();
                            inputStream.close();
                        }
                        if (progress != null) {
                            progress.on((i * 100) / Math.max(1, size), name);
                        }
                    }
                }
            }
        } finally {
            zipFile.close();
        }
    }

    static int tarCount(File file) throws Exception {
        GZIPInputStream gZIPInputStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file), 65536));
        try {
            byte[] bArr = new byte[512];
            int i = 0;
            while (readFully(gZIPInputStream, bArr, 512) >= 512 && !isZero(bArr)) {
                long jOctal = octal(bArr, 124, 12);
                int i2 = bArr[156] & 255;
                if (i2 == 76) {
                    int i3 = (int) jOctal;
                    readFully(gZIPInputStream, new byte[i3], i3);
                    skipFully(gZIPInputStream, pad(jOctal));
                } else if (i2 == 53) {
                    skipFully(gZIPInputStream, jOctal + pad(jOctal));
                } else if (i2 == 120 || i2 == 103) {
                    skipFully(gZIPInputStream, jOctal + pad(jOctal));
                } else {
                    i++;
                    skipFully(gZIPInputStream, jOctal + pad(jOctal));
                }
            }
            return i;
        } finally {
            gZIPInputStream.close();
        }
    }

    static void extractTarGz(File file, File file2, Progress progress) throws Exception {
        int iTarCount = tarCount(file);
        GZIPInputStream gZIPInputStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file), 65536));
        try {
            byte[] bArr = new byte[512];
            int i = 0;
            String strTrim = null;
            while (readFully(gZIPInputStream, bArr, 512) >= 512 && !isZero(bArr)) {
                long jOctal = octal(bArr, 124, 12);
                int i2 = bArr[156] & 255;
                String name = parseName(bArr);
                if (i2 == 76) {
                    int i3 = (int) jOctal;
                    byte[] bArr2 = new byte[i3];
                    readFully(gZIPInputStream, bArr2, i3);
                    strTrim = new String(bArr2, "UTF-8").trim();
                    skipFully(gZIPInputStream, pad(jOctal));
                } else {
                    if (strTrim == null) {
                        strTrim = name;
                    }
                    if (i2 == 120 || i2 == 103) {
                        skipFully(gZIPInputStream, jOctal + pad(jOctal));
                    } else {
                        if (strTrim.length() == 0) {
                            break;
                        }
                        File file3 = new File(file2, strTrim);
                        if (!file3.getCanonicalPath().startsWith(file2.getCanonicalPath())) {
                            skipFully(gZIPInputStream, pad(jOctal));
                        } else {
                            if (i2 == 53 || strTrim.endsWith("/")) {
                                file3.mkdirs();
                            } else {
                                File parentFile = file3.getParentFile();
                                if (parentFile != null) {
                                    parentFile.mkdirs();
                                }
                                FileOutputStream fileOutputStream = new FileOutputStream(file3);
                                copyLimited(gZIPInputStream, fileOutputStream, jOctal);
                                fileOutputStream.close();
                            }
                            skipFully(gZIPInputStream, pad(jOctal));
                            i++;
                            if (progress != null) {
                                progress.on((i * 100) / Math.max(1, iTarCount), strTrim);
                            }
                            strTrim = null;
                        }
                    }
                    strTrim = null;
                }
            }
        } finally {
            gZIPInputStream.close();
        }
    }

    static void copyLimited(InputStream inputStream, OutputStream outputStream, long j) throws IOException {
        int i;
        byte[] bArr = new byte[65536];
        while (j > 0 && (i = inputStream.read(bArr, 0, (int) Math.min(65536, j))) > 0) {
            outputStream.write(bArr, 0, i);
            j -= i;
        }
    }

    static void extractRar(File file, File file2, Progress progress) throws Exception {
        char c;
        int i;
        Class<?> cls = Class.forName("com.github.junrar.Archive");
        Class<?> cls2 = Class.forName("com.github.junrar.rarfile.FileHeader");
        Method method = cls.getMethod("nextFileHeader", new Class[0]);
        Method method2 = cls.getMethod("close", new Class[0]);
        Method method3 = cls2.getMethod("isDirectory", new Class[0]);
        Method method4 = cls2.getMethod("getFileName", new Class[0]);
        char c2 = 2;
        int i2 = 1;
        Method method5 = cls.getMethod("extractFile", cls2, OutputStream.class);
        Object objNewInstance = cls.getConstructor(File.class).newInstance(file);
        try {
            int i3 = 0;
            for (Object objInvoke = method.invoke(objNewInstance, new Object[0]); objInvoke != null; objInvoke = method.invoke(objNewInstance, new Object[0])) {
                i3++;
            }
            method2.invoke(objNewInstance, new Object[0]);
            Object objNewInstance2 = cls.getConstructor(File.class).newInstance(file);
            try {
                Object objInvoke2 = method.invoke(objNewInstance2, new Object[0]);
                int i4 = 0;
                while (objInvoke2 != null) {
                    i4 += i2;
                    String str = (String) method4.invoke(objInvoke2, new Object[0]);
                    if (str == null) {
                        str = "";
                    }
                    if (str.indexOf("__MACOSX") >= 0 || str.length() <= 0) {
                        c = c2;
                    } else {
                        File file3 = new File(file2, str);
                        if (!file3.getCanonicalPath().startsWith(file2.getCanonicalPath())) {
                            c = 2;
                        } else if (((Boolean) method3.invoke(objInvoke2, new Object[0])).booleanValue()) {
                            file3.mkdirs();
                            c = 2;
                        } else {
                            File parentFile = file3.getParentFile();
                            if (parentFile != null) {
                                parentFile.mkdirs();
                            }
                            FileOutputStream fileOutputStream = new FileOutputStream(file3);
                            c = 2;
                            method5.invoke(objNewInstance2, objInvoke2, fileOutputStream);
                            fileOutputStream.close();
                        }
                    }
                    if (progress != null) {
                        i = 1;
                        progress.on((i4 * 100) / Math.max(1, i3), str);
                    } else {
                        i = 1;
                    }
                    int i5 = i;
                    objInvoke2 = method.invoke(objNewInstance2, new Object[0]);
                    c2 = c;
                    i2 = i5;
                }
                method2.invoke(objNewInstance2, new Object[0]);
            } catch (Throwable th) {
                method2.invoke(objNewInstance2, new Object[0]);
                throw th;
            }
        } catch (Throwable th2) {
            method2.invoke(objNewInstance, new Object[0]);
            throw th2;
        }
    }

    static void extract7z(File src, File dest, Progress cb) throws Exception {
        int total = 0;
        SevenZFile zf = new SevenZFile(src);
        try {
            SevenZArchiveEntry entry;
            while ((entry = zf.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    total++;
                }
            }
        } finally {
            zf.close();
        }
        SevenZFile zf2 = new SevenZFile(src);
        try {
            SevenZArchiveEntry entry;
            int i = 0;
            while ((entry = zf2.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    new File(dest, name).mkdirs();
                    continue;
                }
                if (name.indexOf("__MACOSX") >= 0 || name.length() == 0) {
                    continue;
                }
                File out = new File(dest, name);
                if (!out.getCanonicalPath().startsWith(dest.getCanonicalPath())) {
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                InputStream in = zf2.getInputStream(entry);
                FileOutputStream os = new FileOutputStream(out);
                copy(in, os);
                os.close();
                in.close();
                i++;
                if (cb != null) {
                    cb.on(i * 100 / Math.max(1, total), name);
                }
            }
        } finally {
            zf2.close();
        }
    }

    static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[65536];
        while (true) {
            int i = inputStream.read(bArr);
            if (i <= 0) {
                return;
            } else {
                outputStream.write(bArr, 0, i);
            }
        }
    }

    static boolean isZero(byte[] bArr) {
        for (byte b : bArr) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    static long octal(byte[] bArr, int i, int i2) {
        long j = 0;
        for (int i3 = i; i3 < i + i2; i3++) {
            int i4 = bArr[i3] & 255;
            if (i4 != 0 && i4 != 32) {
                if (i4 < 48 || i4 > 55) {
                    break;
                }
                j = (j * 8) + (i4 - 48);
            }
        }
        return j;
    }

    static String parseName(byte[] bArr) {
        String strTrim = new String(bArr, 0, 100).trim();
        String strTrim2 = new String(bArr, 345, 155).trim();
        if (strTrim2.length() <= 0) {
            return strTrim;
        }
        return strTrim2 + "/" + strTrim;
    }

    static long pad(long j) {
        return (512 - (j % 512)) % 512;
    }

    static int readFully(InputStream inputStream, byte[] bArr, int i) throws IOException {
        int i2 = 0;
        while (i2 < i) {
            int i3 = inputStream.read(bArr, i2, i - i2);
            if (i3 <= 0) {
                break;
            }
            i2 += i3;
        }
        return i2;
    }

    static void skipFully(InputStream inputStream, long j) throws IOException {
        while (j > 0) {
            long jSkip = inputStream.skip(j);
            if (jSkip <= 0) {
                if (inputStream.read() < 0) {
                    return;
                } else {
                    jSkip = 1;
                }
            }
            j -= jSkip;
        }
    }
}
