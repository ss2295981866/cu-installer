package com.rblh.cu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* loaded from: classes.dex */
public class InstallResultReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.rblh.cu.INSTALL_RESULT";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        int intExtra = intent.getIntExtra("android.content.pm.extra.STATUS", -1);
        String stringExtra = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
        if (intExtra == 0) {
            Ui.toast(context, "安装成功");
            return;
        }
        if (intExtra == -1) {
            Intent intent2 = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT");
            if (intent2 != null) {
                intent2.addFlags(268435456);
                try {
                    context.startActivity(intent2);
                    return;
                } catch (Throwable th) {
                    Ui.toast(context, "无法打开安装确认: " + th);
                    return;
                }
            }
            return;
        }
        Ui.toast(context, "安装失败: " + stringExtra);
    }
}
