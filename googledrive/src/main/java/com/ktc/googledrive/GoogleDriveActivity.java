package com.ktc.googledrive;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.exoplayer.ImageActivity;
import com.example.exoplayer.PlayerActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.ktc.googledrive.dao.GAccount;
import com.ktc.share.SpUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;

public class GoogleDriveActivity extends AppCompatActivity implements View.OnClickListener{

    private final String CLIENT_ID = "75554621124-rkllbu7ls907j8tasbmdq8m3or17roq4.apps.googleusercontent.com";
    private final String OAUTH_URL = "https://accounts.google.com/o/oauth2/auth";
    private final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private final String REDIRECT_URI = "com.jrm.localmm:/oauth2callback";
    private final String USED_INTENT="USED_INTENT";
    private final String SCOPES="https://www.googleapis.com/auth/drive" + " https://www.googleapis.com/auth/userinfo.profile";   //中间空格分隔


    private GoogleAuthenticationHelper googleHelper;
    private String accessToken;
    private TextView tvPath;
    private ProgressBar pb;
    private TextView tvEmpty;
    private FrameLayout emptyView;
    private GridView gridView;
    private List<File> items = new ArrayList<>();
    private FileAdapter adapter;
    private Activity activity;
    private List<String> records = new ArrayList<>();     //用户返回判断
    private File downloadItem;
    private final String ONE_DRIVE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/GoogleDrive";
    private int REQUEST_WRITE_EXTERNAL_STORAGE = 100;

    private String sub;
    private DriveHelper driveHelper = DriveHelper.getInstance();

    public static void navToGoogleDrive(Context context) {
        Intent intent = new Intent(context, GoogleDriveActivity.class);
        context.startActivity(intent);
    }

