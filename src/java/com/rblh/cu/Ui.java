package com.rblh.cu;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/* loaded from: classes.dex */
public class Ui {
    public static final int ACCENT = -11870592;
    public static final int ACCENT_D = -14498466;
    public static final int ACCENT_DIM = 642440832;
    public static final int BG = -16118253;
    public static final int BG2 = -15789030;
    public static final int CARD = -15393756;
    public static final int CARD2 = -14735309;
    public static final int CARD3 = -14011070;
    public static final int DANGER = -495247;
    public static final float R_BTN = 14.0f;
    public static final float R_CARD = 20.0f;
    public static final float R_SMALL = 10.0f;
    public static final int SUB = -7628632;
    public static final int SUB2 = -10720903;
    private static final String TAG_ANIM = "ui_stagger";
    public static final int TEXT = -985864;
    public static final int WARN = -278748;

    public static int dp(Context context, float f) {
        return (int) TypedValue.applyDimension(1, f, context.getResources().getDisplayMetrics());
    }

    public static GradientDrawable round(int i, float f) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(f);
        return gradientDrawable;
    }

    public static GradientDrawable roundStroke(int i, int i2, float f, int i3) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(f);
        if (i2 != 0 && i3 > 0) {
            gradientDrawable.setStroke(i3, i2);
        }
        return gradientDrawable;
    }

    public static GradientDrawable gradient(int i, int i2, float f) {
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{i, i2});
        gradientDrawable.setCornerRadius(f);
        return gradientDrawable;
    }

    public static View pageBg(Context context) {
        View view = new View(context);
        view.setBackground(gradient(BG2, BG, 0.0f));
        return view;
    }

    public static TextView tv(Context context, String str, float f, int i) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(f);
        textView.setTextColor(i);
        return textView;
    }

    public static TextView title(Context context, String str, float f, int i) {
        TextView textViewTv = tv(context, str, f, i);
        textViewTv.setLetterSpacing(0.08f);
        textViewTv.setTypeface(Typeface.DEFAULT_BOLD);
        return textViewTv;
    }

    public static LinearLayout card(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(round(CARD, dp(context, 20.0f)));
        int iDp = dp(context, 18.0f);
        linearLayout.setPadding(iDp, iDp, iDp, iDp);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(context, 14.0f);
        linearLayout.setLayoutParams(layoutParams);
        return linearLayout;
    }

    public static LinearLayout cardAccent(Context context) {
        LinearLayout linearLayoutCard = card(context);
        linearLayoutCard.setBackground(roundStroke(CARD, 861660315, dp(context, 20.0f), dp(context, 1.0f)));
        return linearLayoutCard;
    }

    public static LinearLayout cardHeader(Context context, String str, String str2) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.addView(title(context, str, 17.0f, TEXT), new LinearLayout.LayoutParams(0, -2, 1.0f));
        if (str2 != null && str2.length() > 0) {
            linearLayout.addView(helpIcon(context, str2));
        }
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(context, 10.0f);
        linearLayout.setLayoutParams(layoutParams);
        return linearLayout;
    }

    public static View helpIcon(final Context context, final String str) {
        TextView textView = new TextView(context);
        textView.setText("?");
        textView.setTextSize(12.0f);
        textView.setTextColor(SUB);
        textView.setGravity(17);
        textView.setBackground(round(-14471880, dp(context, 20.0f)));
        int iDp = dp(context, 22.0f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(iDp, iDp);
        layoutParams.leftMargin = dp(context, 6.0f);
        textView.setLayoutParams(layoutParams);
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.rblh.cu.Ui.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Ui.springPop(view);
                Ui.alert(context, "说明", str);
            }
        });
        return textView;
    }

    public static TextView desc(Context context, String str) {
        TextView textViewTv = tv(context, str, 13.0f, SUB);
        textViewTv.setLineSpacing(dp(context, 4.0f), 1.0f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(context, 6.0f);
        layoutParams.bottomMargin = dp(context, 4.0f);
        textViewTv.setLayoutParams(layoutParams);
        return textViewTv;
    }

    public static Button btn(Context context, String str, int i) {
        Button button = new Button(context);
        button.setText(str);
        button.setTextSize(15.0f);
        button.setTextColor((i == Theme.accent() || i == Theme.accentDark()) ? -16375537 : TEXT);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setBackground(ripple(context, i, dp(context, 14.0f)));
        button.setPadding(dp(context, 20.0f), dp(context, 12.0f), dp(context, 20.0f), dp(context, 12.0f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(context, 8.0f);
        button.setLayoutParams(layoutParams);
        pressable(button);
        return button;
    }

    public static Button smallBtn(Context context, String str, int i) {
        Button buttonBtn = btn(context, str, i);
        buttonBtn.setTextSize(14.0f);
        buttonBtn.setPadding(dp(context, 16.0f), dp(context, 9.0f), dp(context, 16.0f), dp(context, 9.0f));
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) buttonBtn.getLayoutParams();
        layoutParams.width = -2;
        layoutParams.topMargin = dp(context, 8.0f);
        buttonBtn.setLayoutParams(layoutParams);
        return buttonBtn;
    }

    public static Drawable ripple(Context context, int i, float f) {
        return new RippleDrawable(ColorStateList.valueOf(1090519039), round(i, f), null);
    }

    public static Drawable rippleStroke(Context context, int i, int i2, float f, int i3) {
        return new RippleDrawable(ColorStateList.valueOf(1090519039), roundStroke(i, i2, f, i3), null);
    }

    public static void pressable(View view) {
        view.setOnTouchListener(new View.OnTouchListener() { // from class: com.rblh.cu.Ui.2
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view2, MotionEvent motionEvent) {
                switch (motionEvent.getActionMasked()) {
                    case 0:
                        view2.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90L).setInterpolator(new DecelerateInterpolator()).start();
                        break;
                    case 1:
                    case 3:
                        view2.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160L).setInterpolator(new OvershootInterpolator(2.0f)).start();
                        break;
                }
                return false;
            }
        });
    }

    public static void springPop(final View view) {
        view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(110L).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() { // from class: com.rblh.cu.Ui.3
            @Override // java.lang.Runnable
            public void run() {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180L).setInterpolator(new OvershootInterpolator(3.0f)).start();
            }
        }).start();
    }

    public static void animateOut(View view, final Runnable runnable) {
        view.animate().alpha(0.0f).scaleX(0.98f).scaleY(0.98f).setDuration(140L).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() { // from class: com.rblh.cu.Ui.4
            @Override // java.lang.Runnable
            public void run() {
                if (runnable != null) {
                    runnable.run();
                }
            }
        }).start();
    }

    private static void safetyNet(final View view) {
        view.postDelayed(new Runnable() { // from class: com.rblh.cu.Ui.5
            @Override // java.lang.Runnable
            public void run() {
                Ui.forceVisible(view);
            }
        }, 1200L);
    }

    public static void showAll(View view) {
        forceVisible(view);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void forceVisible(View view) {
        if (TAG_ANIM.equals(view.getTag())) {
            view.setTag(null);
            view.animate().cancel();
            view.setAlpha(1.0f);
            view.setTranslationY(0.0f);
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                forceVisible(viewGroup.getChildAt(i));
            }
        }
    }

    public static void animateIn(View view) {
        view.setAlpha(0.0f);
        view.setTranslationY(dp(view.getContext(), 14.0f));
        view.animate().alpha(1.0f).translationY(0.0f).setDuration(260L).setInterpolator(new DecelerateInterpolator()).start();
    }

    public static void staggerIn(ViewGroup viewGroup, int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < viewGroup.getChildCount(); i3++) {
            View childAt = viewGroup.getChildAt(i3);
            if (isCard(childAt)) {
                if (childAt.getAlpha() >= 0.99f) {
                    childAt.setTag(TAG_ANIM);
                    childAt.setAlpha(0.0f);
                    childAt.setTranslationY(dp(childAt.getContext(), 18.0f));
                    childAt.animate().alpha(1.0f).translationY(0.0f).setStartDelay((i2 * 55) + i).setDuration(320L).setInterpolator(new DecelerateInterpolator()).start();
                    i2++;
                }
            } else if (childAt instanceof ViewGroup) {
                staggerIn((ViewGroup) childAt, (i2 * 40) + i);
            }
        }
    }

    private static boolean isCard(View view) {
        return (view instanceof LinearLayout) && view.getBackground() != null;
    }

    public static void countUp(final TextView textView, int i, int i2, long j) {
        ValueAnimator valueAnimatorOfInt = ValueAnimator.ofInt(i, i2);
        valueAnimatorOfInt.setDuration(j);
        valueAnimatorOfInt.setInterpolator(new DecelerateInterpolator());
        valueAnimatorOfInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.rblh.cu.Ui.6
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                textView.setText(String.valueOf(valueAnimator.getAnimatedValue()));
            }
        });
        valueAnimatorOfInt.start();
    }

    public static ScrollView scroll(Context context, View view) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setClipToPadding(false);
        int iDp = dp(context, 16.0f);
        scrollView.setPadding(iDp, dp(context, 8.0f), iDp, dp(context, 8.0f));
        scrollView.addView(view, new FrameLayout.LayoutParams(-1, -2));
        return scrollView;
    }

    public static LinearLayout column(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        return linearLayout;
    }

    public static TextView header(Context context, String str) {
        TextView textViewTv = tv(context, str, 26.0f, TEXT);
        textViewTv.setTypeface(Typeface.DEFAULT_BOLD);
        textViewTv.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(context, 10.0f);
        layoutParams.bottomMargin = dp(context, 18.0f);
        textViewTv.setLayoutParams(layoutParams);
        return textViewTv;
    }

    public static void styleSystemBars(Activity activity) {
        try {
            Window window = activity.getWindow();
            window.setStatusBarColor(BG);
            window.setNavigationBarColor(BG);
            window.getDecorView().setSystemUiVisibility(0);
            if (Build.VERSION.SDK_INT >= 30) {
                window.setNavigationBarDividerColor(BG);
            }
        } catch (Throwable th) {
        }
    }

    public static void toast(Context context, String str) {
        Toast.makeText(context, str, 0).show();
    }

    public static void alert(Context context, String str, String str2) {
        new AlertDialog.Builder(context).setTitle(str).setMessage(str2).setPositiveButton("好", (DialogInterface.OnClickListener) null).show();
    }
}
