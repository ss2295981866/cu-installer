package com.rblh.cu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

/* loaded from: classes.dex */
public class HighlightList extends LinearLayout {
    private int alpha;
    private float hlHeight;
    private float hlTop;
    private final Paint paint;
    private float radius;
    private final RectF rect;
    private ValueAnimator running;
    private int selectedIndex;
    private float strokeW;

    public HighlightList(Context context, float f, float f2) {
        super(context);
        this.paint = new Paint(1);
        this.rect = new RectF();
        this.hlTop = 0.0f;
        this.hlHeight = 0.0f;
        this.alpha = 0;
        this.selectedIndex = -1;
        setOrientation(1);
        this.radius = f;
        this.strokeW = f2;
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(f2);
        this.paint.setColor(Theme.accent());
        setWillNotDraw(false);
    }

    public void snapTo(int i) {
        this.selectedIndex = i;
        View viewChildAt = childAt(i);
        if (viewChildAt == null || viewChildAt.getHeight() == 0) {
            post(new Runnable() { // from class: com.rblh.cu.HighlightList.1
                @Override // java.lang.Runnable
                public void run() {
                    HighlightList.this.snapTo(HighlightList.this.selectedIndex);
                }
            });
            return;
        }
        this.hlTop = viewChildAt.getTop();
        this.hlHeight = viewChildAt.getHeight();
        this.alpha = 255;
        invalidate();
    }

    public void animateTo(final int i, final Runnable runnable) {
        View viewChildAt = childAt(i);
        if (viewChildAt == null || viewChildAt.getHeight() == 0) {
            post(new Runnable() { // from class: com.rblh.cu.HighlightList.2
                @Override // java.lang.Runnable
                public void run() {
                    HighlightList.this.animateTo(i, runnable);
                }
            });
            return;
        }
        if (this.running != null) {
            this.running.cancel();
        }
        this.selectedIndex = i;
        final float top = this.hlHeight <= 1.0f ? viewChildAt.getTop() : this.hlTop;
        final float height = this.hlHeight <= 1.0f ? viewChildAt.getHeight() : this.hlHeight;
        final int i2 = this.hlHeight <= 1.0f ? 0 : this.alpha;
        final float top2 = viewChildAt.getTop();
        final float height2 = viewChildAt.getHeight();
        this.running = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.running.setDuration(320L);
        this.running.setInterpolator(new DecelerateInterpolator(1.4f));
        this.running.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.rblh.cu.HighlightList.3
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                HighlightList.this.hlTop = top + ((top2 - top) * fFloatValue);
                HighlightList.this.hlHeight = height + ((height2 - height) * fFloatValue);
                HighlightList.this.alpha = (int) (i2 + ((255 - i2) * Math.min(1.0f, fFloatValue * 2.5f)));
                HighlightList.this.invalidate();
            }
        });
        this.running.addListener(new AnimatorListenerAdapter() { // from class: com.rblh.cu.HighlightList.4
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        this.running.start();
    }

    public View childAt(int i) {
        if (i < 0 || i >= getChildCount()) {
            return null;
        }
        return getChildAt(i);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.hlHeight > 1.0f && this.alpha > 0) {
            this.paint.setAlpha(this.alpha);
            float f = this.strokeW / 2.0f;
            this.rect.set(f, this.hlTop + f, getWidth() - f, (this.hlTop + this.hlHeight) - f);
            canvas.drawRoundRect(this.rect, this.radius, this.radius, this.paint);
        }
    }
}
