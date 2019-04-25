package com.ktc.googledrive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class WebActivity extends AppCompatActivity {

    private WebView web;
    private static String CLIENT_ID = "1061473829028-5hcks8vhbivqocsa2tnet2i8rjn5vvv9.apps.googleusercontent.com";
    private static String REDIRECT_URI = "http://localhost";
    private static String OAUTH_URL = "https://accounts.google.com/o/oauth2/auth";
    private static String OAUTH_SCOPE = "https://www.googleapis.com/auth/drive" + " https://www.googleapis.com/auth/userinfo.profile";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private TextView tvLoading;
    private RelativeLayout rlLoading;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        activity=this;
        preferences = getSharedPreferences("google_drive", Context.MODE_PRIVATE);
        editor = preferences.edit();
        initView();
    }

    private void initView() {
        web = findViewById(R.id.webView);
        tvLoading = findViewById(R.id.tv_loading);
        rlLoading = findViewById(R.id.rl_loading);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setUserAgentString("MicroMessager");
        web.loadUrl(OAUTH_URL + "?redirect_uri=" + REDIRECT_URI + "&response_type=code&client_id=" + CLIENT_ID + "&scope=" + OAUTH_SCOPE);
        web.setWebViewClient(new WebViewClient() {
            boolean authComplete = false;
            Intent resultIntent = new Intent();

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                rlLoading.setVisibility(View.VISIBLE);
            }

            String authCode;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                rlLoading.setVisibility(View.GONE);
                if (url.contains("?code=") && authComplete != true) {
                    Uri uri = Uri.parse(url);
                    authCode = uri.getQueryParameter("code");
                    Log.e("hml", "code=" + authCode);
                    if (!TextUtils.isEmpty(authCode)) {
                        editor.putString("code", authCode);
                        editor.commit();
                        authComplete = true;
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    }
                } else if (url.contains("error=access_denied")) {
                    authComplete = true;
                    Toast.makeText(activity,"权限受限",Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_CANCELED, resultIntent);
                    finish();
                }

            }

        });

    }
}
