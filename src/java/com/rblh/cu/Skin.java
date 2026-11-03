package com.rblh.cu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/* loaded from: classes.dex */
public class Skin {
    private static final String P = "cu_skin";
    private static final int REQ = 9101;
    private static Bitmap sBmp;
    private static Bitmap sFill;
    private static final String[] STYLES = {"无（不透明）", "毛玻璃", "液态玻璃", "极简描边", "拟态", "荧光暗玻璃", "彩色渐变玻璃"};
    private static final String[] STYLE_KEYS = {"none", "frost", "liquid", "outline", "neumorph", "glow", "tint"};
    private static long sStamp = -1;
    private static int sFillW = -1;
    private static int sFillH = -1;

    public interface Picker {
        void pick(int i);
    }

    public static SharedPreferences sp(Context context) {
        return context.getSharedPreferences(P, 0);
    }

    public static File bgFile(Context context) {
        return new File(context.getFilesDir(), "skin_bg.jpg");
    }

    private static String style(Context context) {
        String string = sp(context).getString("glass_style", null);
        if (string == null) {
            return sp(context).getBoolean("glass", true) ? "frost" : "none";
        }
        return string;
    }

    private static String styleName(String str) {
        int i = 0;
        while (true) {
            String[] strArr = STYLE_KEYS;
            if (i >= strArr.length) {
                return STYLES[1];
            }
            if (strArr[i].equals(str)) {
                return STYLES[i];
            }
            i++;
        }
    }

    private static int styleIndex(String str) {
        int i = 0;
        while (true) {
            String[] strArr = STYLE_KEYS;
            if (i >= strArr.length) {
                return 1;
            }
            if (strArr[i].equals(str)) {
                return i;
            }
            i++;
        }
    }

