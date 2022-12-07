package com.dunn.instrument.oom.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {
    public static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void click(View view) {
        Log.i(TAG,"click:");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
