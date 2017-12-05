package cn.lenovo.smartcontrol.utils;

import android.content.Context;
import android.content.SharedPreferences;

import cn.lenovo.smartcontrol.service.DeviceService;

/**
 * Created by linsen on 17-12-4.
 */

public class SCSharePrefrence {

    private static final String SPNAME = "SC_Control";

    private static SCSharePrefrence mInstance;
    private Context mContext;

    private SharedPreferences sp;
    private SharedPreferences.Editor spEdit;

    private SCSharePrefrence(Context context){
        mContext = context;
        sp = mContext.getSharedPreferences(SPNAME, Context.MODE_PRIVATE);
    }

    public static SCSharePrefrence getInstance(Context context){
        if(mInstance == null){
            mInstance = new SCSharePrefrence(context);
        }
        return mInstance;
    }

    public void setLockTime(long time){
        spEdit = sp.edit();
        spEdit.putLong(DeviceService.LOCK_TIME, time);
        spEdit.commit();
    }

    public long getLockTime(){
        return sp.getLong(DeviceService.LOCK_TIME, 0);
    }


}
