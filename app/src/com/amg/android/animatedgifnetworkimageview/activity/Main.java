package com.amg.android.animatedgifnetworkimageview.activity;


import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.amg.android.animatedgifnetworkimageview.R;
import com.amg.android.animatedgifnetworkimageview.fragment.ImgurListFragment;

public class Main extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ImgurListFragment())
                    .commit();
        }
    }
}
