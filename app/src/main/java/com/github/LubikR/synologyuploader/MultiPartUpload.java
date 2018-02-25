package com.github.LubikR.synologyuploader;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MultiPartUpload {
    private static final String DELIMITER = "--AaB03x";
    private static final String DEL = "AaB03x";
    private static final String CRLF = "\r\n";
    private HttpURLConnection connection;
    private OutputStream outputStream;
    private PrintWriter writer;
    String charset;


    public MultiPartUpload (String requestedUrl, String charset, String sid) throws IOException{
        this.charset = charset;

        URL url = new URL(requestedUrl);
        connection = (HttpURLConnection) url.openConnection();
        String sidCookie = "id=" + sid;
        connection.setRequestProperty("Cookie", sidCookie);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-type", "multipart/form-data, boundary=" + DEL);
        outputStream = connection.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
    }

    public void addFormField (String name, String value) {
        writer.append(DELIMITER).append(CRLF);
        writer.append("content-disposition: form-data; name=\"" + name + "\"")
                .append(CRLF).append(CRLF);
        writer.append(value).append(CRLF);
        writer.flush();
    }

    public void addFilePart (String fieldName, FileInputStream uploadFileStream, String fileName) throws IOException {
        //String fileName = uploadFile.getName();
        writer.append(DELIMITER).append(CRLF);
        writer.append("content-disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"")
                .append(CRLF);
        writer.append("Content-Type: application/octet-stream").append(CRLF).append(CRLF);
        writer.flush();

        //FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = uploadFileStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        //inputStream.close();
        uploadFileStream.close();

        writer.append(CRLF).append(DELIMITER).append("--").append(CRLF);
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
