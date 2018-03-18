package com.github.LubikR.synologyuploader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Broadcasts extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, final Intent intent) {
        final MainActivity mainActivity = (MainActivity) context;
        final int i = intent.getIntExtra("data", 0);

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.progressBar.setProgress(i);
            }
        });
    }
}
