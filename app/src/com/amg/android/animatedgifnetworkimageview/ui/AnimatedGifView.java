package com.amg.android.animatedgifnetworkimageview.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class AnimatedGifView extends View {

    private static final String TAG = AnimatedGifView.class.getSimpleName();

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

    public AnimatedGifView(Context context) {
        this(context, null);
    }

    public AnimatedGifView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimatedGifView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);
    }

    public void setImageResource(int resId) {
        InputStream is = getResources().openRawResource(resId);
        makeMovie(is);
    }

    public void setImageUrl(final String url){
        GifTask gifTask = new GifTask(url);
        gifTask.execute();
    }

    private void makeMovie(InputStream is){
        if (DECODE_STREAM) {
            mMovie = Movie.decodeStream(is);
        } else {
            byte[] array = streamToBytes(is);
            mMovie = Movie.decodeByteArray(array, 0, array.length);
        }
        post(new Runnable() {
            public void run() {
                invalidate();
            }
        });

    }

    private class GifTask extends AsyncTask<Void, Void, Void> {
        private final Void Void = null;

        private String url;

        public GifTask(String url){
            this.url = url;
        }

        @Override
        protected void onPreExecute() { }

        @Override
        protected void onPostExecute(Void result) {

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URLConnection conn = new URL(url).openConnection();
                conn.connect();
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 8192);
                makeMovie(bis);

            } catch (Exception e){
                Log.e(TAG, "Error fetching Gif:", e);
            }
            return Void;
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
