package com.rblh.cu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/* loaded from: classes.dex */
public class FrameHighlight extends View {
    private int alpha;
    private float height;
    private final Paint paint;
    private float radius;
    private final RectF rect;
    private float strokeW;
    private float top;

    public FrameHighlight(Context context, float f, float f2) {
        super(context);
        this.paint = new Paint(1);
        this.rect = new RectF();
        this.top = 0.0f;
        this.height = 0.0f;
        this.alpha = 0;
        this.radius = f;
        this.strokeW = f2;
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(f2);
        this.paint.setColor(Theme.accent());
        setLayerType(2, this.paint);
    }

    public void setBounds(float f, float f2) {
        this.top = f;
        this.height = f2;
        this.alpha = 255;
        invalidate();
    }

    public void setAnimated(float f, float f2, int i) {
        this.top = f;
        this.height = f2;
        this.alpha = i;
        invalidate();
    }

    public float getTopValue() {
        return this.top;
    }

    public float getHeightValue() {
        return this.height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.height <= 1.0f || this.alpha <= 0) {
            return;
        }
        this.paint.setAlpha(this.alpha);
        float f = this.strokeW / 2.0f;
        this.rect.set(f, this.top + f, getWidth() - f, (this.top + this.height) - f);
        canvas.drawRoundRect(this.rect, this.radius, this.radius, this.paint);
    }
}
