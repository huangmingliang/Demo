package com.ktc.googledrive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;

public class GoogleDriveActivity extends AppCompatActivity implements View.OnClickListener {

    private static String CLIENT_ID = "1061473829028-5hcks8vhbivqocsa2tnet2i8rjn5vvv9.apps.googleusercontent.com";
    //Use your own client id
    private static String CLIENT_SECRET = "IvCnfwdK728mKCAwPXHhu9rS";
    //Use your own client secret
    private static String REDIRECT_URI = "http://localhost";
    private static String GRANT_TYPE = "authorization_code";
    private final String REFRESH_TOKEN = "refresh_token";
    private static String TOKEN_URL = "https://oauth2.googleapis.com/token";


    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private int REQUEST_CODE = 1;
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
        preferences = getSharedPreferences("google_drive", Context.MODE_PRIVATE);
        editor = preferences.edit();
        if (TextUtils.isEmpty(sub)) {
            Intent intent = new Intent(this, WebActivity.class);
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            refreshToken();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            getToken();
        } else {
            Log.e("hml", "resultCode=" + resultCode);
        }
    }

    private void getToken() {
        OkHttpUtils
                .post()
                .url(TOKEN_URL)
                .addParams("code", preferences.getString("code", ""))
                .addParams("client_id", CLIENT_ID)
                .addParams("client_secret", CLIENT_SECRET)
                .addParams("redirect_uri", REDIRECT_URI)
                .addParams("grant_type", GRANT_TYPE)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.e("hml", "e=" + e.fillInStackTrace().toString());
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Gson gson = new Gson();
                        TokenInfo tokenInfo = gson.fromJson(response, TokenInfo.class);
                        //Log.e("hml", "access_token=" + tokenInfo.access_token);
                        //Log.e("hml", "refresh_token=" + tokenInfo.refresh_token);
                        editor.putString("refresh_token", tokenInfo.refresh_token);
                        editor.putString("access_token", tokenInfo.access_token);
                        editor.commit();
                        refreshUserInfo();
                        getRootFile(tokenInfo.access_token);
                    }
                });
    }

    private void getRootFile(String accessToken){
        driveHelper.queryRootFiles(accessToken).addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                Log.e("hml","file:"+file.toString());
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
                .addParams("client_secret", CLIENT_SECRET)
                .addParams("grant_type", REFRESH_TOKEN)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.e("hml", "e=" + e.toString());
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Gson gson = new Gson();
                        TokenInfo tokenInfo = gson.fromJson(response, TokenInfo.class);
                        //Log.e("hml", "access_token2=" + tokenInfo.access_token);
                        editor.putString("access_token", tokenInfo.access_token);
                        editor.commit();
                        getRootFile(tokenInfo.access_token);
                    }
                });
    }

    private void list(String query) {
        try {
            driveHelper.listFiles(query, null, true)
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

    private void refreshUserInfo() {
        OkHttpUtils
                .post()
                .url("https://www.googleapis.com/oauth2/v3/userinfo")
                .addHeader("Authorization", "Bearer" + preferences.getString("access_token", ""))
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
                        //Log.e("hml", "userInfo=" + gson.toJson(userInfo));
                        GAccount account = new GAccount(userInfo.sub, preferences.getString("refresh_token", ""), userInfo.name, userInfo.picture);
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
