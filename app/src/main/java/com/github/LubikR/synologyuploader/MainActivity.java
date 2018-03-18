package com.github.LubikR.synologyuploader;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.ma1co.openmemories.framework.DeviceInfo;
import com.github.ma1co.openmemories.framework.ImageInfo;
import com.github.ma1co.openmemories.framework.MediaManager;
import com.sony.scalar.provider.AvindexStore;

import org.apache.http.HttpException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends BaseActivity {

    private final String TAG = "MainActivity";

    DateFormat formatter = new SimpleDateFormat("ddMMyyyy");

    Button btnSettings;
    Button btnUpload;
    TextView statusTextView;

    String ip, port, user, passwd;
    Boolean https, debug;

    private boolean deleteAfterUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSettings = (Button) findViewById(R.id.Settings);
        btnUpload = (Button) findViewById(R.id.Upload_Now);
        statusTextView = (TextView) findViewById(R.id.textviewStatus);

        SharedPreferencesManager.init(getApplicationContext());

        /**
         For testing purpose > delete sharedpreferences
         SharedPreferencesManager.deleteAll();
         **/

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

        private void checkIfAlreadySet() {
            if ((SharedPreferencesManager.read(getString(R.string.port), null)) == null) {
             btnUpload.setEnabled(false);
            } else {
                ip = SharedPreferencesManager.read(getString(R.string.address), null);
                port = SharedPreferencesManager.read(getString(R.string.port), null);
                https = SharedPreferencesManager.readBoolean(getString(R.string.chckBoxUseHttps), false);
                user = SharedPreferencesManager.read(getString(R.string.user), null);
                passwd = SharedPreferencesManager.read(getString(R.string.passwd), null);
                deleteAfterUpload = SharedPreferencesManager.readBoolean(getString(R.string.chckBoxDelete), false);
                debug = SharedPreferencesManager.readBoolean(getString(R.string.chkkBoxLog), false);

                btnUpload.setEnabled(true);
            }
        }

    @Override
    protected void onResume() {
        super.onResume();

        //Check if Connection is already set
        checkIfAlreadySet();

        //News not read, show it
        String versionRead = SharedPreferencesManager.read(getString(R.string.versionReadTag),null);
        if (versionRead == null || !versionRead.equals("1.4")) {
            Intent intent = new Intent(MainActivity.this, NewsActivity.class);
            startActivity(intent);
        }
    }

    public void uploadNowClick(View view) {
        UploadPictures uploadPictures = new UploadPictures();
        uploadPictures.execute();
    }

    //uploading pictures by Multipart class
    class UploadPictures extends AsyncTask<Void, String, Integer> {

        @Override
        protected void onPreExecute() {
            setAutoPowerOffMode(false);
            publishProgress("Connecting to the server...");
            btnSettings.setEnabled(false);
            btnUpload.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            int result = 0;
            int count = 0;

            MediaManager mediaManager = MediaManager.create(getApplicationContext());
            Cursor cursor = mediaManager.queryImages();

            if (cursor == null) {
                publishProgress("Not possible to get images. Probably not supported device.");
                if (debug) {
                    Logger.error(TAG, "Not possible to get images. Probably not supported device");
                }
                ;
                result = -1;
            } else if ((count = cursor.getCount()) == 0) {
                publishProgress("Nothing to upload.");
                result = -1;
                if (debug) {
                    Logger.error(TAG, "Nothing to upload");
                } ;
            } else {
                try {
                    int i = 0;

                    //Get URL
                    if (debug) {
                        Logger.info(TAG, "Getting address : " +
                                ip.replaceAll(".", "*") + ":" + port + " https:" + https); } ;
                    String address = SynologyAPI.getAddress(ip, port, https);

                    //Check Synology API and retrieve maxVersion
                    JSONObject jsonObject = SynologyAPI.CheckAPIAndRetrieveMaxVersions(address);
                    String maxVersionAuth = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.API.Auth")).getString("maxVersion");
                    String maxVersionUpload = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.FileStation.Upload")).getString("maxVersion");
                    if (debug) {
                        Logger.info(TAG, "Got maxVersions: SYNO.API.Auth=" + maxVersionAuth +
                                ", SYNO.FileStation.Upload=" + maxVersionUpload);
                    }
                    ;

                    // Login to Synology
                    jsonObject = SynologyAPI.login(address, user, passwd, maxVersionAuth);
                    String sid = jsonObject.getJSONObject("data").getString("sid");
                    if (debug) {
                        Logger.info(TAG, "Logged-in OK, sid=" + sid);
                    }
                    ;

                    //upload images
                    String model = DeviceInfo.getInstance().getModel();
                    String directory = SharedPreferencesManager.read(getString(R.string.settingViewDirectory), null);

                    while (cursor.moveToNext()) {
                        i++;
                        ImageInfo info = mediaManager.getImageInfo(cursor);
                        String filename = info.getFilename();
                        Date date = info.getDate();

                        publishProgress("Uploading " + i + " / " + count);

                        MultiPartUpload multipart = new MultiPartUpload(address +
                                SynologyAPI.uploadAPI, "UTF-8", sid);

                        multipart.addFormField("api", "SYNO.FileStation.Upload");
                        multipart.addFormField("version", maxVersionUpload);
                        if (debug) { Logger.info(TAG, "UploadVersion=" + maxVersionUpload); };
                        multipart.addFormField("method", "upload");
                        multipart.addFormField("path", "/" + directory + "/" + model + "/" + formatter.format(date));
                        if (debug) { Logger.info(TAG, "Path=" + "/" + directory + "/" + model + "/" +
                                formatter.format(date)); };
                        multipart.addFormField("create_parents", "true");
                        multipart.addFilePart("file", (FileInputStream) info.getFullImage(), filename);
                        if (debug) { Logger.info(TAG, "Filename=" + filename); };

                        String json2 = new String(multipart.finish());
                        String uploadResult = new JSONObject(json2).getString("success");

                        if (uploadResult.equals("true")) {
                            if (deleteAfterUpload) {
                                // Delete uploaded image
                                ContentResolver resolver = getApplicationContext().getContentResolver();
                                Uri uri = mediaManager.getImageContentUri();
                                long id = mediaManager.getImageId(cursor);
                                AvindexStore.Images.Media.deleteImage(resolver, uri, id);
                            }
                        } else {
                            String errorCode = new JSONObject(json2).getJSONObject("error").getString("code");
                            if (debug) { Logger.info(TAG, "Error during upload: " + errorCode); };
                            throw new HttpException(errorCode);
                        }
                    }
                    cursor.close();

                    // Do logout
                    jsonObject = SynologyAPI.logout(address, sid, maxVersionAuth);

                } catch (Exception e) {
                    result = -1;
                    if (e instanceof IOException) {
                        publishProgress("Error - " + e.getMessage());
                        if (debug) {
                            Logger.error(TAG, "IOException - " + e.getMessage());
                        }
                        ;
                    } else if (e instanceof JSONException) {
                        publishProgress("JSON error - " + e.getMessage());
                        if (debug) {
                            Logger.error(TAG, "JSONException - " + e.getMessage());
                        }
                        ;
                    } else if (e instanceof HttpException) {
                        publishProgress("Connection error : " + e.getMessage());
                        if (debug) {
                            Logger.error(TAG, "HttpException - " + e.toString());
                        }
                        ;
                    } else {
                        publishProgress("Something wrong with error : " + e.getMessage());
                        if (debug) {
                            Logger.error(TAG, "AnotherException - " + e.getMessage());
                        }
                        ;
                    }
                }
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(String... strings) {
            statusTextView.setText(strings[0]);
            if (debug) { Logger.info(TAG, "Publish progress: " + strings[0]); };
        }

        @Override
        protected void onPostExecute(Integer result) {
            setAutoPowerOffMode(true);
            if (result == 0) {
                statusTextView.setText("Everything uploaded OK!");
                if (debug) { Logger.info(TAG, "Everything uploaded OK"); };
            }
            btnUpload.setEnabled(true);
            btnSettings.setEnabled(true);
        }
    }
}
