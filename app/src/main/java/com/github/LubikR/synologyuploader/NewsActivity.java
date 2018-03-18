package com.github.LubikR.synologyuploader;


import android.os.Bundle;
import android.widget.TextView;

public class NewsActivity extends BaseActivity {

    TextView news;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news);

        news = (TextView) findViewById(R.id.textViewNews);
        SharedPreferencesManager.write(getString(R.string.versionReadTag), "1.4");
    }
}
