package com.github.LubikR.synologyuploader;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.ma1co.openmemories.framework.DeviceInfo;
import com.github.ma1co.openmemories.framework.ImageInfo;
import com.github.ma1co.openmemories.framework.MediaManager;
import com.sony.scalar.sysutil.ScalarInput;
import com.sony.scalar.provider.AvindexStore;

import org.apache.http.HttpException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    Button btnSettings;
    Button btnUpload;
    TextView statusTextView;

    DateFormat formatter = new SimpleDateFormat("ddMMyyyy");
    String maxVersionAuth = new String();
    String maxVersionUpload = new String();
    String sid = new String();

    final static String Protocol = "http://";
    final static String Port = ":5000";
    final static String RootAPI = "/webapi/";
    final static String uploadAPI = "entry.cgi";
    String getAPIs = "query.cgi?api=SYNO.API.Info&version=1&method=query&query=SYNO.API.Auth,SYNO.FileStation.Upload";
    String authAPI = "auth.cgi?api=SYNO.API.Auth&version=VERSION&method=login&account=USER&passwd=PASSWORD&session=FileStation&format=cookie";
    String authAPILogout = "auth.cgi?api=SYNO.API.Auth&version=VERSION&method=logout&_sid=SID";

    String Address;
    String IP = null;
    String user = null;
    String passwd = null;
    String directory = null;

    private static long backupPressedTime;

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

        checkIfAlreadySet();

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

        private void checkIfAlreadySet() {
            if ((IP = SharedPreferencesManager.read(getString(R.string.address), null)) == null) {
             btnUpload.setEnabled(false);
            } else {
                user = SharedPreferencesManager.read(getString(R.string.user), null);
                passwd = SharedPreferencesManager.read(getString(R.string.passwd), null);
                directory = SharedPreferencesManager.read(getString(R.string.settingViewDirectory), null);
                btnUpload.setEnabled(true);
            }
        }

    @Override
    protected void onResume() {
        super.onResume();
       checkIfAlreadySet();
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
                publishProgress("Not possible to get images.");
                result = -1;
            } else if ((count = cursor.getCount()) == 0) {
                publishProgress("Nothing to upload.");
                result = -1;
            } else {

                try {
                    int i = 0;
                    String model = DeviceInfo.getInstance().getModel();
                    Address = Protocol + IP + Port + RootAPI;

                    //Check Synology API and retrieve maxVersion
                    Http.Response response = Http.get(Address + getAPIs);
                    String json = new String(response.getResponseBytes());
                    response.close();
                    JSONObject jsonObject = new JSONObject(json);
                    maxVersionAuth = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.API.Auth")).getString("maxVersion");
                    maxVersionUpload = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.FileStation.Upload")).getString("maxVersion");

                    // Login to Synology
                    authAPI = authAPI.replace("USER", user);
                    authAPI = authAPI.replace("PASSWORD", passwd);
                    response = Http.get(Address + authAPI.replace("VERSION", maxVersionAuth));
                    json = new String(response.getResponseBytes());
                    response.close();
                    jsonObject = new JSONObject(json);
                    sid = jsonObject.getJSONObject("data").getString("sid");

                    //upload images
                    while (cursor.moveToNext()) {
                        i++;
                        ImageInfo info = mediaManager.getImageInfo(cursor);
                        String filename = info.getFilename();
                        Date date = info.getDate();

                        publishProgress("Uploading " + i + " / " + count);

                        MultiPartUpload multipart = new MultiPartUpload(Address + uploadAPI, "UTF-8", sid);

                        multipart.addFormField("api", "SYNO.FileStation.Upload");
                        multipart.addFormField("version", maxVersionUpload);
                        multipart.addFormField("method", "upload");
                        multipart.addFormField("path", "/" + directory + "/" + model + "/" + formatter.format(date));
                        multipart.addFormField("create_parents", "true");
                        multipart.addFilePart("file", (FileInputStream) info.getFullImage(), filename);

                        String json2 = new String(multipart.finish());
                        String uploadResult = new JSONObject(json2).getString("success");

                        if (uploadResult.equals("true")) {
                            // Delete uploaded image
                            ContentResolver resolver = getApplicationContext().getContentResolver();
                            Uri uri = mediaManager.getImageContentUri();
                            long id = mediaManager.getImageId(cursor);
                            AvindexStore.Images.Media.deleteImage(resolver, uri, id);
                        } else {
                            String errorCode = new JSONObject(json2).getJSONObject("error").getString("code");
                            throw new HttpException(errorCode);
                        }
                    }
                    cursor.close();

                    //update logout URI
                    authAPILogout = authAPILogout.replace("VERSION", maxVersionAuth);
                    authAPILogout = authAPILogout.replace("SID", sid);

                    //Do logout
                    response = Http.get(Address + authAPILogout);
                    json = new String(response.getResponseBytes());
                    response.close();
                    jsonObject = new JSONObject(json);

                } catch (Exception e) {
                    result = -1;
                    if (e instanceof IOException) {
                        publishProgress("Error - " + e.getMessage());
                    } else if (e instanceof JSONException) {
                        publishProgress("JSON error - " + e.getMessage());
                    } else if (e instanceof HttpException) {
                        publishProgress("Connection error : " + e.getMessage());
                    } else {
                        publishProgress("Something wrong with error : " + e.getMessage());
                    }
                }
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(String... strings) {
            statusTextView.setText(strings[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            setAutoPowerOffMode(true);
            if (result == 0) {
                statusTextView.setText("Everything uploaded OK!");
            }
            btnUpload.setEnabled(true);
            btnSettings.setEnabled(true);
        }
    }

    protected void setAutoPowerOffMode(boolean enable) {
        String mode = enable ? "APO/NORMAL" : "APO/NO";// or "APO/SPECIAL" ?
        Intent intent = new Intent();
        intent.setAction("com.android.server.DAConnectionManagerService.apo");
        intent.putExtra("apo_info", mode);
        sendBroadcast(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (event.getScanCode()) {
            case ScalarInput.ISV_KEY_DELETE:
            case ScalarInput.ISV_KEY_SK2:
            case ScalarInput.ISV_KEY_MENU:
                return onDeleteKeyUp();
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    protected boolean onDeleteKeyUp() {
        onBackPressed();
        return true;
    }
}
