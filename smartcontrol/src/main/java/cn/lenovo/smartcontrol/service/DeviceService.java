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

import java.util.List;

import cn.lenovo.smartcontrol.activity.LockDeviceActivity;
import cn.lenovo.smartcontrol.app_manager.AppManager;
import cn.lenovo.smartcontrol.utils.LockDevice;
import cn.lenovo.smartcontrol.utils.SCSharePrefrence;
import cn.lenovo.smartcontrol.wifi_manager.WifiControl;

/**
 * Created by linsen on 17-12-1.
 */

public class DeviceService extends Service {

    private static final boolean DBG = true;
    private static final String TAG = "SC-DeviceService";
    private HandlerThread mWorkThread;
    private Handler mWorkHandler;
    private Context mContext;

    private AppManager mAppManager;
    private WifiControl mWifiControl;
    private SCSharePrefrence mSharePrefrence;
    private LockDevice mLockDevice;

    // extra for intent
    public static final String EXTRA_CMD_ID = "cmdId";
    public static final String EXTRA_CMD_OBJ = "cmdObj";
    public static final String EXTRA_CMD_RESPONSE = "cmdRsp";
    public static final String EXTRA_RST_OBJ = "rstObj";

    // extra for Bundle
    public static final String DOWNLOAD_APP_CMD_RESULT = "downloadAppCmdRst";
    public static final String INSTALL_APP_CMD_RESULT = "installAppCmdRst";


    // message from App
    public static final int MSG_STORE_DOWNLOAD_APP = 0;
    public static final int MSG_STORE_INSTALL_APP = 1;
    public static final int MSG_EXIT_LENOVO_ID = 2;
    public static final int MSG_SET_LOCK_DEVICE = 3;
    public static final int MSG_SET_USE_TIMING = 4;
    public static final int MSG_CANCEL_USE_TIMEING = 5;
    public static final int MSG_GET_APP_LIST = 6;
    public static final int MSG_GET_APP_DETAIL_INFO = 7;
    public static final int MSG_CLEAR_DATA_INFO = 8;
    public static final int MSG_CLEAR_CACHE_INFO = 9;
    public static final int MSG_KILL_APP_PROCESS = 10;
    public static final int MSG_UNINSTALL_APP = 11;

    // message for callback
    public static final int MSG_RESPONSE = 100;

    // extra for install app name
    public static final String APP_NAME = "packageName";
    public static final String APP_ADDRESS = "downloadUrl";

    // extra for set lock device
    public static final String LOCK_TIME = "lockTime";
    public static final String LOCK_START_TIME = "lockStartTime";
    public static final String LOCK_END_TIME = "lockEndTime";
    public static final String LCOK_WEEK_DAY = "lockWeekDay";

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mWorkThread = new HandlerThread("SCWorkThread");
        mWorkThread.start();
        mWorkHandler = new SCServiceHandler(mWorkThread.getLooper());

        // init AppManager
        mAppManager = new AppManager(mContext, mWorkHandler);
        // init WifiControl
        mWifiControl = new WifiControl(mContext);
        mSharePrefrence = SCSharePrefrence.getInstance(mContext);
        mLockDevice = LockDevice.getInstance(mContext);

        // check lock device
        checkLockDeviceTime();
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
        String pkgName = cmdParams.getString(APP_NAME);
        switch (cmdId){
            case MSG_STORE_INSTALL_APP:
                String packageName = cmdParams.getString(APP_NAME);
                String apkUrl = cmdParams.getString(APP_ADDRESS);
                mAppManager.downLoadApk(packageName, apkUrl);
                break;
            case MSG_EXIT_LENOVO_ID:

                break;
            case MSG_SET_LOCK_DEVICE:
                long lockTime = cmdParams.getLong(LOCK_TIME);
                Intent lockIntent = new Intent(mContext, LockDeviceActivity.class);
                lockIntent.putExtra(LOCK_TIME, lockTime);
                startActivity(lockIntent);
                break;
            case MSG_SET_USE_TIMING:
                String startTime = cmdParams.getString(LOCK_START_TIME);
                String endTime = cmdParams.getString(LOCK_END_TIME);
                String[] weekDays = cmdParams.getStringArray(LCOK_WEEK_DAY);
                mLockDevice.setDeviceTiming(startTime, endTime, weekDays);
                break;
            case MSG_CANCEL_USE_TIMEING:
                mLockDevice.cancelTiming();
                break;
            case MSG_GET_APP_LIST:
                mAppManager.getInstalledApps();
                break;
            case MSG_GET_APP_DETAIL_INFO:
                mAppManager.getpkginfo(pkgName);
                break;
            case MSG_CLEAR_DATA_INFO:
                mAppManager.clearDataInfo(pkgName);
                break;
            case MSG_CLEAR_CACHE_INFO:
                mAppManager.clearCacheInfo(pkgName);
                break;
            case MSG_KILL_APP_PROCESS:
                mAppManager.killProcesses(pkgName);
                break;
            case MSG_UNINSTALL_APP:
                mAppManager.deleteApk(pkgName);
                break;

            case MSG_RESPONSE:
                handleResponseMessage(intent);
                break;
            default:
                break;
        }
    }

    private void handleResponseMessage(Intent intent){
        int ret = -1;
        String packageName;
        int cmdId = intent.getIntExtra(EXTRA_CMD_RESPONSE, -1);
        //if (DBG) Log.d(TAG, "handle response " + parseCmdIdToString(cmdId));
        Bundle result = intent.getBundleExtra(EXTRA_RST_OBJ);
        switch (cmdId){
            case MSG_STORE_DOWNLOAD_APP:
                packageName = result.getString(APP_NAME);
                ret = result.getInt(DOWNLOAD_APP_CMD_RESULT, -1);
                break;
            case MSG_STORE_INSTALL_APP:
                packageName = result.getString(APP_NAME);
                ret = result.getInt(DOWNLOAD_APP_CMD_RESULT, -1);
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

    private void checkLockDeviceTime(){
        long time = mSharePrefrence.getLockTime();
        if(time != 0){
            Message msg = new Message();
            msg.what = MSG_SET_LOCK_DEVICE;
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putLong(LOCK_TIME, time);
            intent.putExtra(EXTRA_CMD_OBJ, bundle);
            msg.obj = intent;
            mWorkHandler.sendMessage(msg);
        }
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
