package com.github.LubikR.synologyuploader;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

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
        final EditText editTextPort = (EditText) findViewById(R.id.port);

        final Button btnSave = (Button) findViewById(R.id.buttonSave);
        final CheckBox chboxSecure = (CheckBox) findViewById(R.id.checkBox_Secure);
        final CheckBox checkBoxDelete = (CheckBox) findViewById(R.id.checkBox_Delete);

        editViewServer.setText(SharedPreferencesManager.read(getString(R.string.address), null));

        editViewUser.setText(SharedPreferencesManager.read(getString(R.string.user), null));
        editViewPasswd.setText(SharedPreferencesManager.read(getString(R.string.passwd), null));
        editTextDir.setText(SharedPreferencesManager.read(getString(R.string.settingViewDirectory), null));
        editTextPort.setText(SharedPreferencesManager.read(getString(R.string.port), null));

        chboxSecure.setChecked(SharedPreferencesManager.readBoolean(getString(R.string.chckBoxUseHttps), false));
        checkBoxDelete.setChecked(SharedPreferencesManager.readBoolean(getString(R.string.chckBoxDelete), false));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String result = "Everyting OK! Push the button again to save it.";
                String ip = editViewServer.getText().toString();
                String passwd = editViewPasswd.getText().toString();
                String user = editViewUser.getText().toString();
                String directory = editTextDir.getText().toString();
                String port = editTextPort.getText().toString();
                String maxVersionAuth = null;
                String address;

                boolean connectionOK = false;

                if (!isOK) {
                    HttpConn connection;
                    String json;
                    JSONObject jsonObject;
                    String sid = null;

                    isOK = true;

                    if (chboxSecure.isChecked()) {
                        address = MainActivity.ProtocolHTTPS;
                    } else {
                        address = MainActivity.ProtocolHTTP;
                    }
                    address = address + ip + ":" + port + MainActivity.RootAPI;

                    //Check connection to Synology
                    try {
                        connection = new HttpConn(address + getAPIs);
                        json = new String(connection.finish());
                        jsonObject = new JSONObject(json);
                        maxVersionAuth = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.API.Auth")).getString("maxVersion");
                        checkPermissoinVersion = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.FileStation.CheckPermission")).getString("maxVersion");
                    } catch (Exception e) {
                        result = "Server not reached : " + e.toString();
                        isOK = false;
                    }

                    if (isOK) {
                        // Login to Synology
                        authAPI = authAPI.replace("USER", user);
                        authAPI = authAPI.replace("PASSWORD", passwd);
                        authAPI = authAPI.replace("VERSION", maxVersionAuth);
                        try {
                            connection = new HttpConn(address + authAPI);
                            json = new String(connection.finish());
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
                        checkPermissoinAPI = checkPermissoinAPI.replace("VRSN", checkPermissoinVersion);
                        checkPermissoinAPI = checkPermissoinAPI.replace("DIRECTORY", "/" + directory);
                        checkPermissoinAPI = checkPermissoinAPI.replace("SID", sid);
                        try {
                            connection = new HttpConn(address + checkPermissoinAPI);
                            json = new String(connection.finish());
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
                    authAPILogout = authAPILogout.replace("VERSION", maxVersionAuth);
                    authAPILogout = authAPILogout.replace("SID", sid);

                        try {
                            connection = new HttpConn(address + authAPILogout);
                            json = new String(connection.finish());
                            jsonObject = new JSONObject(json);
                        }
                        catch (Exception e){
                            //TODO : do something with exceptions
                            }
                    }

                    // Show result to the screen
                    Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG);
                    toast.show();
                }
                else {
                    SharedPreferencesManager.write(getString(R.string.address), ip);
                    SharedPreferencesManager.write(getString(R.string.passwd), passwd);
                    SharedPreferencesManager.write(getString(R.string.user), user);
                    SharedPreferencesManager.write(getString(R.string.settingViewDirectory), directory);
                    SharedPreferencesManager.write(getString(R.string.port), port);
                    SharedPreferencesManager.writeBoolean(getString(R.string.chckBoxUseHttps), chboxSecure.isChecked());
                    SharedPreferencesManager.writeBoolean(getString(R.string.chckBoxDelete), checkBoxDelete.isChecked());
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
                return super.onKeyDown(keyCode, event);
        }
    }

    protected boolean onDeleteKeyUp() {
        onBackPressed();
        return true;
    }
}
