package com.github.LubikR.synologyuploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class HttpConn {
    private HttpURLConnection connection;
    private URL url;
    private InputStream is;


    public HttpConn(String requestedUrl) throws IOException {
        url = new URL(requestedUrl);

        if (url.getProtocol().toLowerCase().equals("https")) {
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setHostnameVerifier(DO_NOT_VERIFY);
            connection = conn;
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
    }

    private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    };

    public byte[] finish() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1)
                os.write(buffer, 0, n);
            is.close();
            connection.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }
        return os.toByteArray();
    }

}