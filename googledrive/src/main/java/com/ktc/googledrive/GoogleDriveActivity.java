package com.ktc.googledrive;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;

public class GoogleDriveActivity extends AppCompatActivity implements View.OnClickListener {

    private final String CLIENT_ID = "75554621124-e96rprvnhp0kne5ji83apb79esvna73j.apps.googleusercontent.com";
    private final String OAUTH_URL = "https://accounts.google.com/o/oauth2/auth";
    private final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private final String REDIRECT_URI = "com.ktc.demo:/oauth2callback";
    private final String USED_INTENT="USED_INTENT";
    private final String SCOPES="https://www.googleapis.com/auth/drive" + " https://www.googleapis.com/auth/userinfo.profile";   //中间空格分隔

    GoogleAuthenticationHelper googleHelper;
    private TextView tvPath;
    private ProgressBar pb;
    private TextView tvEmpty;
    private FrameLayout emptyView;
    private ListView lvFile;
    private List<File> items = new ArrayList<>();
    private FileAdapter adapter;
    private Activity activity;
    private List<String> records = new ArrayList<>();     //用户返回判断

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
        setContentView(R.layout.activity_google_drive);
        initView();
        init();

    }

    private void initView() {
        tvPath = findViewById(R.id.tv_path);
        pb = findViewById(R.id.pb);
        tvEmpty = findViewById(R.id.tv_empty);
        emptyView = findViewById(R.id.emptyView);
        lvFile = findViewById(R.id.lv_file);
        lvFile.setEmptyView(emptyView);
        adapter = new FileAdapter(this, items, getSupportFragmentManager());
        lvFile.setAdapter(adapter);
        lvFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
                    Toast.makeText(activity, "暂不支持预览", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        activity=this;
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
                            if (TextUtils.isEmpty(uploadFilePath)){
                                getRootFile(tokenResponse.accessToken);
                            }else {
                                //uploadFile(tokenResponse.accessToken,uploadFilePath);
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

    private void getRootFile(String accessToken){
        driveHelper.queryRootFiles(accessToken).addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                Log.d("hml","file:"+file.toString());
                String query = "'" + file.getId() + "' in parents";
                list(query);
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
                        String uploadFilePath= SpUtil.getUploadFilePath();
                        if (TextUtils.isEmpty(uploadFilePath)){
                            getRootFile(tokenInfo.access_token);
                        }else {
                            //uploadFile(tokenInfo.access_token,uploadFilePath);
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
                            //Log.e("hml","size="+fileList.getFiles().size());
                            items.clear();
                            items.addAll(fileList.getFiles());
                            Collections.sort(items,new ComparatorFile());
                            adapter.notifyDataSetChanged();
                            setEmptyView(false);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

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
}
