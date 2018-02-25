package com.github.LubikR.synologyuploader;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sony.scalar.sysutil.ScalarInput;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsActivity extends Activity {

    String getAPIs = "query.cgi?api=SYNO.API.Info&version=1&method=query&query=SYNO.API.Auth,SYNO.FileStation.Upload,SYNO.FileStation.CheckPermission";
    String authAPI = "auth.cgi?api=SYNO.API.Auth&version=VERSION&method=login&account=USER&passwd=PASSWORD&session=FileStation&format=cookie";
    String checkPermissoinAPI = "entry.cgi?api=SYNO.FileStation.CheckPermission&version=VRSN&method=write&path=DIRECTORY&filename=test.tmp&_sid=SID";
    String authAPILogout = "auth.cgi?api=SYNO.API.Auth&version=VERSION&method=logout&_sid=SID";
    private boolean isOK = false;
    String checkPermissoinVersion = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        final EditText editViewServer = (EditText) findViewById(R.id.server);
        final EditText editViewUser = (EditText) findViewById(R.id.user);
        final EditText editViewPasswd = (EditText) findViewById(R.id.passwd);
        final EditText editTextDir = (EditText) findViewById(R.id.directory);
        final TextView textViewStatus = (TextView) findViewById(R.id.settingsViewStaus);

        final Button btnSave = (Button) findViewById(R.id.buttonSave);

        editViewServer.setText(SharedPreferencesManager.read(getString(R.string.address), null));

        editViewUser.setText(SharedPreferencesManager.read(getString(R.string.user), null));
        editViewPasswd.setText(SharedPreferencesManager.read(getString(R.string.passwd), null));
        editTextDir.setText(SharedPreferencesManager.read(getString(R.string.settingViewDirectory), null));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textViewStatus.setText("");
                String result = "Everyting OK! Push the button again to save it.";
                String ip = editViewServer.getText().toString();
                String passwd = editViewPasswd.getText().toString();
                String user = editViewUser.getText().toString();
                String directory = editTextDir.getText().toString();
                String maxVersionAuth = null;

                boolean connectionOK = false;

                if (!isOK) {
                    String address = MainActivity.Protocol + ip + MainActivity.Port + MainActivity.RootAPI;
                    Http.Response response;
                    String json;
                    JSONObject jsonObject;
                    String sid = null;

                    isOK = true;

                    //Check connection to Synology
                    try {
                        response = Http.get(address + getAPIs);
                        json = new String(response.getResponseBytes());
                        response.close();
                        jsonObject = new JSONObject(json);
                        maxVersionAuth = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.API.Auth")).getString("maxVersion");
                        checkPermissoinVersion = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.FileStation.CheckPermission")).getString("maxVersion");
                    } catch (Exception e) {
                        result = "Server not reached : " + e.toString();
                        isOK = false;
                    }

                    if (isOK) {
                        // Login to Synology
                        String authAPIReplaced = authAPI.replace("USER", user);
                        authAPIReplaced = authAPIReplaced.replace("PASSWORD", passwd);
                        authAPIReplaced = authAPIReplaced.replace("VERSION", maxVersionAuth);
                        try {
                            response = Http.get(address + authAPIReplaced);
                            json = new String(response.getResponseBytes());
                            response.close();
                            jsonObject = new JSONObject(json);
                            if ((jsonObject.getString("success")).equals("true")) {
                                sid = jsonObject.getJSONObject("data").getString("sid");
                                connectionOK = true;
                            } else throw new JSONException(jsonObject.getJSONObject("error").getString("code"));
                        } catch (Exception e) {
                            result = "Login failed : Synology returns error " + e.getMessage();
                            isOK = false;
                        }
                    }

                    //Check remote directory a permission
                    if (isOK){
                        String checkPermissoinAPIReplaced = checkPermissoinAPI.replace("VRSN", checkPermissoinVersion);
                        checkPermissoinAPIReplaced = checkPermissoinAPIReplaced.replace("DIRECTORY", "/" + directory);
                        checkPermissoinAPIReplaced = checkPermissoinAPIReplaced.replace("SID", sid);
                        try {
                            response = Http.get(address + checkPermissoinAPIReplaced);
                            json = new String(response.getResponseBytes());
                            response.close();
                            jsonObject = new JSONObject(json);
                            if ((jsonObject.getString("success")).equals("true")) {

                            } else throw new JSONException(jsonObject.getJSONObject("error").getString("code"));
                        }
                        catch (Exception e) {
                            result = "Permission problem : Synology returns error " + e.getMessage();
                            isOK = false;
                        }
                    }

                    if (connectionOK) {
                    //Logout
                    String authAPILogoutReplaced = authAPILogout.replace("VERSION", maxVersionAuth);
                    authAPILogoutReplaced = authAPILogoutReplaced.replace("SID", sid);

                    try {
                    response = Http.get(address + authAPILogoutReplaced);
                    json = new String(response.getResponseBytes());
                    response.close();
                    jsonObject = new JSONObject(json);}
                    catch (Exception e){
                        //TODO : do something with exceptions
                        }
                    }

                    // Show result to the screen
                    textViewStatus.setText(result);
                    textViewStatus.setVisibility(View.VISIBLE);
                }
                else {
                    SharedPreferencesManager.write(getString(R.string.address), ip);
                    SharedPreferencesManager.write(getString(R.string.passwd), passwd);
                    SharedPreferencesManager.write(getString(R.string.user), user);
                    SharedPreferencesManager.write(getString(R.string.settingViewDirectory), directory);
                    finish();
                }
                // If everything is OK change button text to "Save"
                if (isOK) btnSave.setText("Save");
            }
        });
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
