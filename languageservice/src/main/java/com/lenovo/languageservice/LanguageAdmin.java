package com.lenovo.languageservice;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;


import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Created by linsen on 2017/11/9.
 */

public class LanguageAdmin {
    private Context mContext;
    public static final String zh_CN = "中文(简体)";
    public static final String US = "English";

    public LanguageAdmin(Context context) {
        mContext = context;
    }

    public String getLanguage() {
        Locale locale;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            LocaleList localeList = mContext.getResources().getConfiguration().getLocales();
            locale = localeList.get(0);
        } else {
            locale = Locale.getDefault();
        }

        return locale == null ? null : locale.toString();
    }

    public void setLanguage(String language){
        if (language.equals(zh_CN)) {
            updateLanguage(Locale.SIMPLIFIED_CHINESE);
        }else {
            updateLanguage(Locale.US);
        }
    }
    public void addLanguage(){

    }

    public void updateLanguage(Locale locale) {
        try {
            Object objIActMag, objActMagNative;
            Class clzIActMag = Class.forName("android.app.IActivityManager");
            Class clzActMagNative = Class.forName("android.app.ActivityManagerNative");
            Method mtdActMagNative$getDefault = clzActMagNative.getDeclaredMethod("getDefault");
            objIActMag = mtdActMagNative$getDefault.invoke(clzActMagNative);
            // objIActMag = amn.getConfiguration();
            Method mtdIActMag$getConfiguration = clzIActMag
                    .getDeclaredMethod("getConfiguration");
            Configuration config = (Configuration) mtdIActMag$getConfiguration.invoke(objIActMag);         // set the locale to the new value
            config.locale = locale;         //持久化  config.userSetLocale = true;
            Class clzConfig = Class.forName("android.content.res.Configuration");
            java.lang.reflect.Field userSetLocale = clzConfig.getField("userSetLocale");
            userSetLocale.set(config, true);
            // 此处需要声明权限:android.permission.CHANGE_CONFIGURATION       // 会重新调用 onCreate();
            Class[] clzParams = {Configuration.class};
            // objIActMag.updateConfiguration(config);
            Method mtdIActMag$updateConfiguration = clzIActMag.getDeclaredMethod("updateConfiguration", clzParams);
            mtdIActMag$updateConfiguration.invoke(objIActMag, config);
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
