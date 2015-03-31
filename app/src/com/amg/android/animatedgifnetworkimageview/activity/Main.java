package com.amg.android.animatedgifnetworkimageview.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;

import com.amg.android.animatedgifnetworkimageview.R;
import com.amg.android.animatedgifnetworkimageview.fragment.ImgurListFragment;
import com.amg.android.animatedgifnetworkimageview.ui.AnimatedGifView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class Main extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new AnimatedGifView(this,R.drawable.freak_out));
//        setContentView(R.layout.activity_main);
//
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.container, new ImgurListFragment())
//                    .commit();
//        }
    }
}
