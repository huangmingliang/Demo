package com.ktc.onedrive;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ktc.onedrive.util.FileUtil;
import com.ktc.onedrive.util.HttpUtil;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.core.DefaultClientConfig;
import com.microsoft.graph.core.IClientConfig;
import com.microsoft.graph.extensions.DriveItem;
import com.microsoft.graph.extensions.DriveItemUploadableProperties;
import com.microsoft.graph.extensions.GraphServiceClient;
import com.microsoft.graph.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.extensions.IGraphServiceClient;
import com.microsoft.graph.extensions.Thumbnail;
import com.microsoft.graph.extensions.UploadSession;
import com.microsoft.graph.extensions.User;
import com.microsoft.graph.http.IHttpRequest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GraphHelper implements IAuthenticationProvider {
    private static GraphHelper INSTANCE = null;
    private IGraphServiceClient mClient;
    private String mAccessToken;

    IClientConfig mClientConfig = DefaultClientConfig
            .createWithAuthenticationProvider(this);

    private GraphHelper() {
        mClient = new GraphServiceClient
                .Builder()
                .fromConfig(mClientConfig)
                .buildClient();

    }

    public static synchronized GraphHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GraphHelper();
        }

        return INSTANCE;
    }

    // Part of the Graph IAuthenticationProvider interface
    // This method is called before sending the HTTP request
    @Override
    public void authenticateRequest(IHttpRequest request) {
        // Add the access token in the Authorization header
        request.addHeader("Authorization", "Bearer " + mAccessToken);
    }

    public void getUser(String accessToken, ICallback<User> callback) {
        mAccessToken = accessToken;
        mClient.getMe().buildRequest().get(callback);
    }


    public void getDriveRootChildren(String accessToken, ICallback<IDriveItemCollectionPage> callback) {
        if (!TextUtils.isEmpty(accessToken)) {
            mAccessToken = accessToken;
        }
        mClient.getMe().getDrive().getRoot().getChildren().buildRequest().get(callback);
    }

    public void getDriveItemById(ICallback<IDriveItemCollectionPage> callback, String itemId) {
        mClient.getMe().getDrive().getItems(itemId).getChildren().buildRequest().get(callback);
    }

    public void downloadDriveItemById(ICallback<InputStream> callback, String itemId) {
        mClient.getMe().getDrive().getItems(itemId).getContent().buildRequest().get(callback);
    }

    public void uploadDriveItemById(String accessToken, final File file, final ICallback<DriveItem> callback) {
        if (file == null || !file.exists()) {
            return;
        }
        mAccessToken = accessToken;
        final byte[] bytes = FileUtil.readFile(file);
        new Thread(new Runnable() {
            @Override
            public void run() {
                DriveItem item = mClient.getMe().getDrive().getRoot().buildRequest().get();
                final String itemId = item.id + ":/" + file.getName() + ":";
                mClient.getMe().getDrive().getItems(itemId).getContent().buildRequest().put(bytes, callback);
            }
        }).start();

    }

    public void uploadBigFileDriveItemById(String accessToken, final File file, final ICallback<UploadSession> callback) {
        if (file == null || !file.exists()) {
            return;
        }
        mAccessToken = accessToken;
        new Thread(new Runnable() {
            @Override
            public void run() {
                DriveItem item = mClient.getMe().getDrive().getRoot().buildRequest().get();
                final String itemId = item.id + ":/" + file.getName() + ":";
                DriveItemUploadableProperties properties = new DriveItemUploadableProperties();
                properties.name = file.getName();
                mClient.getMe().getDrive().getItems(itemId).getCreateUploadSession(properties).buildRequest().post(callback);
            }
        }).start();


    }

    public void deleteDriveItemById(String itemId,ICallback<Void> callback){
        mClient.getMe().getDrive().getItems(itemId).buildRequest().delete(callback);
    }


    public void getDriveItemThumbnail(String itemId, String size, ICallback<Thumbnail> callback){
        mClient.getMe().getDrive().getItems(itemId).getThumbnails("0").getThumbnailSize(size).buildRequest().get(callback);
    }


}
