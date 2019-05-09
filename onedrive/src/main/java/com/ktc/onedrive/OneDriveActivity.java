package com.ktc.onedrive;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import com.example.exoplayer.ImageActivity;
import com.example.exoplayer.PlayerActivity;
import com.ktc.onedrive.adapter.FileAdapter;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.extensions.DriveItem;
import com.microsoft.graph.extensions.IDriveItemCollectionPage;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import java.util.ArrayList;
import java.util.List;

public class OneDriveActivity extends AppCompatActivity {

    private AuthenticationHelper mAuthHelper;
    private GraphHelper graphHelper = GraphHelper.getInstance();
    private final String TAG = getClass().getSimpleName();
    private String accountId;
    private List<DriveItem> items = new ArrayList<>();
    private FileAdapter adapter;
    private ListView lvFile;
    private Activity activity;
    private List<String> records = new ArrayList<>();     //用户返回判断
    private FrameLayout emptyView;
    private TextView tvPath;
    private ProgressBar pb;
    private TextView tvEmpty;


    static public void navToOneDrive(Context context) {
        Intent intent = new Intent(context, OneDriveActivity.class);
        context.startActivity(intent);
    }

    static public void navToOneDrive(Context context, String accountId) {
        Intent intent = new Intent(context, OneDriveActivity.class);
        intent.putExtra("accountId", accountId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthHelper = AuthenticationHelper.getInstance(getApplicationContext());
        accountId = getIntent().getStringExtra("accountId");
        //Log.e("hml","accountId="+accountId);
        activity = this;
        setContentView(R.layout.activity_one_drive);
        initView();
        signIn();
    }

    private void initView() {
        tvPath = findViewById(R.id.tv_path);
        emptyView = findViewById(R.id.emptyView);
        pb = findViewById(R.id.pb);
        tvEmpty = findViewById(R.id.tv_empty);
        lvFile = findViewById(R.id.lv_file);
        lvFile.setEmptyView(emptyView);
        adapter = new FileAdapter(this, items, getSupportFragmentManager());
        lvFile.setAdapter(adapter);
        lvFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DriveItem item = items.get(position);
                if (item.folder != null) {
                    items.clear();
                    setEmptyView(true);
                    graphHelper.getDriveItemById(getDriveCallback(), item.id);
                    records.add(item.id);
                } else {
                    String mimeType=item.file.mimeType;
                    String url="https://graph.microsoft.com/v1.0/me/drive/items/"+item.id+"/content";
                    if (mimeType.startsWith("image/")) {
                        ImageActivity.navToImageActivity(activity,url,accessToken);
                    } else if (mimeType.startsWith("audio/")) {
                        PlayerActivity.navToPlayer(activity,url,accessToken);
                    } else if (mimeType.startsWith("video/")) {
                        PlayerActivity.navToPlayer(activity,url,accessToken);
                    } else{

                    }

                }
            }
        });

    }


    // Silently sign in - used if there is already a
// user account in the MSAL cache
    private void doSilentSignIn() {
        for (IAccount iAccount : mAuthHelper.getAccounts()) {
            if (iAccount.getAccountIdentifier().getIdentifier().equals(accountId)) {
                mAuthHelper.acquireTokenSilently(getAuthCallback(), iAccount);
            }
        }
    }


    // Prompt the user to sign in
    private void doInteractiveSignIn() {
        mAuthHelper.acquireTokenInteractively(this, getAuthCallback());
    }


    // Handles the authentication result
    private String accessToken;
    public AuthenticationCallback getAuthCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                // Log the token for debug purposes
                accessToken = authenticationResult.getAccessToken();
                Log.d("hml", String.format("Access token: %s", accessToken));
                items.clear();
                graphHelper.getDriveRootChildren(accessToken, getDriveCallback());

            }

            @Override
            public void onError(MsalException exception) {
                // Check the type of exception and handle appropriately
                if (exception instanceof MsalUiRequiredException) {
                    Log.d("hml", "Interactive login required");
                    doInteractiveSignIn();

                } else if (exception instanceof MsalClientException) {
                    // Exception inside MSAL, more info inside MsalError.java
                    Log.e("hml", "Client error authenticating", exception);
                } else if (exception instanceof MsalServiceException) {
                    // Exception when communicating with the auth server, likely config issue
                    Log.e("hml", "Service error authenticating", exception);
                }
            }

            @Override
            public void onCancel() {
                // User canceled the authentication
                Log.d("hml", "Authentication canceled");
            }
        };
    }

    private void signIn() {
        if (!TextUtils.isEmpty(accountId)) {
            doSilentSignIn();
        } else {
            doInteractiveSignIn();
        }
    }

    private void signOut() {
        mAuthHelper.signOut();
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


    private ICallback<IDriveItemCollectionPage> getDriveCallback() {
        return new ICallback<IDriveItemCollectionPage>() {
            @Override
            public void success(final IDriveItemCollectionPage page) {
                items.clear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (page.getCurrentPage() != null) {
                            items.addAll(page.getCurrentPage());
                        }
                        adapter.notifyDataSetChanged();
                        setEmptyView(false);
                    }
                });


            }

            @Override
            public void failure(final ClientException ex) {
                Log.e("hml", "ex=" + ex.toString());
                items.clear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        Toast.makeText(activity, "请求数据失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (records.size() > 1) {
            records.remove(records.size() - 1);
            String itemId = records.get(records.size() - 1);
            graphHelper.getDriveItemById(getDriveCallback(), itemId);
        } else if (records.size() > 0 && records.size() <= 1) {
            records.remove(records.size() - 1);
            graphHelper.getDriveRootChildren(null, getDriveCallback());
        } else {
            super.onBackPressed();
        }

    }

    public void refresh() {
        setEmptyView(true);
        if (records.size()>0){
            String itemId=records.get(records.size()-1);
            graphHelper.getDriveItemById(getDriveCallback(), itemId);
        }else {
            graphHelper.getDriveRootChildren(null,getDriveCallback());
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityManager am= (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.killBackgroundProcesses("com.android.browser");
        mAuthHelper.handleRedirect(requestCode, resultCode, data);
    }

}
