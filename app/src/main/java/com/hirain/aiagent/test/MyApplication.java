package com.hirain.aiagent.test;

import android.app.Application;

import com.hirain.aiagent.AIAgent;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AIAgent.getInstance().init(this);
    }
}
