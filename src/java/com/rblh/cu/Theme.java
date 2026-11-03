package com.rblh.cu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.LinearLayout;

/* loaded from: classes.dex */
public class Theme {
    private static final int DEFAULT = -11870592;
    private static final int DEFAULT_D = -14498466;
    private static final String KEY = "accent";
    private static final String PREFS = "cu_theme";
    private static Context sCtx;
    public static final String[] NAMES = {"翠绿", "青色", "天蓝", "紫色", "粉色", "橙色", "黄色", "红色"};
    public static final int[] LIGHT = {-11870592, -14494738, -10443270, -5796870, -757066, -290244, -340971, Ui.DANGER};
    public static final int[] DARK = {-14498466, -16215630, -12877066, -7643914, -1292135, -429290, -1395960, -1096636};
    private static int sAccent = -11870592;
    private static int sDark = -14498466;

    public static void init(Context context) {
        if (context == null) {
            return;
        }
        try {
            Context applicationContext = context.getApplicationContext();
            sCtx = applicationContext;
            int i = applicationContext.getSharedPreferences(PREFS, 0).getInt(KEY, -11870592);
            sAccent = i;
            sDark = darkOf(i);
        } catch (Throwable th) {
        }
    }

    public static int accent() {
        return sAccent;
    }

    public static int accentDark() {
        return sDark;
    }

    public static int accentDim() {
        return (sAccent & 16777215) | 637534208;
    }

    private static int darkOf(int i) {
        int i2 = 0;
        while (true) {
            int[] iArr = LIGHT;
            if (i2 >= iArr.length) {
                return -14498466;
            }
            if (iArr[i2] == i) {
                return DARK[i2];
            }
            i2++;
        }
    }

    public static int indexOf(int i) {
        for (int i2 = 0; i2 < NAMES.length; i2++) {
            if (LIGHT[i2] == i) {
                return i2;
            }
        }
        return 0;
    }

    public static int index() {
        return indexOf(sAccent);
    }

    public static void setIndex(Activity activity, int i) {
        if (i >= 0) {
            int[] iArr = LIGHT;
            if (i >= iArr.length) {
                return;
            }
            sAccent = iArr[i];
            sDark = DARK[i];
            try {
                Context context = sCtx;
                if (context == null) {
                    context = activity;
                }
                context.getSharedPreferences(PREFS, 0).edit().putInt(KEY, sAccent).apply();
            } catch (Throwable th) {
            }
            if (activity != null) {
                activity.recreate();
            }
        }
    }

    public static void pick(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("主题色");
            builder.setSingleChoiceItems(NAMES, indexOf(sAccent), new DialogInterface.OnClickListener() { // from class: com.rblh.cu.Theme.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    int unused = Theme.sAccent = Theme.LIGHT[i];
                    int unused2 = Theme.sDark = Theme.DARK[i];
                    try {
                        (Theme.sCtx != null ? Theme.sCtx : activity).getSharedPreferences(Theme.PREFS, 0).edit().putInt(Theme.KEY, Theme.sAccent).apply();
                    } catch (Throwable th) {
                    }
                    dialogInterface.dismiss();
                    activity.recreate();
                }
            });
            builder.setNegativeButton("取消", (DialogInterface.OnClickListener) null);
            builder.show();
        } catch (Throwable th) {
        }
    }

    public static void attach(Context context, LinearLayout linearLayout) {
        if (context == null || linearLayout == null) {
            return;
        }
        try {
            LinearLayout linearLayoutCard = Ui.card(context);
            linearLayoutCard.addView(Ui.cardHeader(context, "主题色", "选择界面的强调色，点一下即生效。"));
            linearLayoutCard.addView(Ui.desc(context, "当前：" + NAMES[indexOf(sAccent)]));
            linearLayoutCard.setOnClickListener(new View.OnClickListener() { // from class: com.rblh.cu.Theme.2
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    Theme.pick((Activity) view.getContext());
                }
            });
            linearLayout.addView(linearLayoutCard);
        } catch (Throwable th) {
        }
    }
}
