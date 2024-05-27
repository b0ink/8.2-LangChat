package com.example.langchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class AudioMessageWaveformView extends View {
    private List<Integer> amplitudes = new ArrayList<>();

    public AudioMessageWaveformView(Context context) {
        super(context);
    }

    public AudioMessageWaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void addAmplitude(int amplitude) {
        amplitudes.add(amplitude);
        if (amplitudes.size() > getWidth() / 10) {
            amplitudes.remove(0); // Keep a fixed number of amplitudes to fit within the view width
        }
        invalidate(); // Refresh the view
    }

    public void reset() {
        amplitudes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT); // Background color

        int width = getWidth();
        int height = getHeight();
        int middle = height / 2;
        int maxAmplitude = 32767; // Max amplitude value for 16-bit PCM

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2);

        int x = 0;
        for (int amplitude : amplitudes) {
            float scaledHeight = (float) amplitude / maxAmplitude * middle;
            canvas.drawLine(x, middle + scaledHeight, x, middle - scaledHeight, paint);
            x += 10; // Move to the next x position
        }
    }
}
