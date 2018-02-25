package com.github.LubikR.synologyuploader;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Http {
    public static class Response implements Closeable {
        private HttpURLConnection connection;

        public Response (HttpURLConnection connection){
            this.connection = connection;
        }

        public byte[] getResponseBytes() throws IOException {
            return getResponseinBytes();
        }

        private byte[] getResponseinBytes() throws  IOException {
            InputStream is = connection.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1)
                os.write(buffer, 0, n);
            is.close();
            return os.toByteArray();
        }

        @Override
        public void close() {
        }
    }

    public static Response get(String url) throws IOException {
        return toResponse(createConnection(url));
    }

    public static Response get(String url, String sid) throws IOException {
        return toResponse(createConnection(url));
    }

    private static Response toResponse(HttpURLConnection connection) throws IOException {
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP Error " + connection.getResponseCode() + " (" + connection.getResponseMessage() + ")");
        }
        return new Response(connection);
    }

    private static HttpURLConnection createConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        return connection;
    }

    private static  HttpURLConnection createConnection(String url, String sid) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        String sidCookie = "id=" + sid;
        connection.setRequestProperty("Cookie", sidCookie);
        return connection;
    }
}
