package com.github.LubikR.synologyuploader;


import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import com.sony.scalar.sysutil.ScalarInput;

public class NewsActivity extends BaseActivity {

    TextView news;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news);

        news = (TextView) findViewById(R.id.textViewNews);
        SharedPreferencesManager.write(getString(R.string.versionReadTag), getString(R.string.tagLastVersion));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (event.getScanCode()) {
            case ScalarInput.ISV_KEY_ENTER:
                return onDeleteKeyUp();
            default:
                return super.onKeyDown(keyCode, event);
        }
    }
}
