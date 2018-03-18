package com.github.LubikR.synologyuploader;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class MultiPartUpload {

    private final String DELIMITER_SHORT = UUID.randomUUID().toString();
    private final String DELIMITER = "--" + DELIMITER_SHORT;
    private final String CRLF = "\r\n";
    private HttpURLConnection connection;
    private OutputStream outputStream;
    private PrintWriter writer;
    String charset;
    Intent intent = new Intent();

    public MultiPartUpload (String requestedUrl, String charset, String sid) throws IOException{
        this.charset = charset;

        URL url = new URL(requestedUrl);
        connection = (HttpURLConnection) url.openConnection();
        String sidCookie = "id=" + sid;
        connection.setRequestProperty("Cookie", sidCookie);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        // 1MB as fixed chunk size
        connection.setRequestProperty("Content-type", "multipart/form-data, boundary=" + DELIMITER_SHORT);
        connection.setChunkedStreamingMode(1024 * 1024);
        connection.setRequestProperty("Transfer-Encoding","chunked");

        outputStream = connection.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);

        intent.setAction("com.github.LubikR.synologyuploader.PROGRESS_BAR_NOTIFICATION");
    }

    public void addFormField (String name, String value) {
        writer.append(DELIMITER)
                .append(CRLF)
                .append("content-disposition: form-data; name=\"" + name + "\"")
                .append(CRLF)
                .append(CRLF)
                .append(value)
                .append(CRLF);
        writer.flush();
    }

    public void addFilePart (String fieldName, FileInputStream uploadFileStream, String fileName, Context context) throws IOException {

        writer.append(DELIMITER).append(CRLF)
                .append("content-disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"")
                .append(CRLF)
                .append("Content-Type: application/octet-stream")
                .append(CRLF)
                .append(CRLF);
        writer.flush();

        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        int i = 1;
        while ((bytesRead = uploadFileStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);

            i++;
            intent.putExtra("data", i);
            context.sendBroadcast(intent);
        }
        outputStream.flush();
        uploadFileStream.close();

        writer.append(CRLF)
                .append(DELIMITER)
                .append("--")
                .append(CRLF);
        writer.flush();
    }

    public byte[] finish() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        writer.close();
        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();

            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1)
                os.write(buffer, 0, n);
            is.close();
            connection.disconnect();
        }
        else {
            throw new IOException("Server returned non-OK status: " + status);
        }
        return os.toByteArray();
    }
}
