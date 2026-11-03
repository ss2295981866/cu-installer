package com.rblh.cu;

import android.util.Base64;

/* loaded from: classes.dex */
public class Obf {
    private static final byte[] K = "CuMgr2026#obf".getBytes();

    public static String d(String str) {
        try {
            byte[] bArrDecode = Base64.decode(str, 0);
            for (int i = 0; i < bArrDecode.length; i++) {
                bArrDecode[i] = (byte) (bArrDecode[i] ^ K[i % K.length]);
            }
            return new String(bArrDecode, "UTF-8");
        } catch (Throwable th) {
            return "";
        }
    }

    public static boolean intact(String str) {
        return str != null && str.length() >= 20 && str.startsWith("本软件完全免费") && str.endsWith("请立即退款并举报。");
    }
}
