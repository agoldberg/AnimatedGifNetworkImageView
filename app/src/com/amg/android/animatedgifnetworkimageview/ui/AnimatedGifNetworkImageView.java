package com.amg.android.animatedgifnetworkimageview.ui;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.InputStreamRequest;
import com.android.volley.toolbox.NetworkImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Stack;

public class AnimatedGifNetworkImageView extends NetworkImageView {

    private static final String TAG = AnimatedGifNetworkImageView.class.getSimpleName();

    private static final int TRANSITION_TIME = 250;

    private boolean mAnimateImageLoad = false;

    /** The URL of the network image to load */
    private String mUrl;

    /**
     * Resource ID of the image to be used as a placeholder until the network image is loaded.
     */
    private int mDefaultImageId;

    /**
     * Resource ID of the image to be used if the network response fails.
     */
    private int mErrorImageId;

    /** Local copy of the ImageLoader. */
    private ImageLoader mImageLoader;

    /** Current ImageContainer. (either in-flight or finished) */
    private ImageLoader.ImageContainer mImageContainer;

    private RequestQueue mRequestQueue;

    /** List of urls to try */
    Stack<String> mUrls;

    /** force loading of image regardless of view bounds or layout params */
    private boolean forceLoad = false;

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

    public AnimatedGifNetworkImageView(Context context) {
        this(context, null);
    }

    public AnimatedGifNetworkImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimatedGifNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setRequestQueue(RequestQueue requestQueue){
        this.mRequestQueue = requestQueue;
    }

    public void setImageUrl(String url, ImageLoader imageLoader) {
        Stack<String> stack = new Stack<>();
        stack.add(url);
        setImageUrl(stack, imageLoader);
    }

    /**
     * @param urls an array of urls to try in sucuessive order,
     * @param imageLoader
     */
    public void setImageUrl(Stack<String> urls, ImageLoader imageLoader){
        mImageLoader = imageLoader;
        mUrls = urls;
        mUrl = (!mUrls.isEmpty() ? mUrls.peek() : null);
        if (mUrl != null) {
            Log.d(TAG, "Trying Url: " + mUrl);
            loadImageIfNecessary(false);
        }
    }

    public void setDefaultImageResId(int defaultImage) {
        mDefaultImageId = defaultImage;
    }

    public void setErrorImageResId(int errorImage) {
        mErrorImageId = errorImage;
    }

    public void setGifResourceId(int resId) {
        InputStream is = getResources().openRawResource(resId);
        makeMovie(is);
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

    /**
     * Loads the image for the view if it isn't already loaded.
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     *
     */
    void loadImageIfNecessary(final boolean isInLayoutPass) {
        int width = getWidth();
        int height = getHeight();

        boolean isFullyWrapContent = getLayoutParams() != null
                && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT
                && getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        if (width == 0 && height == 0 && !isFullyWrapContent && !forceLoad) {
            return;
        }

        mMovie = null;
        boolean isAniGif = mUrl != null && mUrl.lastIndexOf(".") > 0 && mUrl.substring(mUrl.lastIndexOf("."),mUrl.length() ).equals(".gif");

        if (isAniGif) {
            InputStreamRequest gifRequest = new InputStreamRequest(Request.Method.GET, mUrl, new Response.Listener<byte[]>() {
                @Override
                public void onResponse(byte[] data) {
                    InputStream is = new ByteArrayInputStream(data);
                    makeMovie(is);
                }
            },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG,"Gif Request Error: "+ error.toString());
                        }
                    }
            );
            mRequestQueue.add(gifRequest);
        }else {
            // if the URL to be loaded in this view is empty, cancel any old requests and clear the
            // currently loaded image.
            if (TextUtils.isEmpty(mUrl)) {
                if (mImageContainer != null) {
                    mImageContainer.cancelRequest();
                    mImageContainer = null;
                }
                setImageBitmap(null);
                return;
            }

            // if there was an old request in this view, check if it needs to be canceled.
            if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
                if (mImageContainer.getRequestUrl().equals(mUrl)) {
                    // if the request is from the same URL, return.
                    return;
                } else {
                    // if there is a pre-existing request, cancel it if it's fetching a different URL.
                    mImageContainer.cancelRequest();
                    setImageBitmap(null);
                }
            }

            // The pre-existing content of this view didn't match the current URL. Load the new image
            // from the network.
            ImageLoader.ImageContainer newContainer = mImageLoader.get(mUrl,
                    new ImageLoader.ImageListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                                // we failed to retreive the image from that url, let's try the next url
                                // in the list, if we can.
                                if (mUrls != null && !mUrls.isEmpty()) {
                                    mUrls.pop();
                                    post(new Runnable() {
                                        @Override
                                        public void run() {
                                            setImageUrl(mUrls, mImageLoader);
                                        }
                                    });
                                    // none of the urls work, let's set a generic placeholder, if we can
                                } else if (mErrorImageId != 0) {
                                    setImageResource(mErrorImageId);
                                }
                            }
                        }

                        @Override
                        public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                            // If this was an immediate response that was delivered inside of a layout
                            // pass do not set the image immediately as it will trigger a requestLayout
                            // inside of a layout. Instead, defer setting the image by posting back to
                            // the main thread.
                            if (isImmediate && isInLayoutPass) {
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        onResponse(response, false);
                                    }
                                });
                                return;
                            }

                            if (response.getBitmap() != null) {
                                setImageBitmap(response.getBitmap());
                            } else if (mDefaultImageId != 0) {
                                setImageResource(mDefaultImageId);
                            }
                        }
                    });

            // update the ImageContainer to be the new bitmap container.
            mImageContainer = newContainer;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mImageContainer.cancelRequest();
            setImageBitmap(null);
            // also clear out the container so we can reload the image if necessary.
            mImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        if (mAnimateImageLoad) {
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{new ColorDrawable(
                    android.R.color.transparent), new BitmapDrawable(getContext().getResources(), bm)});
            setImageDrawable(td);
            td.startTransition(TRANSITION_TIME);
        } else {
            super.setImageBitmap(bm);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mMovie != null) {

            Paint p = new Paint();
            p.setAntiAlias(true);


            long now = android.os.SystemClock.uptimeMillis();
            if (mMovieStart == 0) {
                mMovieStart = now;
            }

            int dur = mMovie.duration();
            if (dur == 0) {
                dur = 1000;
            }
            int relTime = (int)((now - mMovieStart) % dur);
            mMovie.setTime(relTime);
            mMovie.draw(canvas, (getWidth() - mMovie.width())/2,
                    (getHeight() - mMovie.height())/2);

            invalidate();
        }else {
            super.onDraw(canvas);
        }
    }

}
