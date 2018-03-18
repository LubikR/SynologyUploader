package com.github.LubikR.synologyuploader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Broadcasts extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity mainActivity = (MainActivity) context;
        int i = intent.getIntExtra("data", 0);
        mainActivity.progressBar.setProgress(i);
    }
}
