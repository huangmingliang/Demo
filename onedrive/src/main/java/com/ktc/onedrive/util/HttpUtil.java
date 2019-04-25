package com.ktc.onedrive.util;

import android.app.ProgressDialog;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {

    public static void upload(File file, String uploadUrl) {
        try {
            URL url = new URL(uploadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("PUT");
            conn.setConnectTimeout(50000);
            conn.connect();
            FileInputStream fis=new FileInputStream(file);
            OutputStream out = new DataOutputStream(conn.getOutputStream());
            DataInputStream in = new DataInputStream(fis);
            int bytes;
            byte[] bufferOut = new byte[2048];
            while ((bytes = in.read(bufferOut)) != -1) {
                out.write(bufferOut, 0, bytes);
            }
            in.close();
            out.flush();
            out.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.e("hml","line="+line);
            }
        } catch (Exception e) {
            Log.e("hml","e="+e);
            e.printStackTrace();
        }
    }
}