    private static int dp(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static void attach(Context context, LinearLayout linearLayout) {
        if (context == null || linearLayout == null) {
            return;
        }
        try {
            LinearLayout linearLayoutCard = Ui.card(context);
            linearLayoutCard.addView(Ui.cardHeader(context, "外观", "主题色、自定义背景与玻璃样式。"));
            String str = bgFile(context).exists() ? "已设置" : "未设置";
            String strStyleName = styleName(style(context));
            linearLayoutCard.addView(Ui.desc(context, "背景图：" + str + "（" + ("stretch".equals(sp(context).getString("bg_mode", "fill")) ? "拉伸" : "填充") + "）\u3000玻璃：" + strStyleName));
            linearLayoutCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Skin.menu((Activity) view.getContext());
                }
            });
            linearLayout.addView(linearLayoutCard);
        } catch (Throwable th) {
        }
    }

    public static void menu(final Activity activity) {
        if (activity == null) {
            return;
        }
        final boolean zEquals = "stretch".equals(sp(activity).getString("bg_mode", "fill"));
        String strStyle = style(activity);
        String[] strArr = new String[5];
        strArr[0] = "主题色";
        strArr[1] = "选择背景图";
        StringBuilder sb = new StringBuilder();
        sb.append("背景显示：");
        sb.append(zEquals ? "拉伸" : "填充");
        strArr[2] = sb.toString();
        strArr[3] = "玻璃样式：" + styleName(strStyle);
        strArr[4] = "移除背景";
        modal(activity, "外观", strArr, -1, new Picker() {
            @Override
            public void pick(int i) {
                if (i == 0) {
                    Skin.themePicker(activity);
                    return;
                }
                if (i == 1) {
                    Skin.pickImage(activity);
                    return;
                }
                if (i == 2) {
                    Skin.sp(activity).edit().putString("bg_mode", zEquals ? "fill" : "stretch").apply();
                    Bitmap unused = Skin.sFill = null;
                    activity.recreate();
                } else {
                    if (i == 3) {
                        Skin.stylePicker(activity);
                        return;
                    }
                    Skin.bgFile(activity).delete();
                    Bitmap unused2 = Skin.sBmp = null;
                    long unused3 = Skin.sStamp = -1L;
                    Bitmap unused4 = Skin.sFill = null;
                    activity.recreate();
                }
            }
        });
    }

    public static void themePicker(final Activity activity) {
        modal(activity, "主题色", Theme.NAMES, Theme.index(), new Picker() {
            @Override
            public void pick(int i) {
                Theme.setIndex(activity, i);
            }
        });
    }

    public static void stylePicker(final Activity activity) {
        modal(activity, "玻璃样式", STYLES, styleIndex(style(activity)), new Picker() {
            @Override
            public void pick(int i) {
                Skin.sp(activity).edit().putString("glass_style", Skin.STYLE_KEYS[i]).apply();
                activity.recreate();
            }
        });
    }

    public static void modal(Activity activity, String str, String[] strArr, int i, final Picker picker) {
        try {
            ViewGroup viewGroup = (ViewGroup) activity.findViewById(android.R.id.content);
            if (viewGroup == null) {
                return;
            }
            final FrameLayout frameLayout = new FrameLayout(activity);
            frameLayout.setBackgroundColor(-1442840576);
            frameLayout.setClickable(true);
            frameLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Skin.dismiss(frameLayout);
                }
            });
            LinearLayout linearLayout = new LinearLayout(activity);
            linearLayout.setOrientation(1);
            int iDp = dp(activity, 20.0f);
            linearLayout.setPadding(iDp, iDp, iDp, dp(activity, 8.0f));
            GradientDrawable gradientDrawable = new GradientDrawable();
            String strStyle = style(activity);
            if ("liquid".equals(strStyle)) {
                gradientDrawable.setColors(new int[]{-650492073, -1508961254, -234222061});
                gradientDrawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
                gradientDrawable.setStroke(dp(activity, 1.0f), 1728053247);
            } else if ("glow".equals(strStyle)) {
                gradientDrawable.setColor(-234353646);
                gradientDrawable.setStroke(dp(activity, 1.0f), Theme.accent());
            } else if ("tint".equals(strStyle)) {
                gradientDrawable.setColors(new int[]{(Theme.accent() & 16777215) | 1711276032, 1432397306, 1727296182});
                gradientDrawable.setOrientation(GradientDrawable.Orientation.TL_BR);
                gradientDrawable.setStroke(dp(activity, 1.0f), 1442840575);
            } else if ("outline".equals(strStyle)) {
                gradientDrawable.setColor(-435548653);
                gradientDrawable.setStroke(dp(activity, 1.0f), -1426063361);
            } else {
                gradientDrawable.setColor(-133229542);
                gradientDrawable.setStroke(dp(activity, 1.0f), 587202559);
            }
            gradientDrawable.setCornerRadius(dp(activity, 20.0f));
            linearLayout.setBackground(gradientDrawable);
            TextView textView = new TextView(activity);
            textView.setText(str);
            textView.setTextSize(20.0f);
            textView.setTextColor(Ui.TEXT);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setPadding(0, 0, 0, dp(activity, 6.0f));
            linearLayout.addView(textView);
            int i2 = 0;
            while (i2 < strArr.length) {
                TextView textView2 = new TextView(activity);
                StringBuilder sb = new StringBuilder();
                sb.append(i2 == i ? "●  " : "     ");
                sb.append(strArr[i2]);
                textView2.setText(sb.toString());
                textView2.setTextSize(16.0f);
                textView2.setTextColor(i2 == i ? Theme.accent() : -985864);
                textView2.setPadding(dp(activity, 2.0f), dp(activity, 12.0f), dp(activity, 2.0f), dp(activity, 12.0f));
                final int fi2 = i2;
                textView2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Skin.dismiss(frameLayout);
                        picker.pick(fi2);
                    }
                });
                linearLayout.addView(textView2);
                i2++;
            }
            TextView textView3 = new TextView(activity);
            textView3.setText("取消");
            textView3.setTextSize(16.0f);
            textView3.setTextColor(Ui.SUB);
            textView3.setGravity(17);
            textView3.setPadding(0, dp(activity, 10.0f), 0, dp(activity, 10.0f));
            textView3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Skin.dismiss(frameLayout);
                }
            });
            linearLayout.addView(textView3);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(Math.min(dp(activity, 320.0f), activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 48.0f)), -2);
            layoutParams.gravity = 17;
            frameLayout.addView(linearLayout, layoutParams);
            viewGroup.addView(frameLayout, new FrameLayout.LayoutParams(-1, -1));
            frameLayout.setAlpha(0.0f);
            frameLayout.animate().alpha(1.0f).setDuration(150L).start();
        } catch (Throwable th) {
        }
    }

    static class Bridge {
        private final Activity a;
        private final Picker cb;
        private final View overlay;
        private final WebView web;

        Bridge(Activity activity, View view, WebView webView, Picker picker) {
            this.a = activity;
            this.overlay = view;
            this.web = webView;
            this.cb = picker;
        }

        @JavascriptInterface
        public void pick(final int i) {
            this.a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bridge.this.close();
                    Bridge.this.cb.pick(i);
                }
            });
        }

        @JavascriptInterface
        public void cancel() {
            this.a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bridge.this.close();
                }
            });
        }

        public void close() {
            try {
                ((ViewGroup) this.overlay.getParent()).removeView(this.overlay);
            } catch (Throwable th) {
            }
            try {
                this.web.destroy();
            } catch (Throwable th2) {
            }
        }
    }

    public static void dismiss(final View view) {
        try {
            view.animate().alpha(0.0f).setDuration(120L).withEndAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        ((ViewGroup) view.getParent()).removeView(view);
                    } catch (Throwable th) {
                    }
                }
            }).start();
        } catch (Throwable th) {
            try {
                ((ViewGroup) view.getParent()).removeView(view);
            } catch (Throwable th2) {
            }
        }
    }

    public static void pickImage(Activity activity) {
        try {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("image/*");
            intent.addCategory("android.intent.category.OPENABLE");
            activity.startActivityForResult(Intent.createChooser(intent, "选择背景图"), REQ);
        } catch (Throwable th) {
        }
    }

    public static void onResult(Activity activity, int i, int i2, Intent intent) {
        if (i != REQ || i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        try {
            Uri data = intent.getData();
            InputStream inputStreamOpenInputStream = activity.getContentResolver().openInputStream(data);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
            if (inputStreamOpenInputStream != null) {
                inputStreamOpenInputStream.close();
            }
            int iMax = Math.max(activity.getResources().getDisplayMetrics().widthPixels, activity.getResources().getDisplayMetrics().heightPixels);
            int i3 = 1;
            while (true) {
                int i4 = i3 * 2;
                if (Math.max(options.outWidth, options.outHeight) / i4 < iMax) {
                    break;
                } else {
                    i3 = i4;
                }
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = i3;
            InputStream inputStreamOpenInputStream2 = activity.getContentResolver().openInputStream(data);
            Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream2, null, options2);
            if (inputStreamOpenInputStream2 != null) {
                inputStreamOpenInputStream2.close();
            }
            if (bitmapDecodeStream == null) {
                return;
            }
            int width = bitmapDecodeStream.getWidth();
            int height = bitmapDecodeStream.getHeight();
            int iMax2 = Math.max(width, height);
            if (iMax2 != iMax) {
                float f = iMax / iMax2;
                width = Math.max(2, Math.round(width * f));
                height = Math.max(2, Math.round(height * f));
            }
            Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapDecodeStream, width, height, true);
            FileOutputStream fileOutputStream = new FileOutputStream(bgFile(activity));
            bitmapCreateScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fileOutputStream);
            fileOutputStream.close();
            sBmp = null;
            sStamp = -1L;
            sFill = null;
            activity.recreate();
        } catch (Throwable th) {
        }
    }

    public static void applyLater(final Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Skin.apply(activity);
                }
            };
            activity.getWindow().getDecorView().postDelayed(runnable, 120L);
            activity.getWindow().getDecorView().postDelayed(runnable, 520L);
        } catch (Throwable th) {
        }
    }

    public static void apply(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            File fileBgFile = bgFile(activity);
            if (!fileBgFile.exists()) {
                View viewFindViewById = activity.findViewById(android.R.id.content);
                View childAt = viewFindViewById != null ? ((ViewGroup) viewFindViewById).getChildAt(0) : activity.getWindow().getDecorView();
                if (childAt != null) {
                    paint(childAt, style(activity));
                    return;
                }
                return;
            }
            long jLastModified = fileBgFile.lastModified();
            if (sBmp == null || sStamp != jLastModified) {
                sBmp = BitmapFactory.decodeFile(fileBgFile.getAbsolutePath());
                sStamp = jLastModified;
            }
            Bitmap bitmap = sBmp;
            if (bitmap == null) {
                return;
            }
            if (!"stretch".equals(sp(activity).getString("bg_mode", "fill"))) {
                int i = activity.getResources().getDisplayMetrics().widthPixels;
                int i2 = activity.getResources().getDisplayMetrics().heightPixels;
                if (sFill == null || sFillW != i || sFillH != i2) {
                    sFill = centerCrop(sBmp, i, i2);
                    sFillW = i;
                    sFillH = i2;
                }
                Bitmap bitmap2 = sFill;
                if (bitmap2 != null) {
                    bitmap = bitmap2;
                }
            }
            BitmapDrawable bitmapDrawable = new BitmapDrawable(activity.getResources(), bitmap);
            bitmapDrawable.setGravity(119);
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{bitmapDrawable, new ColorDrawable(1711276032)});
            View viewFindViewById2 = activity.findViewById(android.R.id.content);
            View childAt2 = viewFindViewById2 != null ? ((ViewGroup) viewFindViewById2).getChildAt(0) : activity.getWindow().getDecorView();
            if (childAt2 == null) {
                return;
            }
            childAt2.setBackground(layerDrawable);
            if (childAt2 instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) childAt2;
                for (int i3 = 0; i3 < viewGroup.getChildCount(); i3++) {
                    View childAt3 = viewGroup.getChildAt(i3);
                    if (childAt3.getBackground() instanceof ColorDrawable) {
                        childAt3.setBackground(null);
                    }
                }
            }
            paint(childAt2, style(activity));
        } catch (Throwable th) {
        }
    }

    private static Bitmap centerCrop(Bitmap bitmap, int i, int i2) {
        try {
            float fMax = Math.max(i / bitmap.getWidth(), i2 / bitmap.getHeight());
            int iMax = Math.max(i, Math.round(bitmap.getWidth() * fMax));
            int iMax2 = Math.max(i2, Math.round(bitmap.getHeight() * fMax));
            return Bitmap.createBitmap(Bitmap.createScaledBitmap(bitmap, iMax, iMax2, true), (iMax - i) / 2, (iMax2 - i2) / 2, i, i2);
        } catch (Throwable th) {
            return bitmap;
        }
    }

    private static void paint(View view, String str) {
        try {
            Drawable background = view.getBackground();
            if ((background instanceof GradientDrawable) && !(view instanceof TextView)) {
                if (view instanceof ScrollView) {
                    view.setBackground(null);
                } else {
                    GradientDrawable gradientDrawable = (GradientDrawable) background.mutate();
                    if ("liquid".equals(str)) {
                        gradientDrawable.setAlpha(255);
                        gradientDrawable.setColors(new int[]{-650492073, -1508961254, -234222061});
                        gradientDrawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
                        gradientDrawable.setStroke(dp(view.getContext(), 1.0f), 1728053247);
                    } else if ("frost".equals(str)) {
                        gradientDrawable.setAlpha(225);
                        gradientDrawable.setStroke(0, 0);
                    } else if ("outline".equals(str)) {
                        gradientDrawable.setAlpha(22);
                        gradientDrawable.setStroke(dp(view.getContext(), 1.0f), -1711276033);
                    } else if ("neumorph".equals(str)) {
                        gradientDrawable.setAlpha(255);
                        gradientDrawable.setStroke(dp(view.getContext(), 1.0f), 587202559);
                    } else if ("glow".equals(str)) {
                        gradientDrawable.setAlpha(250);
                        gradientDrawable.setStroke(dp(view.getContext(), 1.0f), Theme.accent());
                    } else if ("tint".equals(str)) {
                        gradientDrawable.setAlpha(255);
                        gradientDrawable.setColors(new int[]{(Theme.accent() & 16777215) | 1291845632, 861971962, 1039430326});
                        gradientDrawable.setOrientation(GradientDrawable.Orientation.TL_BR);
                        gradientDrawable.setStroke(dp(view.getContext(), 1.0f), 1124073471);
                    } else {
                        gradientDrawable.setAlpha(255);
                        gradientDrawable.setStroke(0, 0);
                    }
                    view.setBackground(gradientDrawable);
                }
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    paint(viewGroup.getChildAt(i), str);
                }
            }
        } catch (Throwable th) {
        }
    }

    static class Glass extends Drawable {
        private final Bitmap bg;
        private final Path clip;
        private final Paint inner;
        private final Paint lens;
        private final View owner;
        private final float radius;
        private final Paint rim;
        private final Paint scrim;
        private final int sh;
        private final float stroke;
        private final int sw;
        private final int tint;
        private final Paint fill = new Paint(1);
        private final Paint sheen = new Paint(1);

        Glass(View view, Bitmap bitmap, int i, int i2, float f, float f2, int i3) {
            Paint paint = new Paint(1);
            this.rim = paint;
            Paint paint2 = new Paint(1);
            this.inner = paint2;
            this.lens = new Paint(3);
            Paint paint3 = new Paint(1);
            this.scrim = paint3;
            this.clip = new Path();
            this.owner = view;
            this.bg = bitmap;
            this.sw = Math.max(1, i);
            this.sh = Math.max(1, i2);
            this.radius = f;
            this.stroke = f2;
            this.tint = i3;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(f2);
            paint2.setStyle(Paint.Style.STROKE);
            paint2.setStrokeWidth(f2);
            paint2.setColor(637534208);
            paint3.setColor(1711276032);
        }

        @Override
        public void draw(Canvas canvas) {
            RectF rectF = new RectF(getBounds());
            this.clip.reset();
            Path path = this.clip;
            float f = this.radius;
            path.addRoundRect(rectF, f, f, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(this.clip);
            Bitmap bitmap = this.bg;
            if (bitmap != null && !bitmap.isRecycled()) {
                int[] r1 = new int[2];
                try {
                    this.owner.getLocationOnScreen(r1);
                } catch (Throwable th) {
                }
                float width = this.bg.getWidth() / this.sw;
                float height = this.bg.getHeight() / this.sh;
                int i = (int) (r1[0] * width);
                int i2 = (int) (r1[1] * height);
                int iWidth = (int) ((r1[0] + rectF.width()) * width);
                int iHeight = (int) ((r1[1] + rectF.height()) * height);
                int iMax = Math.max(0, Math.min(this.bg.getWidth() - 1, i));
                int iMax2 = Math.max(0, Math.min(this.bg.getHeight() - 1, i2));
                Rect rect = new Rect(iMax, iMax2, Math.max(iMax + 1, Math.min(this.bg.getWidth(), iWidth)), Math.max(iMax2 + 1, Math.min(this.bg.getHeight(), iHeight)));
                float fWidth = rectF.width() * 1.06f;
                float fHeight = rectF.height() * 1.06f;
                try {
                    canvas.drawBitmap(this.bg, rect, new RectF((rectF.width() - fWidth) / 2.0f, (rectF.height() - fHeight) / 2.0f, (rectF.width() + fWidth) / 2.0f, (rectF.height() + fHeight) / 2.0f), this.lens);
                } catch (Throwable th2) {
                }
                canvas.drawRect(rectF, this.scrim);
            } else {
                this.fill.setColor(this.tint);
                float f2 = this.radius;
                canvas.drawRoundRect(rectF, f2, f2, this.fill);
            }
            this.fill.setColor(184549375);
            float f3 = this.radius;
            canvas.drawRoundRect(rectF, f3, f3, this.fill);
            this.sheen.setShader(new LinearGradient(rectF.left, rectF.top, rectF.right, rectF.bottom, new int[]{-637534209, 16777215, 16777215, -637534209}, new float[]{0.0f, 0.16f, 0.84f, 1.0f}, Shader.TileMode.CLAMP));
            float f4 = this.radius;
            canvas.drawRoundRect(rectF, f4, f4, this.sheen);
            this.sheen.setShader(null);
            this.rim.setShader(null);
            this.rim.setColor(-419430401);
            this.rim.setStrokeWidth(this.stroke * 2.0f);
            RectF rectF2 = new RectF(rectF.left + this.stroke, rectF.top + this.stroke, rectF.right - this.stroke, rectF.bottom - this.stroke);
            float f5 = this.radius;
            canvas.drawRoundRect(rectF2, f5, f5, this.rim);
            this.rim.setColor(1509949439);
            this.rim.setStrokeWidth(this.stroke * 6.0f);
            RectF rectF3 = new RectF(rectF.left + (this.stroke * 3.0f), rectF.top + (this.stroke * 3.0f), rectF.right - (this.stroke * 3.0f), rectF.bottom - (this.stroke * 3.0f));
            float f6 = this.radius;
            canvas.drawRoundRect(rectF3, f6, f6, this.rim);
            this.inner.setColor(855638016);
            this.inner.setStrokeWidth(this.stroke);
            RectF rectF4 = new RectF(rectF.left + (this.stroke / 2.0f), rectF.top + (this.stroke / 2.0f), rectF.right - (this.stroke / 2.0f), rectF.bottom - (this.stroke / 2.0f));
            float f7 = this.radius;
            canvas.drawRoundRect(rectF4, f7, f7, this.inner);
            canvas.restore();
        }

        @Override
        public void setAlpha(int i) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return -3;
        }
    }
}
