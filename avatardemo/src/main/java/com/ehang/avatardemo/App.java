package com.ehang.avatardemo;

import android.app.Application;

import com.ehang.coptersdk.CopterControl;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //CopterControl initialization must be done in Application onCreate()
        CopterControl.getInstance().init(this);
    }
}
