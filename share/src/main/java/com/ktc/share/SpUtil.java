package com.ktc.share;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtil {
    private static SpUtil spUtil;
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    public SpUtil(Context context) {
      preferences=context.getSharedPreferences("ktc_config",Context.MODE_PRIVATE);
      editor=preferences.edit();
    }

    public static void getInstance(Context context){
        if (spUtil==null){
            spUtil=new SpUtil(context);
        }
    }

    public static void setSelectedUser(int userId){
        editor.putInt("userId",userId);
        editor.commit();
    }

    public static int getSelectedUser(){
        return preferences.getInt("userId",0);
    }

    public static void setSelectedUsbDevice(String usbDevice){
        editor.putString("usbDevice",usbDevice);
        editor.commit();
    }

    public static String getSelectedUsbDevice(){
        return preferences.getString("usbDevice","");
    }

    public static void setUploadFilePath(String filePath){
        editor.putString("filePath",filePath);
        editor.commit();
    }

    public static String getUploadFilePath(){
        return preferences.getString("filePath",null);
    }
}
