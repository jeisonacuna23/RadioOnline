package com.radio.generica;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class VisualizerView extends View {
    private Paint paint;
    private RectF rect;
    private float[] heights;
    private int numBars = 7;
    private Handler handler;
    private Runnable runnable;
    private boolean isAnimating = false;
    private Random random = new Random();

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE); // Espectro Blanco
        rect = new RectF();
        heights = new float[numBars];
        handler = new Handler(Looper.getMainLooper());
        
        runnable = new Runnable() {
            @Override
            public void run() {
                if (isAnimating) {
                    for (int i = 0; i < numBars; i++) {
                        heights[i] = 10 + random.nextInt(30); 
                    }
                    invalidate();
                    handler.postDelayed(this, 150); 
                }
            }
        };
    }

    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            handler.post(runnable);
        }
    }

    public void stopAnimation() {
        isAnimating = false;
        handler.removeCallbacks(runnable);
        for (int i = 0; i < numBars; i++) {
            heights[i] = 5; 
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float barWidth = width / (numBars * 2f);
        float spacing = barWidth;

        for (int i = 0; i < numBars; i++) {
            float h = heights[i];
            float left = i * (barWidth + spacing) + spacing;
            float right = left + barWidth;
            float top = height - h;
            float bottom = height;

            rect.set(left, top, right, bottom);
            canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, paint);
        }
    }
}
