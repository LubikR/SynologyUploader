package com.github.LubikR.synologyuploader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SynologyAPI {

    final static String ProtocolHTTP = "http://";
    final static String ProtocolHTTPS = "https://";
    final static String RootAPI = "/webapi/";
    private final static String authAPI = "auth.cgi?api=SYNO.API.Auth&version=VERSION&method=login&account=USER&passwd=PASSWORD&session=FileStation&format=cookie";
    private final static String getAPIs = "query.cgi?api=SYNO.API.Info&version=1&method=query&query=SYNO.API.Auth,SYNO.FileStation.Upload,SYNO.FileStation.CheckPermission";
    private final static String authAPILogout = "auth.cgi?api=SYNO.API.Auth&version=VERSION&method=logout&_sid=SID";
    private final static String checkPermissoinAPI = "entry.cgi?api=SYNO.FileStation.CheckPermission&version=VRSN&method=write&path=DIRECTORY&filename=test.tmp&_sid=SID";
    public final static String uploadAPI = "entry.cgi";

    private static HttpConn connection;

    public static String getAddress(String ip, String port, boolean https) {
        String address;

        if (https) {
            address = ProtocolHTTPS;
        } else {
            address = ProtocolHTTP;
        }
        return (address + ip + ":" + port + RootAPI);
    }

    public static JSONObject CheckAPIAndRetrieveMaxVersions(String address) throws IOException, JSONException {
        connection = new HttpConn(address + getAPIs);
        return (new JSONObject(new String(connection.finish())));
    }

    public static JSONObject login (String address, String user, String passwd, String maxVersionAuth) throws IOException, JSONException {
        String authAPIReplaced = authAPI.replace("USER", user);
        authAPIReplaced = authAPIReplaced.replace("PASSWORD", passwd);
        authAPIReplaced = authAPIReplaced.replace("VERSION", maxVersionAuth);
        connection = new HttpConn(address + authAPIReplaced);
        return (new JSONObject(new String(connection.finish())));
}

    public static JSONObject checkPermissionToDirectory(String address, String directory, String sid, String permisionVersion) throws IOException, JSONException {
        String checkPermissoinAPIReplaced = checkPermissoinAPI.replace("VRSN", permisionVersion);
        checkPermissoinAPIReplaced = checkPermissoinAPIReplaced.replace("DIRECTORY", "/" + directory);
        checkPermissoinAPIReplaced = checkPermissoinAPIReplaced.replace("SID", sid);

        connection = new HttpConn(address + checkPermissoinAPIReplaced);
        return (new JSONObject(new String(connection.finish())));
    }

    public static JSONObject logout(String address, String sid, String maxVersionAuth) throws IOException, JSONException {
        String authAPILogoutReplaced = authAPILogout.replace("VERSION", maxVersionAuth);
        authAPILogoutReplaced = authAPILogoutReplaced.replace("SID", sid);

        connection = new HttpConn(address + authAPILogoutReplaced);
        return (new JSONObject(new String(connection.finish())));
    }
}
