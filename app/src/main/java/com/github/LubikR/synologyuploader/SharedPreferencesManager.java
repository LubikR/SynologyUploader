package com.github.LubikR.synologyuploader;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {

    private static SharedPreferences sharedPrefs;

    private SharedPreferencesManager() {}

    public static void init(Context context) {
        if (sharedPrefs == null)
            sharedPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static String read(String key, String defaultValue) {
        return sharedPrefs.getString(key, defaultValue);
    }

    public static boolean readBoolean(String key, Boolean defaultValue) {
        return sharedPrefs.getBoolean(key, defaultValue);
    }

    public static void write(String key, String value) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void writeBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void deleteAll() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.commit();
    }

    public static void deleteBoolean(String key) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(key);
        editor.commit();
    }
}
