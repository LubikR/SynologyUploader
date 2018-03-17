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

public class SettingsActivity extends BaseActivity {

    private final String TAG = "SettingsActivity";
    private boolean isOK = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        final EditText serverEditView = (EditText) findViewById(R.id.server);
        final EditText userEditView = (EditText) findViewById(R.id.user);
        final EditText passwdEditView = (EditText) findViewById(R.id.passwd);
        final EditText dirEditText = (EditText) findViewById(R.id.directory);
        final EditText portEditText = (EditText) findViewById(R.id.port);
        final Button saveButton = (Button) findViewById(R.id.buttonSave);
        final CheckBox secureCheckbox = (CheckBox) findViewById(R.id.checkBox_Secure);
        final CheckBox deleteCheckBox = (CheckBox) findViewById(R.id.checkBox_Delete);
        final CheckBox debugLevelCheckBox = (CheckBox) findViewById(R.id.checkBox_Log);

        //load preferences from sharedPreferences to View
        serverEditView.setText(SharedPreferencesManager.read(getString(R.string.address), null));
        userEditView.setText(SharedPreferencesManager.read(getString(R.string.user), null));
        passwdEditView.setText(SharedPreferencesManager.read(getString(R.string.passwd), null));
        dirEditText.setText(SharedPreferencesManager.read(getString(R.string.settingViewDirectory), null));
        portEditText.setText(SharedPreferencesManager.read(getString(R.string.port), null));
        secureCheckbox.setChecked(SharedPreferencesManager.readBoolean(getString(R.string.chckBoxUseHttps), false));
        deleteCheckBox.setChecked(SharedPreferencesManager.readBoolean(getString(R.string.chckBoxDelete), false));
        debugLevelCheckBox.setChecked(SharedPreferencesManager.readBoolean(getString(R.string.chkkBoxLog), false));

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String result = getString(R.string.connectionCheckOK);
                String ip = serverEditView.getText().toString();
                String passwd = passwdEditView.getText().toString();
                String user = userEditView.getText().toString();
                String directory = dirEditText.getText().toString();
                String port = portEditText.getText().toString();
                String maxVersionAuth = null;
                String checkPermissoinVersion = null;
                String address;

                boolean connectionOK = false;

                if (!isOK) {
                    JSONObject jsonObject;
                    String sid = null;

                    isOK = true;

                    //Get URL
                    if (debugLevelCheckBox.isChecked()) { Logger.info(TAG, "Getting address : " +
                            ip.replaceAll(".","*") + ":" + port + " https:" +
                                    secureCheckbox.isChecked()); };
                    address = SynologyAPI.getAddress(ip, port, secureCheckbox.isChecked());

                    //Check connection to Synology
                    try {
                        jsonObject = SynologyAPI.CheckAPIAndRetrieveMaxVersions(address);
                        maxVersionAuth = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.API.Auth")).getString("maxVersion");
                        checkPermissoinVersion = ((jsonObject.getJSONObject("data")).getJSONObject("SYNO.FileStation.CheckPermission")).getString("maxVersion");
                        if (debugLevelCheckBox.isChecked()) { Logger.info(TAG, "Got maxVersions: SYNO.API.Auth=" + maxVersionAuth +
                            ", SYNO.FileStation.CheckPermission="+ checkPermissoinVersion); };
                    } catch (Exception e) {
                        result = "Server not reached : " + e.toString();
                        if (debugLevelCheckBox.isChecked()) { Logger.error(TAG, "Problem during MaxVersions: " + e.toString()); };
                        isOK = false;
                    }

                    if (isOK) {
                        // Login to Synology
                        try {
                            jsonObject = SynologyAPI.login(address, user, passwd, maxVersionAuth);
                            if ((jsonObject.getString("success")).equals("true")) {
                                sid = jsonObject.getJSONObject("data").getString("sid");
                                connectionOK = true;
                                if (debugLevelCheckBox.isChecked()) { Logger.info(TAG, "Logged-in OK, sid=" + sid); };
                            } else throw new JSONException(jsonObject.getJSONObject("error").getString("code"));
                        } catch (Exception e) {
                            result = "Login failed : Synology returns error " + e.getMessage();
                            if (debugLevelCheckBox.isChecked()) { Logger.error(TAG, "Problem during Login: " +
                                    e.getMessage()); };
                                    isOK = false;
                        }
                    }

                    //Check remote directory a permission
                    if (isOK){
                        try {
                            jsonObject = SynologyAPI.checkPermissionToDirectory(address, directory, sid, checkPermissoinVersion);
                            if ((jsonObject.getString("success")).equals("true")) {
                                if (debugLevelCheckBox.isChecked()) { Logger.info(TAG, "Check permission OK. Directory=" + directory); };
                            } else throw new JSONException(jsonObject.getJSONObject("error").getString("code"));
                        }
                        catch (Exception e) {
                            result = "Permission problem : Synology returns error " + e.getMessage();
                            if (debugLevelCheckBox.isChecked()) { Logger.error(TAG,"Problem during permission check: " +
                                    e.getMessage()); };
                            isOK = false;
                        }
                    }

                    if (connectionOK) {
                    //Logout
                        try {
                            jsonObject = SynologyAPI.logout(address, sid, maxVersionAuth);
                            if (debugLevelCheckBox.isChecked()) { Logger.info(TAG, "Logged-out"); };
                        }
                        catch (Exception e){
                            //TODO : do something with exceptions
                            if (debugLevelCheckBox.isChecked()) { Logger.error(TAG,"Problem during Loggining-out: " +
                                    e.getMessage()); };
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
                    SharedPreferencesManager.writeBoolean(getString(R.string.chckBoxUseHttps), secureCheckbox.isChecked());
                    SharedPreferencesManager.writeBoolean(getString(R.string.chckBoxDelete), deleteCheckBox.isChecked());
                    SharedPreferencesManager.writeBoolean(getString(R.string.chkkBoxLog), debugLevelCheckBox.isChecked());

                    if (debugLevelCheckBox.isChecked()) { Logger.info(TAG, "Saved and Go to MainActivity"); };
                    finish();
                }
                // If everything is OK change button text to "Save"
                if (isOK) saveButton.setText("Save and Exit");
            }
        });
    }
}
