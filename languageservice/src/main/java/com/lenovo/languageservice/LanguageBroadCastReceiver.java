package com.lenovo.languageservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by linsen on 17-11-10.
 */

public class LanguageBroadCastReceiver extends BroadcastReceiver {

    private static final String TAG = "DC-LanguageReceiver";
    public static final String Key = "language";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "language receiver = " + intent.getAction());
        if(intent.getAction().equals("android.intent.action.SWITCH_LANGUAGE")){
            String language = intent.getStringExtra(Key);
            Intent serviceIntent = new Intent(context, LanguageSwitchService.class);
            serviceIntent.putExtra(Key, language);
            context.startService(serviceIntent);
        }
    }
}
