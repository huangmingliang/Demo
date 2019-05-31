package com.ktc.onedrive;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
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
import com.ktc.onedrive.adapter.FileAdapter;
import com.ktc.share.ReflectUtil;
import com.ktc.share.SpUtil;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.extensions.DriveItem;
import com.microsoft.graph.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.extensions.UploadSession;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OneDriveActivity extends AppCompatActivity {

    private AuthenticationHelper mAuthHelper;
    private String accessToken;
    private GraphHelper graphHelper = GraphHelper.getInstance();
    private final String TAG = getClass().getSimpleName();
    private String accountId;
    private List<DriveItem> items = new ArrayList<>();
    private FileAdapter adapter;
    private GridView gridView;
    private Activity activity;
    private List<String> records = new ArrayList<>();     //用户返回判断
    private FrameLayout emptyView;
    private TextView tvPath;
    private ProgressBar pb;
    private TextView tvEmpty;
    private int REQUEST_WRITE_EXTERNAL_STORAGE = 100;
    private final String ONE_DRIVE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/OneDrive";
    private DriveItem downloadItem;


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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        float scale = getResources().getDisplayMetrics().density;
        mAuthHelper = AuthenticationHelper.getInstance(getApplicationContext());
        accountId = getIntent().getStringExtra("accountId");
        //Log.e("hml","accountId="+accountId);
        activity = this;
        setContentView(R.layout.activity_cloud_files);
        initView();
        signIn();
    }

    private void initView() {
        tvPath = findViewById(R.id.tv_path);
        tvPath.setText("OneDrive");
        emptyView = findViewById(R.id.emptyView);
        pb = findViewById(R.id.pb);
        tvEmpty = findViewById(R.id.tv_empty);
        gridView = findViewById(R.id.gv_file);
        gridView.setEmptyView(emptyView);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DriveItem item = items.get(position);
                if (item.folder != null) {
                    items.clear();
                    setEmptyView(true);
                    graphHelper.getDriveItemById(getDriveCallback(), item.id);
                    records.add(item.id);
                } else {

                    String mimeType = item.file.mimeType;
                    String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + item.id + "/content";
                    if (mimeType.startsWith("image/")) {
                        ImageActivity.navToImageActivity(activity, url, accessToken);
                    } else if (mimeType.startsWith("audio/")) {
                        PlayerActivity.navToPlayer(activity, url, accessToken,item.name);
                    } else if (mimeType.startsWith("video/")) {
                        PlayerActivity.navToPlayer(activity, url, accessToken);
                    } else {
                        Toast.makeText(activity, getString(R.string.preview_is_not_supported), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

    private class DownloadTask extends AsyncTask<InputStream, Integer, Void> {

        private ProgressDialog dialog;
        private String fileName;
        private long size;

        DownloadTask(String fileName, long size) {
            this.fileName = fileName;
            this.size = size;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = createProgressBar();
            dialog.show();
        }

        @Override
        protected Void doInBackground(InputStream... inputStreams) {

            File dir = new File(ONE_DRIVE_SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File target = new File(dir, fileName);
            if (target.exists()) {
                target.delete();
            }
            try {
                target.createNewFile();
                FileOutputStream fos = new FileOutputStream(target);
                byte[] b = new byte[1024];
                int length;
                int total = 0;
                while ((length = inputStreams[0].read(b)) != -1) {
                    total += length;
                    fos.write(b, 0, length);
                    int value = (int) (total / (size * 1.0f) * 100);
                    publishProgress(value);
                }
                fos.flush();
                inputStreams[0].close();
                fos.close();
            } catch (Exception e) {
                publishProgress(-1);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            dialog.setProgress(values[0]);
            if (values[0]==100){
                dialog.setTitle("下载完成!可在本地OneDrive目录中查看");
                downloadItem=null;
            }
        }

        private ProgressDialog createProgressBar() {
            ProgressDialog dialog = new ProgressDialog(activity);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setTitle("正在下载");
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
    }



    private ICallback<InputStream> getDownloadCallback(final String fileName, final long size) {
        return new ICallback<InputStream>() {
            @Override
            public void success(InputStream inputStream) {
                new DownloadTask(fileName, size).execute(inputStream);
            }

            @Override
            public void failure(final ClientException ex) {
                Log.e("failure", "ClientException:" + ex.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "请求失败" + ex.getCause(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        };
    }

    private ICallback<Void> getDeleteCallback() {
        return new ICallback<Void>() {
            @Override
            public void success(Void aVoid) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "删除成功", Toast.LENGTH_SHORT).show();
                        refresh();
                    }
                });
            }

            @Override
            public void failure(ClientException ex) {

            }
        };
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
    public AuthenticationCallback getAuthCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                // Log the token for debug purposes
                accessToken = authenticationResult.getAccessToken();
                //Log.d("token", String.format("Access token: %s", accessToken));
                String uploadFilePath = SpUtil.getUploadFilePath();
                if (!TextUtils.isEmpty(uploadFilePath)) {
                    final File uploadFile = new File(uploadFilePath);
                    SpUtil.setUploadFilePath(null);
                    graphHelper.uploadBigFileDriveItemById(accessToken, uploadFile, new ICallback<UploadSession>() {
                        @Override
                        public void success(UploadSession uploadSession) {
                            new UploadTask().execute(uploadFile.getPath(), uploadSession.uploadUrl);
                        }

                        @Override
                        public void failure(ClientException ex) {
                        }
                    });
                }
                items.clear();
                graphHelper.getDriveRootChildren(accessToken, getDriveCallback());

            }

            @Override
            public void onError(MsalException exception) {
                // Check the type of exception and handle appropriately
                if (exception instanceof MsalUiRequiredException) {
                    Log.d("exception", "Interactive login required");
                    doInteractiveSignIn();

                } else if (exception instanceof MsalClientException) {
                    // Exception inside MSAL, more info inside MsalError.java
                    Log.e("exception", "Client error authenticating", exception);
                } else if (exception instanceof MsalServiceException) {
                    // Exception when communicating with the auth server, likely config issue
                    Log.e("exception", "Service error authenticating", exception);
                }
            }

            @Override
            public void onCancel() {
                // User canceled the authentication
                Log.d("AUTH", "Authentication canceled");
            }
        };
    }


    private class UploadTask extends AsyncTask<String, Integer, Void> {

        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = createProgressBar();
            dialog.show();
        }

        @Override
        protected Void doInBackground(final String... strings) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(strings[1]);
                File file = new File(strings[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("PUT");
                conn.setConnectTimeout(50000);
                conn.connect();
                FileInputStream fis = new FileInputStream(file);
                OutputStream out = new DataOutputStream(conn.getOutputStream());
                DataInputStream in = new DataInputStream(fis);
                int bytes;
                int total = 0;
                long size = fis.available();
                byte[] bufferOut = new byte[2048];
                while ((bytes = in.read(bufferOut)) != -1) {
                    out.write(bufferOut, 0, bytes);
                    total += bytes;
                    int value = (int) (total / (size * 1.0f) * 100);
                    publishProgress(value);
                }
                in.close();
                out.flush();
                out.close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.e("hml", "line=" + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            dialog.setProgress(values[0]);
            if (values[0] == 100) {
                dialog.setTitle("上传完成");
                Toast.makeText(activity,getString(R.string.has_been_uploaded_to)+getString(R.string.one_drive),Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            refresh();
            //finish();
        }

        private ProgressDialog createProgressBar() {
            ProgressDialog dialog = new ProgressDialog(activity);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setTitle("正在上传");
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

    private void setEmptyView(boolean reset) {
        adapter.notifyDataSetChanged();
        if (reset) {
            tvEmpty.setVisibility(View.GONE);
            pb.setVisibility(View.VISIBLE);
        } else {
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
                        //adapter.notifyDataSetChanged();
                        adapter=new FileAdapter(activity,items);
                        gridView.setAdapter(adapter);
                        adapter.setOnFileListener(listener);
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
        if (records.size() > 0) {
            String itemId = records.get(records.size() - 1);
            graphHelper.getDriveItemById(getDriveCallback(), itemId);
        } else {
            graphHelper.getDriveRootChildren(null, getDriveCallback());
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ReflectUtil.forceStopPackage(am, "com.android.browser");
        mAuthHelper.handleRedirect(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (downloadItem!=null){
                    Toast.makeText(activity, downloadItem.name + "准备下载", Toast.LENGTH_SHORT).show();
                    graphHelper.downloadDriveItemById(getDownloadCallback(downloadItem.name, downloadItem.size), downloadItem.id);
                }
            }
        }
    }

    private FileAdapter.OnFileListener listener=new FileAdapter.OnFileListener() {
        @Override
        public void onDelete(int position) {
            String itemId = items.get(position).id;
            graphHelper.deleteDriveItemById(itemId, getDeleteCallback());
        }

        @Override
        public void onDownload(int position) {
            downloadItem = items.get(position);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission
                        .WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(activity, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
                }
                //申请权限
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                if (downloadItem!=null){
                    Toast.makeText(activity, downloadItem.name + "准备下载", Toast.LENGTH_SHORT).show();
                    graphHelper.downloadDriveItemById(getDownloadCallback(downloadItem.name, downloadItem.size), downloadItem.id);
                }
            }
        }
    };


}
