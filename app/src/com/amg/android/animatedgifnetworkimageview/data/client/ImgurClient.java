package com.amg.android.animatedgifnetworkimageview.data.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.amg.android.animatedgifnetworkimageview.data.model.ImgurImage;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ImgurClient {
    private static final String TAG = ImgurClient.class.getSimpleName();

    private static final String IMGUR_V3_CLIENT_ID = "1967f096c58cd53";
    private static final String IMGUR_PATH = "https://api.imgur.com";

    private static final String IMGUR_GALLERY_V3_URI = "/3/gallery";
    private static final String IMGUR_GALLERY_R_V3_URI = "/r";

    private static final String IMGUR_GALLERY_SEARCH_V3_URI = "/search";
    private static final String IMGUR_GALLERY_SEARCH_V3_QUERY_KEY = "q";
    private static final String IMGUR_GALLERY_SEARCH_V3_QUERY_FILE_TYPE_KEY = "q_type";
    private static final String IMGUR_GALLERY_SEARCH_V3_EXCLUDE_ALBUMS = " album:false";

    public static final String IMGUR_EXT_ANIGIF = "anigif";
    public static final String IMGUR_EXT_JPG = "jpg";
    public static final String IMGUR_EXT_GIF = "gif";
    public static final String IMGUR_EXT_PNG = "png";

    private static final String IMGUR_HEADER_AUTH_KEY = "Authorization";
    private static final String IMGUR_HEADER_CLIENT_ID = "Client-ID ";

    private static ImgurClient imgurClient;
    private static Context ctx;

    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    private ArrayList<ImgurImage> imgurImages;

    public interface ImgurClientInterface{
        abstract void onPostExecute(boolean success);
    }

    public static synchronized ImgurClient getInstance(Context context){
        if (imgurClient == null) {
            imgurClient = new ImgurClient(context);
        }
        return imgurClient;
    }

    private ImgurClient(Context context){
        ctx = context.getApplicationContext();
        requestQueue = getRequestQueue();
        imageLoader = new ImageLoader(requestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<>(100);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public ArrayList<ImgurImage> getImgurImages(){
        return imgurImages;
    }

    public void getRGallery(String galleryName, final ImgurClientInterface callback){
        String url = buildRGalleryUrl(galleryName);
        Log.e(TAG,"getting R Gallery with url: "+ url);
        fetchImages(url,callback);
    }

    private String buildRGalleryUrl(String galleryName){
        return Uri.parse(IMGUR_PATH).buildUpon()
                .path(IMGUR_GALLERY_V3_URI)
                .appendPath(IMGUR_GALLERY_R_V3_URI)
                .appendPath(galleryName)
                .build().toString();
    }

    public void searchGallery(String searchTerm, ImgurClientInterface callback){
        searchGallery(searchTerm,null,false,callback);
    }

    public void searchGallery(String searchTerm, String searchType, boolean excludeAlbums, ImgurClientInterface callback){
        String url = buildSearchGalleryUrl(searchTerm,searchType,excludeAlbums);
        Log.e(TAG,"Searching Gallery with url: "+ url);
        fetchImages(url,callback);
    }

    private String buildSearchGalleryUrl(String searchTerm, String searchType, boolean excludeAlbums){
        if (excludeAlbums) {
            searchTerm += IMGUR_GALLERY_SEARCH_V3_EXCLUDE_ALBUMS;
        }
        Uri.Builder builder = Uri.parse(IMGUR_PATH).buildUpon().path(IMGUR_GALLERY_V3_URI)
                .appendPath(IMGUR_GALLERY_SEARCH_V3_URI)
                .appendQueryParameter(IMGUR_GALLERY_SEARCH_V3_QUERY_KEY, searchTerm);
        if (searchType != null) {
            builder.appendQueryParameter(IMGUR_GALLERY_SEARCH_V3_QUERY_FILE_TYPE_KEY,searchType);
        }
        return builder.build().toString();
    }

    private void fetchImages(String url, final ImgurClientInterface callback){
        ImgurStringRequest request =new ImgurStringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String string) {
                        JSONObject response = null;
                        try {
                            response = new JSONObject(string);
                            imgurImages = parseImages(response);
                            callback.onPostExecute(true);

                        }catch (Exception e){
                            Log.e(TAG,"Error parsing Imgur Detail (missing fields): ", e);
                            callback.onPostExecute(false);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Log.e(TAG, "Volley Error fetching Imgur Search:"  , e);
                callback.onPostExecute(false);
            }

        });
        getRequestQueue().add(request);
    }

    private ArrayList<ImgurImage> parseImages(JSONObject response) throws JSONException{
        ArrayList<ImgurImage> images = new ArrayList<>();
        JSONArray data = response.getJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            ImgurImage image = new ImgurImage();
            image.parse(object);
            if (image.isValid()) {
                images.add(image);
            } else {
                Log.e(TAG, "Error parsing Imgur Object: " + object.toString());
            }
        }
        return images.size()>0?images:null;
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(ctx);
        }
        return requestQueue;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    private class ImgurStringRequest extends StringRequest {

        public ImgurStringRequest(int method, String url, Response.Listener<String> listener,
                                  Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> map = new HashMap<String, String>();
            map.put(IMGUR_HEADER_AUTH_KEY, IMGUR_HEADER_CLIENT_ID + IMGUR_V3_CLIENT_ID);
            return map;
        }
    }

}
