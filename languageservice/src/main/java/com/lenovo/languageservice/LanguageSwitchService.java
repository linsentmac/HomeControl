package com.lenovo.languageservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by linsen on 17-11-10.
 */

public class LanguageSwitchService extends Service {

    private static final String TAG = "DC-LanguageService";
    private LanguageAdmin languageAdmin;

    @Override
    public void onCreate() {
        super.onCreate();
        languageAdmin = new LanguageAdmin(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String result = intent.getStringExtra(LanguageBroadCastReceiver.Key);
        Log.d(TAG, "result = " + result);
        if(result != null){
            if(result.equals(LanguageAdmin.zh_CN)){
                languageAdmin.setLanguage(LanguageAdmin.zh_CN);
            }else {
                languageAdmin.setLanguage(LanguageAdmin.US);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
