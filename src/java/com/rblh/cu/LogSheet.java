package com.rblh.cu;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;

public class LogSheet {
    public static void show(final Activity a) {
        try {
            final ViewGroup content = (ViewGroup) a.findViewById(android.R.id.content);
            if (content == null) {
                return;
            }
            final FrameLayout overlay = new FrameLayout(a);
            overlay.setBackgroundColor(0x66000000);
            overlay.setClickable(true);
            overlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss(overlay);
                }
            });

            final int screenH = a.getResources().getDisplayMetrics().heightPixels;
            final int panelH = (int) (screenH * 0.88f);
            final float half = panelH * 0.5f;

            final LinearLayout panel = new LinearLayout(a);
            panel.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Ui.CARD);
            float r = Ui.dp(a, 22.0f);
            bg.setCornerRadii(new float[]{r, r, r, r, 0.0f, 0.0f, 0.0f, 0.0f});
            panel.setBackground(bg);
            panel.setPadding(0, Ui.dp(a, 6.0f), 0, 0);

            final LinearLayout head = new LinearLayout(a);
            head.setOrientation(LinearLayout.VERTICAL);

            View handle = new View(a);
            GradientDrawable hb = new GradientDrawable();
            hb.setColor(Ui.SUB2);
            hb.setCornerRadius(Ui.dp(a, 2.0f));
            handle.setBackground(hb);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(Ui.dp(a, 40.0f), Ui.dp(a, 4.0f));
            hp.gravity = Gravity.CENTER_HORIZONTAL;
            hp.topMargin = Ui.dp(a, 8.0f);
            head.addView(handle, hp);

            LinearLayout titleRow = new LinearLayout(a);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            int p = Ui.dp(a, 20.0f);
            titleRow.setPadding(p, Ui.dp(a, 10.0f), p, Ui.dp(a, 6.0f));

            TextView title = new TextView(a);
            title.setText("运行日志");
            title.setTextSize(18.0f);
            title.setTextColor(Ui.TEXT);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            titleRow.addView(title);

            final TextView rawBtn = new TextView(a);
            rawBtn.setText("原文");
            rawBtn.setTextSize(14.0f);
            rawBtn.setTextColor(Ui.SUB);
            rawBtn.setPadding(Ui.dp(a, 10.0f), Ui.dp(a, 4.0f), Ui.dp(a, 10.0f), Ui.dp(a, 4.0f));
            titleRow.addView(rawBtn);

            TextView exportBtn = new TextView(a);
            exportBtn.setText("导出");
            exportBtn.setTextSize(14.0f);
            exportBtn.setTextColor(Ui.SUB);
            exportBtn.setPadding(Ui.dp(a, 10.0f), Ui.dp(a, 4.0f), Ui.dp(a, 10.0f), Ui.dp(a, 4.0f));
            titleRow.addView(exportBtn);

            TextView closeBtn = new TextView(a);
            closeBtn.setText("关闭");
            closeBtn.setTextSize(14.0f);
            closeBtn.setTextColor(Ui.SUB);
            closeBtn.setPadding(Ui.dp(a, 10.0f), Ui.dp(a, 4.0f), 0, Ui.dp(a, 4.0f));
            titleRow.addView(closeBtn);

            head.addView(titleRow);
            panel.addView(head, new LinearLayout.LayoutParams(-1, -2));

            final TextView body = new TextView(a);
            body.setTextSize(12.0f);
            body.setTextColor(Ui.TEXT);
            body.setTypeface(android.graphics.Typeface.MONOSPACE);
            body.setLineSpacing(0.0f, 1.15f);
            body.setPadding(p, Ui.dp(a, 4.0f), p, Ui.dp(a, 16.0f));
            body.setTextIsSelectable(true);
            body.setText(ModLog.details());

            ScrollView scroll = new ScrollView(a);
            scroll.addView(body, new LinearLayout.LayoutParams(-1, -2));
            panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

            final boolean[] raw = {false};
            rawBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    raw[0] = !raw[0];
                    rawBtn.setText(raw[0] ? "翻译" : "原文");
                    body.setText(ModLog.detailsExt(160, raw[0]));
                }
            });
            exportBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        File dir = new File("/storage/emulated/0/Download/cu-logs");
                        dir.mkdirs();
                        File f = new File(dir, "modlog.txt");
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(ModLog.detailsExt(2000, true).getBytes("UTF-8"));
                        fos.close();
                        Ui.toast(a, "已导出：" + f.getAbsolutePath());
                    } catch (Throwable th) {
                        Ui.toast(a, "导出失败");
                    }
                }
            });
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss(overlay);
                }
            });

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, panelH, Gravity.BOTTOM);
            overlay.addView(panel, lp);
            content.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

            panel.setTranslationY(panelH);
            panel.animate().translationY(half).setDuration(240).start();

            head.setOnTouchListener(new View.OnTouchListener() {
                float downY;
                float startT;

                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downY = motionEvent.getRawY();
                            startT = panel.getTranslationY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float t = startT + (motionEvent.getRawY() - downY);
                            if (t < 0.0f) {
                                t = 0.0f;
                            }
                            if (t > panelH) {
                                t = panelH;
                            }
                            panel.setTranslationY(t);
                            return true;
                        case MotionEvent.ACTION_UP:
                            float t2 = panel.getTranslationY();
                            if (t2 > panelH * 0.6f) {
                                dismiss(overlay);
                            } else if (t2 > panelH * 0.22f) {
                                panel.animate().translationY(half).setDuration(180).start();
                            } else {
                                panel.animate().translationY(0.0f).setDuration(180).start();
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            });
        } catch (Throwable th) {
            Ui.toast(a, "日志读取失败");
        }
    }

    private static void dismiss(final FrameLayout overlay) {
        try {
            int h = overlay.getHeight();
            View panel = overlay.getChildAt(0);
            if (panel != null) {
                panel.animate().translationY(h).setDuration(180).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        ViewGroup parent = (ViewGroup) overlay.getParent();
                        if (parent != null) {
                            parent.removeView(overlay);
                        }
                    }
                }).start();
            } else if (overlay.getParent() != null) {
                ((ViewGroup) overlay.getParent()).removeView(overlay);
            }
        } catch (Throwable th) {
            try {
                if (overlay.getParent() != null) {
                    ((ViewGroup) overlay.getParent()).removeView(overlay);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
