package com.ktc.share;

import android.app.ActivityManager;
import android.content.Context;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import com.ktc.share.User;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ReflectUtil {

    public static List<User> getUsers(Context context) {
        List<User> users=new ArrayList<>();
        List<Object> objects=new ArrayList<>();
        UserManager userManager= (UserManager) context.getSystemService(Context.USER_SERVICE);
        try {
            Method method = UserManager.class.getDeclaredMethod("getUsers");
            method.setAccessible(true);
            objects.clear();
            objects.addAll((Collection<?>) method.invoke(userManager));

        } catch (Exception e) {
            Log.e("hml","e="+e);
            e.printStackTrace();
        } finally {
            users.clear();
            for (Object object:objects){
                User user=new User();
                user.name=getUserName(object);
                user.id=getUserId(object);
                users.add(user);
            }
            return users;
        }

    }

    public static int getCurrentUser(Context context){
        int userId=0;
        try {
            Method method=ActivityManager.class.getDeclaredMethod("getCurrentUser");
            method.setAccessible(true);
            userId= (int) method.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            return userId;
        }
    }

    private static String getUserName(Object object) {
        String name = "";
        Class userInfo = object.getClass();
        try {
            Field field = userInfo.getDeclaredField("name");
            name = (String) field.get(object);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return name;
        }
    }

    private static int getUserId(Object object){
        int id =-1;
        Class userInfo = object.getClass();
        try {
            Field field = userInfo.getDeclaredField("id");
            id = (int) field.get(object);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return id;
        }
    }

    public static StorageVolume getStorageVolume(StorageManager storageManager, File file, int userId){
        StorageVolume storageVolume=null;
        if (storageManager==null||file==null){
            return storageVolume;
        }
        try {
            Method method=StorageManager.class.getDeclaredMethod("getStorageVolume",File.class,int.class);
            method.setAccessible(true);
            storageVolume= (StorageVolume) method.invoke(storageManager,file,userId);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            return storageVolume;
        }
    }


    public static final int FLAG_REAL_STATE = 1 << 9;
    public static final int FLAG_INCLUDE_INVISIBLE = 1 << 10;

    public static List<StorageVolume> getStorageVolumes(StorageManager storageManager,int userId){
        final ArrayList<StorageVolume> res = new ArrayList<>();
        try {
            Method method=StorageManager.class.getDeclaredMethod("getVolumeList",int.class,int.class);
            method.setAccessible(true);
            StorageVolume[] storageVolumes= (StorageVolume[]) method.invoke(storageManager,userId,FLAG_REAL_STATE | FLAG_INCLUDE_INVISIBLE);
            Collections.addAll(res,storageVolumes);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            return res;
        }
    }

    public static void forceStopPackage(ActivityManager am, String packName){
        try {
            Method method=ActivityManager.class.getMethod("forceStopPackage",String.class);
            method.setAccessible(true);
            method.invoke(am,packName);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
