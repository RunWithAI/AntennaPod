package de.danoeh.antennapod.core.util;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class HttpGet {
    protected static final int SOCKET_TIMEOUT = 10000; // 10S
    protected static final String GET = "GET";
    protected static final String POST = "POST";

    public static String post(String endUrl, JSONObject dataObj){

        try {
            // 设置SSLContext
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[] { myX509TrustManager }, null);

            URL url = new URL(endUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(sslcontext.getSocketFactory());
            }

            conn.setDoOutput(true);
            conn.setRequestMethod(POST);
// Set Headers (if any)
            conn.setRequestProperty("Content-Type", "application/json");
// Write Body
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(dataObj.toString());
            writer.flush();
            writer.close();
            os.close();
// Get the Response
            int responseCode = conn.getResponseCode();
            InputStream inputStream;

            if (200 <= responseCode && responseCode <= 299) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                sb.append(line);
            }

            in.close();
            conn.disconnect();
            return sb.toString();

        }catch (Exception e){
            return null;
        }
    }

    public static String get(String host, Map<String, String> params) {
        try {
            // 设置SSLContext
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[] { myX509TrustManager }, null);

            String sendUrl = getUrlWithQueryString(host, params);

            // System.out.println("URL:" + sendUrl);

            URL uri = new URL(sendUrl); // 创建URL对象
            HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(sslcontext.getSocketFactory());
            }

            conn.setConnectTimeout(SOCKET_TIMEOUT); // 设置相应超时
            conn.setRequestMethod(GET);
            int statusCode = conn.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                System.out.println("Http错误码：" + statusCode);
                return "Http错误码：" + statusCode;
            }

            // 读取服务器的数据
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }

            String text = builder.toString();

            close(br); // 关闭数据流
            close(is); // 关闭数据流
            conn.disconnect(); // 断开连接

            return text;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "MalformedURL异常";
        } catch (IOException e) {
            e.printStackTrace();
            return "IO异常";
        } catch (KeyManagementException e) {
            e.printStackTrace();
            return "KeyManagement异常";
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "Algorithm异常";
        }
    }

    public static String download(Context context, String downloadUrl, int expectedLength, String targetDir, String fileName, Handler handler){
        try {

            int totalLength = 0;
            int downloadedLength = 0;
            // 设置SSLContext
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[] { myX509TrustManager }, null);

            URL uri = new URL(downloadUrl); // 创建URL对象
            HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(sslcontext.getSocketFactory());
            }

            conn.setConnectTimeout(SOCKET_TIMEOUT); // 设置相应超时
            conn.setRequestMethod(GET);

            conn.connect();
            totalLength = conn.getContentLength();
            Log.e("UpdateApp", "download begin, got  totalLength " + totalLength);

            if(totalLength == 0){
                totalLength = expectedLength;
            }

            int statusCode = conn.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                Log.e("HttpGet", "download failed with http error code " + statusCode);
                return null;
            }

            File directory = new File(context.getFilesDir(), targetDir);
            if (!directory.exists()) {
                if (!directory.mkdir()) {
                    Log.e("HttpGet", "download failed to create apk download directory");
                    return null;
                }
            }

            File outputFile = new File(directory, fileName);
            if (outputFile.exists()) {
                outputFile.delete();
            }

            // 读取服务器的数据
            InputStream is = conn.getInputStream();
            OutputStream os = new FileOutputStream(outputFile);

            byte[] buffer = new byte[8192];
            int len;
            int previousRatio = 0;
            while ((len = is.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    is.close();
                    os.close();
                    throw new InterruptedException("Task interrupted!");
                }
                os.write(buffer, 0, len);
                downloadedLength = downloadedLength + len;
                if(totalLength != 0){
                    int ratio = (int)(downloadedLength * 1.0/totalLength * 100);
                    if (ratio - previousRatio > 1) {
                        previousRatio = ratio;
                        handler.sendEmptyMessage(ratio);
                        Log.d("UpdateApp", "Http download content ratio " + ratio + ", downloaded: " + downloadedLength + ", totalLength " + totalLength);
                    }
                }
            }
            os.close();
            is.close();
            handler.sendEmptyMessage(100);
            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getUrlWithQueryString(String url, Map<String, String> params) {
        if (params == null) {
            return url;
        }

        StringBuilder builder = new StringBuilder(url);
        if (url.contains("?")) {
            builder.append("&");
        } else {
            builder.append("?");
        }

        int i = 0;
        for (String key : params.keySet()) {
            String value = params.get(key);
            if (value == null) { // 过滤空的key
                continue;
            }

            if (i != 0) {
                builder.append('&');
            }

            builder.append(key);
            builder.append('=');
            builder.append(encode(value));

            i++;
        }

        return builder.toString();
    }

    protected static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 对输入的字符串进行URL编码, 即转换为%20这种形式
     *
     * @param input 原文
     * @return URL编码. 如果编码失败, 则返回原文
     */
    public static String encode(String input) {
        if (input == null) {
            return "";
        }

        try {
            return URLEncoder.encode(input, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return input;
    }

    @SuppressLint("CustomX509TrustManager")
    private static TrustManager myX509TrustManager = new X509TrustManager() {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    };
}

