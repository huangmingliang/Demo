package com.ktc.googledrive;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveHelper {
    private static DriveHelper INSTANCE;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private String accessToken;
    private Drive drive;
    private GoogleNetHttpTransport httpTransport;


    public DriveHelper() {
        initDrive();
    }

    public static DriveHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DriveHelper();
        }
        return INSTANCE;
    }


    private void initDrive() {
        HttpTransport ht = AndroidHttp.newCompatibleTransport();             // Makes a transport compatible with both Android 2.2- and 2.3+
        JacksonFactory jf = new JacksonFactory();                            // You need a JSON parser to help you out with the API response
        Drive.Builder b = new Drive.Builder(ht, jf, null);
        b.setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
            @Override
            public void initialize(AbstractGoogleClientRequest<?> request) throws IOException {
                DriveRequest driveRequest = (DriveRequest) request;
                driveRequest.setPrettyPrint(true);
//                driveRequest.setKey(getString(R.string.google_server_client_id));
                driveRequest.setOauthToken(accessToken);
            }
        });
        drive = b.build();
    }

    public Task<File> queryRootFiles(String accessToken) {//查询根文件
        if (!TextUtils.isEmpty(accessToken)){
            this.accessToken=accessToken;
        }
        return Tasks.call(mExecutor, new Callable<File>() {
            @Override
            public File call() throws Exception {
                return drive.files().get("root").setFields("id").execute();
            }
        });
    }

    public Task<FileList> listFiles(String query, String fields, boolean includeTrashed) throws IOException{//查询特定文件集合
        final Drive.Files.List list = drive.files().list();
        if(fields != null) {
            list.setFields(fields);
        }
        if(query != null) {
            list.setQ(query);
        }
        if(!includeTrashed) {
            list.setQ(query + (query != null ? " AND" : "") + " trashed = false");
        }
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                return list.execute();
            }
        });
    }

    public Task<Void> downloadFile(final String id, final OutputStream outputStream, final MediaHttpDownloaderProgressListener listener) {//下载文件
        if (TextUtils.isEmpty(id)){
            return null;
        }
        if (outputStream==null){
            return null;
        }
        return Tasks.call(mExecutor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                Drive.Files.Get get = drive.files().get(id);
                get.getMediaHttpDownloader().setProgressListener(listener);
                get.executeMediaAndDownloadTo(outputStream);
                return null;
            }
        });

    }

    public Task<Void> deleteFile(final String fileId) {//根据fileId来删除文件
        return Tasks.call(mExecutor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                return drive.files().delete(fileId).execute();
            }
        });
    }


}
