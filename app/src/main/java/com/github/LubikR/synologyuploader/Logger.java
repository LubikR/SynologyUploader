package com.github.LubikR.synologyuploader;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {

    private static File getFile() {
        return new File(Environment.getExternalStorageDirectory(), "synoupld.log");
    }

    private static void log (String type, String activity, String msg) {
        try {
            getFile().getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(), true));
            writer.append("[" + type + "][" + activity + "] " + msg);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            //TODO : Do something with exception
            Log.d("Logger", "log: " + e.getMessage());
        }
    }

    public static void info(String activity, String msg) {
        log ("INFO", activity, msg);
    }

    public static void error(String activity, String msg) {
        log("ERROR", activity, msg);
    }
}
