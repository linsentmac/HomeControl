package cn.lenovo.smartcontrol.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import cn.lenovo.smartcontrol.app_manager.AppInstall;

/**
 * Created by linsen on 17-12-1.
 */

public class DeviceService extends Service {

    private static final boolean DBG = true;
    private static final String TAG = "SC-DeviceService";
    private HandlerThread mWorkThread;
    private Handler mWorkHandler;
    private Context mContext;

    private AppInstall mAppInstall;

    // extra for intent
    public static final String EXTRA_CMD_ID = "cmdId";
    public static final String EXTRA_CMD_OBJ = "cmdObj";
    public static final String EXTRA_CMD_RESPONSE = "cmdRsp";
    public static final String EXTRA_RST_OBJ = "rstObj";

    // extra for Bundle
    public static final String INSTALL_APP_CMD_RESULT = "installAppCmdRst";


    // message from App
    public static final int MSG_STARE_DOWNLOAD_APP = 0;
    public static final int MSG_STORE_INSTALL_APP = 1;

    // message for callback
    public static final int MSG_RESPONSE = 100;

    // extra for install app name
    public static final String APP_NAME = "packageName";
    public static final String APP_ADDRESS = "downloadUrl";

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mWorkThread = new HandlerThread("SCWorkThread");
        mWorkThread.start();
        mWorkHandler = new SCServiceHandler(mWorkThread.getLooper());

        // init AppInstall
        mAppInstall = new AppInstall(mContext, mWorkHandler);
    }

    private class SCServiceHandler extends Handler{

        private SCServiceHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int cmdId = msg.what;
            Intent intent = (Intent) msg.obj;
            if(intent != null){
                handleSCServiceCommands(cmdId, intent);
            }
            super.handleMessage(msg);
        }
    }

    private void handleSCServiceCommands(int cmdId, Intent intent){
        Bundle cmdParams = intent.getBundleExtra(EXTRA_CMD_OBJ);
        switch (cmdId){
            case MSG_STORE_INSTALL_APP:
                String packageName = cmdParams.getString(APP_NAME);
                String apkUrl = cmdParams.getString(APP_ADDRESS);
                mAppInstall.downLoadApk(packageName, apkUrl);
                break;
            case MSG_RESPONSE:

                break;
            default:
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(DBG) Log.d(TAG, "onStartCommand");
        /*if (intent == null) {
            if(DBG) Log.e(TAG, "intent is null");
            intent = new Intent(mContext, DeviceService.class);
            intent.putExtra(DeviceService.EXTRA_CMD_ID, DeviceService.MSG_DEVICE_APP_STARTUP);
        }*/
        int cmdId = intent.getIntExtra(EXTRA_CMD_ID, -1);
        Message.obtain(mWorkHandler, cmdId, intent).sendToTarget();
        // flags must be START_REDELIVER_INTENT, avoid next start intent change to null.
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
