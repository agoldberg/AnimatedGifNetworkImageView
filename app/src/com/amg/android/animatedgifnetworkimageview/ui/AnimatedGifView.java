package com.amg.android.animatedgifnetworkimageview.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.view.View;

import com.amg.android.animatedgifnetworkimageview.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AnimatedGifView extends View {

    private Movie mMovie;
    private long mMovieStart;

    private static final boolean DECODE_STREAM = true;

    private static byte[] streamToBytes(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = is.read(buffer)) >= 0) {
                os.write(buffer, 0, len);
            }
        } catch (java.io.IOException e) {
        }
        return os.toByteArray();
    }

    public AnimatedGifView(Context context, int resId) {
        super(context);

        setFocusable(true);

        InputStream is = context.getResources().openRawResource(resId);

        if (DECODE_STREAM) {
            mMovie = Movie.decodeStream(is);
        } else {
            byte[] array = streamToBytes(is);
            mMovie = Movie.decodeByteArray(array, 0, array.length);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFCCCCCC);

        Paint p = new Paint();
        p.setAntiAlias(true);

        long now = android.os.SystemClock.uptimeMillis();
        if (mMovieStart == 0) {
            mMovieStart = now;
        }
        if (mMovie != null) {
            int dur = mMovie.duration();
            if (dur == 0) {
                dur = 1000;
            }
            int relTime = (int)((now - mMovieStart) % dur);
            mMovie.setTime(relTime);
            mMovie.draw(canvas, getWidth() - mMovie.width(),
                    getHeight() - mMovie.height());
            invalidate();
        }
    }

}
