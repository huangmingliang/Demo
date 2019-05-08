package com.ktc.demo;

import android.app.Application;

import com.ktc.share.SpUtil;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SpUtil.getInstance(this);
    }
}
