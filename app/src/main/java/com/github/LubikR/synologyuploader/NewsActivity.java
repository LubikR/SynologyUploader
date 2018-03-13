package com.github.LubikR.synologyuploader;


import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import com.sony.scalar.sysutil.ScalarInput;

public class NewsActivity extends Activity {

    TextView news;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news);

        news = (TextView) findViewById(R.id.textViewNews);
        SharedPreferencesManager.write(getString(R.string.VersionRead), "1.2");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (event.getScanCode()) {
            case ScalarInput.ISV_KEY_DELETE:
            case ScalarInput.ISV_KEY_SK2:
            case ScalarInput.ISV_KEY_MENU:
                return onDeleteKeyUp();
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    protected boolean onDeleteKeyUp() {
        onBackPressed();
        return true;
    }
}