    public static void navToGoogleDrive(Context context, String sub) {
        Intent intent = new Intent(context, GoogleDriveActivity.class);
        intent.putExtra("sub", sub);
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_cloud_files);
        activity=this;
        initView();
        init();

    }

    private void initView() {
        tvPath = findViewById(R.id.tv_path);
        tvPath.setText("GoogleDrive");
        pb = findViewById(R.id.pb);
        tvEmpty = findViewById(R.id.tv_empty);
        emptyView = findViewById(R.id.emptyView);
        gridView = findViewById(R.id.gv_file);
        gridView.setEmptyView(emptyView);
        adapter=new FileAdapter(activity,items);
        gridView.setAdapter(adapter);
        adapter.setOnFileListener(listener);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File item = items.get(position);
                if (item.getMimeType().equals("application/vnd.google-apps.folder")) {
                    items.clear();
                    setEmptyView(true);
                    String query = "'" + item.getId() + "' in parents";
                    list(query);
                    records.add(item.getId());
                } else {
                    String url="https://www.googleapis.com/drive/v3/files/"+item.getId()+"?alt=media";
                    String mimeType=item.getMimeType();
                    if (mimeType.startsWith("image/")) {
                        ImageActivity.navToImageActivity(activity,url,accessToken);
                    } else if (mimeType.startsWith("audio/")) {
                        PlayerActivity.navToPlayer(activity,url,accessToken,item.getName());
                    } else if (mimeType.startsWith("video/")) {
                        PlayerActivity.navToPlayer(activity,url,accessToken);
                    } else{
                        Toast.makeText(activity, getString(R.string.preview_is_not_supported), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private ProgressDialog dialog;

    private class CustomProgressDownloadListener implements MediaHttpDownloaderProgressListener {
        public CustomProgressDownloadListener() {
            dialog = createProgressBar("正在下载");
            dialog.show();
        }

        @Override
        public void progressChanged(final MediaHttpDownloader downloader) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("hml", "status=" + downloader.getDownloadState() + " progress=" + downloader.getProgress());
                    final int value = (int) (downloader.getProgress() * 100);
                    switch (downloader.getDownloadState()) {
                        case MEDIA_IN_PROGRESS:
                            dialog.setProgress(value);
                            dialog.setTitle("正在下载");
                            break;
                        case MEDIA_COMPLETE:
                            dialog.setProgress(value);
                            dialog.setTitle("下载完成!可在本地GoogleDrive目录中查看");
                            downloadItem=null;
                            break;
                    }
                }
            });

        }


    }


    private FileOutputStream getFileOutputStream(String fileName) {
        FileOutputStream fos = null;
        java.io.File dir = new java.io.File(ONE_DRIVE_SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        java.io.File target = new java.io.File(dir, fileName);
        if (target.exists()) {
            target.delete();
        }
        try {
            target.createNewFile();
            fos = new FileOutputStream(target);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            return fos;
        }
    }

    private void setEmptyView(boolean reset){
        adapter.notifyDataSetChanged();
        if (reset){
            tvEmpty.setVisibility(View.GONE);
            pb.setVisibility(View.VISIBLE);
        }else {
            tvEmpty.setVisibility(View.VISIBLE);
            pb.setVisibility(View.GONE);
        }

    }

    private void init() {
        sub = getIntent().getStringExtra("sub");
        googleHelper = GoogleAuthenticationHelper.getInstance(this);
        if (TextUtils.isEmpty(sub)) {
            signIn();
        } else {
            refreshToken();
        }
    }

    private void signIn(){
        AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                Uri.parse(OAUTH_URL) /* auth endpoint */,
                Uri.parse(TOKEN_URL) /* token endpoint */
        );
        AuthorizationService authorizationService = new AuthorizationService(activity);
        Uri redirectUri = Uri.parse(REDIRECT_URI);
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                serviceConfiguration,
                CLIENT_ID,
                AuthorizationRequest.RESPONSE_TYPE_CODE,
                redirectUri
        );
        builder.setScopes(SCOPES);
        AuthorizationRequest request = builder.build();
        String action = "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE";
        Intent postAuthorizationIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(activity, request.hashCode(), postAuthorizationIntent, 0);
        authorizationService.performAuthorizationRequest(request, pendingIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)){
                return;
            }
            switch (action) {
                case "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE":
                    if (!intent.hasExtra(USED_INTENT)) {
                        ActivityManager am= (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                        am.killBackgroundProcesses("com.android.browser");
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

    private void handleAuthorizationResponse(@NonNull Intent intent) {
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);
        if (response != null) {
            AuthorizationService service = new AuthorizationService(this);
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                    } else {
                        if (tokenResponse != null) {
                            String uploadFilePath=SpUtil.getUploadFilePath();
                            accessToken=tokenResponse.accessToken;
                            if (TextUtils.isEmpty(uploadFilePath)){
                                getRootFile(tokenResponse.accessToken);
                            }else {
                                uploadFile(tokenResponse.accessToken,uploadFilePath);
                            }
                            refreshUserInfo(tokenResponse.accessToken,tokenResponse.refreshToken);
                        }
                    }
                }
            });
        }else {
            Log.e("hml","error="+authState.toJsonString());
        }
    }


    private void uploadFile(String accessToken,String uploadFile){
        final ProgressDialog dialog=createProgressBar("准备上传");
        try {
            SpUtil.setUploadFilePath(null);
            driveHelper.uploadFile(accessToken, uploadFile, new com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener() {
                @Override
                public void progressChanged(final MediaHttpUploader uploader) throws IOException {
                    final int value= (int) (uploader.getProgress()*100);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (uploader.getUploadState()) {
                                case INITIATION_STARTED:
                                    dialog.show();
                                    dialog.setTitle("准备上传");
                                    break;
                                case MEDIA_IN_PROGRESS:
                                    dialog.setProgress(value);
                                    dialog.setTitle("正在上传");
                                    break;
                                case MEDIA_COMPLETE:
                                    dialog.setProgress(value);
                                    dialog.setTitle("上传完成");
                                    Toast.makeText(activity,getString(R.string.has_been_uploaded_to)+getString(R.string.google_drive),Toast.LENGTH_SHORT).show();
                                    refresh();
                                    break;
                            }
                        }
                    });


                }
            });
        } catch (IOException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity,"上传失败",Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });

            e.printStackTrace();
        }
    }

    private ProgressDialog createProgressBar(String title) {
        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setTitle(title);
        dialog.setMax(100);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return dialog;
    }

    private void getRootFile(String accessToken){
        driveHelper.queryRootFiles(accessToken).addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                String query = "'" + file.getId() + "' in parents";
                list(query);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("hml","e:"+e.toString());
            }
        });
    }

    private void refreshToken() {
        GAccount gAccount = googleHelper.getAccountById(sub);
        if (gAccount == null) {
            return;
        }
        String refreshToken = gAccount.refreshToken;
        OkHttpUtils
                .post()
                .url(TOKEN_URL)
                .addParams("refresh_token", refreshToken)
                .addParams("client_id", CLIENT_ID)
                .addParams("grant_type", "refresh_token")
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.e("hml", "e=" + e.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity,"自动登录失败",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Gson gson = new Gson();
                        TokenInfo tokenInfo = gson.fromJson(response, TokenInfo.class);
                        accessToken=tokenInfo.access_token;
                        String uploadFilePath=SpUtil.getUploadFilePath();
                        if (TextUtils.isEmpty(uploadFilePath)){
                            getRootFile(tokenInfo.access_token);
                        }else {
                            uploadFile(tokenInfo.access_token,uploadFilePath);
                        }
                    }
                });
    }

    private void list(String query) {
        try {
            String fields="files(hasThumbnail,id,imageMediaMetadata(height,width),mimeType,modifiedTime,name,size,thumbnailLink,videoMediaMetadata/durationMillis)";
            driveHelper.listFiles(query, fields, true)
                    .addOnSuccessListener(new OnSuccessListener<FileList>() {
                        @Override
                        public void onSuccess(FileList fileList) {
                            items.clear();
                            items.addAll(fileList.getFiles());
                            Collections.sort(items,new ComparatorFile());
                            //adapter.notifyDataSetChanged();
                            adapter=new FileAdapter(activity,items);
                            gridView.setAdapter(adapter);
                            adapter.setOnFileListener(listener);
                            setEmptyView(false);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("hml","e="+e.toString());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshUserInfo(String accessToken, final String refreshToken) {
        OkHttpUtils
                .post()
                .url("https://www.googleapis.com/oauth2/v3/userinfo")
                .addHeader("Authorization", "Bearer" + accessToken)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.e("hml", "e=" + e.toString());
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Gson gson = new Gson();
                        UserInfo userInfo = gson.fromJson(response, UserInfo.class);
                        GAccount account = new GAccount(userInfo.sub, refreshToken, userInfo.name, userInfo.picture);
                        googleHelper.saveAccount(account);
                    }
                });
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onBackPressed() {
        if (records.size() > 1) {
            records.remove(records.size() - 1);
            String itemId = records.get(records.size() - 1);
            String query = "'" + itemId + "' in parents";
            list(query);
        } else if (records.size() > 0 && records.size() <= 1) {
            records.remove(records.size() - 1);
            getRootFile(null);
        } else {
            super.onBackPressed();
        }

    }

    public void refresh() {
        setEmptyView(true);
        if (records.size()>0){
            String itemId=records.get(records.size()-1);
            String query = "'" + itemId + "' in parents";
            list(query);
        }else {
            getRootFile(null);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (downloadItem!=null){
                    Toast.makeText(activity, downloadItem.getName() + "准备下载", Toast.LENGTH_SHORT).show();
                    driveHelper.downloadFile(downloadItem.getId(), getFileOutputStream(downloadItem.getId()), new CustomProgressDownloadListener());

                }
                }
        }
    }

    private FileAdapter.OnFileListener listener=new FileAdapter.OnFileListener() {
        @Override
        public void onDelete(int position) {
            String itemId = items.get(position).getId();
            driveHelper.deleteFile(itemId).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("hml","onSuccess");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refresh();
                            Toast.makeText(activity,getString(R.string.deleted_successfully),Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            });
        }

        @Override
        public void onDownload(int position) {
            downloadItem = items.get(position);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission
                        .WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(activity, "请开通相关权限，否则无法正常使用本应用", Toast.LENGTH_SHORT).show();
                }
                //申请权限
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                if (downloadItem!=null){
                    Toast.makeText(activity, downloadItem.getName() + "准备下载", Toast.LENGTH_SHORT).show();
                    driveHelper.downloadFile(downloadItem.getId(), getFileOutputStream(downloadItem.getName()), new CustomProgressDownloadListener());
                }
            }
        }
    };
}
