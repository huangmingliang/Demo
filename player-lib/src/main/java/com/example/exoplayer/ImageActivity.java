package com.example.exoplayer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class ImageActivity extends AppCompatActivity {

    private String url;
    private String accessToken;
    private ImageView iv;

    public static void navToImageActivity(Context context, String... params) {
        Intent intent = new Intent(context, ImageActivity.class);
        intent.putExtra("url", params[0]);
        intent.putExtra("accessToken", params[1]);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        initView();
        url = getIntent().getStringExtra("url");
        accessToken = getIntent().getStringExtra("accessToken");
    }

    @Override
    protected void onStart() {
        super.onStart();
        LoginInterceptor logging = new LoginInterceptor();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        Picasso picasso = new Picasso.Builder(this).downloader(new OkHttp3Downloader(client)).build();
        picasso.load(url)
                .error(R.mipmap.ic_launcher)
                .tag(this)
                .into(iv);
    }

    private void initView() {
        iv = findViewById(R.id.iv);
    }

    private class LoginInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request.Builder request = chain.request().newBuilder();
            request.addHeader("Authorization", "Bearer " + accessToken);
            return chain.proceed(request.build());
        }
    }
}
