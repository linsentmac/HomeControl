package cn.lenovo.smartcontrol.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import cn.lenovo.smartcontrol.activity.LockDeviceActivity;
import cn.lenovo.smartcontrol.app_manager.AppManager;
import cn.lenovo.smartcontrol.device.LockDevice;
import cn.lenovo.smartcontrol.mqtt.MqttManager;
import cn.lenovo.smartcontrol.receiver.AndroidSystemReceiver;
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

    private DeviceStatus mDeviceStatus;
    private AppManager mAppManager;
    private WifiControl mWifiControl;
    private SCSharePrefrence mSharePrefrence;
    private LockDevice mLockDevice;
    private AndroidSystemReceiver systemReceiver;
    private MqttManager mMqttManager;

    // extra for intent
    public static final String EXTRA_CMD_ID = "cmdId";
    public static final String EXTRA_CMD_OBJ = "cmdObj";
    public static final String EXTRA_CMD_RESPONSE = "cmdRsp";
    public static final String EXTRA_RST_OBJ = "rstObj";

    // extra for Bundle
    public static final String DOWNLOAD_APP_CMD_RESULT = "downloadAppCmdRst";
    public static final String INSTALL_APP_CMD_RESULT = "installAppCmdRst";


    // Cmd from App
    public static final int MSG_STORE_DOWNLOAD_APP = 0;
    public static final int MSG_STORE_INSTALL_APP = 1;
    public static final int MSG_STORE_DOWNLOAD_PERCENT = 2;
    public static final int MSG_EXIT_LENOVO_ID = 3;
    public static final int MSG_SET_LOCK_DEVICE = 4;
    public static final int MSG_SET_USE_TIMING = 5;
    public static final int MSG_CANCEL_USE_TIMEING = 6;
    public static final int MSG_GET_APP_LIST = 7;
    public static final int MSG_GET_APP_DETAIL_INFO = 8;
    public static final int MSG_CLEAR_DATA_INFO = 9;
    public static final int MSG_CLEAR_CACHE_INFO = 10;
    public static final int MSG_KILL_APP_PROCESS = 11;
    public static final int MSG_UNINSTALL_APP = 12;

    // message to notify with Lenovo ID
    public static final int MSG_NOTIFY_ONLINE = 13;
    public static final int MSG_NOTIFY_OFFLINE = 14;

    // report msg type
    public static final String REPORT_MSG_TYPE = "messageType";
    public static final int REPORT_DEVICE_MSG = 100;
    public static final int REPORT_APP_DOWNLOAD_MSG = 101;
    public static final int REPORT_APP_INSTALL_MSG = 102;
    public static final int REPORT_APP_DOWNLOAD_PERCENT = 103;

    // message for callback
    public static final int MSG_RESPONSE = 1000;

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
        Log.d(TAG, "DeviceService started !");
        mContext = this;
        mWorkThread = new HandlerThread("SCWorkThread");
        mWorkThread.start();
        mWorkHandler = new SCServiceHandler(mWorkThread.getLooper());

        mDeviceStatus = DeviceStatus.getInstance(mContext);
        // init AppManager
        mAppManager = new AppManager(mContext, mWorkHandler);
        // init WifiControl
        mWifiControl = new WifiControl(mContext);
        mSharePrefrence = SCSharePrefrence.getInstance(mContext);
        mLockDevice = LockDevice.getInstance(mContext);
        // init MqttManager
        mMqttManager = MqttManager.getInstance(mContext, mWorkHandler);

        // register SystemReceiver
        systemReceiver = new AndroidSystemReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(systemReceiver, intentFilter);

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
        String pkgName = null;
        if(cmdParams != null){
            pkgName = cmdParams.getString(APP_NAME);
        }
        switch (cmdId){
            case MSG_STORE_INSTALL_APP:
                Log.d(TAG, "install app........");
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

            case MSG_NOTIFY_ONLINE:
                mMqttManager.reportLenovoID();
                mMqttManager.reportDeviceStatus();
                break;
            case MSG_NOTIFY_OFFLINE:
                mMqttManager.notifyDeviceOFFLINE();
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
                JSONObject downLoad_Json = new JSONObject();
                try {
                    downLoad_Json.put(REPORT_MSG_TYPE, REPORT_APP_DOWNLOAD_MSG);
                    downLoad_Json.put("packageName", packageName);
                    downLoad_Json.put("download_result", ret);
                    mMqttManager.publishMessage(MqttManager.publishTopic, downLoad_Json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MSG_STORE_INSTALL_APP:
                packageName = result.getString(APP_NAME);
                ret = result.getInt(DOWNLOAD_APP_CMD_RESULT, -1);
                JSONObject install_Json = new JSONObject();
                try {
                    install_Json.put(REPORT_MSG_TYPE, REPORT_APP_INSTALL_MSG);
                    install_Json.put("packageName", packageName);
                    install_Json.put("install_result", ret);
                    mMqttManager.publishMessage(MqttManager.publishTopic, install_Json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MSG_STORE_DOWNLOAD_PERCENT:
                packageName = result.getString(APP_NAME);
                ret = result.getInt(DOWNLOAD_APP_CMD_RESULT, -1);
                JSONObject percent_Json = new JSONObject();
                try {
                    percent_Json.put(REPORT_MSG_TYPE, REPORT_APP_DOWNLOAD_PERCENT);
                    percent_Json.put("packageName", packageName);
                    percent_Json.put("percent", ret);
                    mMqttManager.publishMessage(MqttManager.publishTopic, percent_Json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
        if(intent != null){
            int cmdId = intent.getIntExtra(EXTRA_CMD_ID, -1);
            Message.obtain(mWorkHandler, cmdId, intent).sendToTarget();
        }
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
        Log.d(TAG, "DeviceService onDestory");
        unregisterReceiver(systemReceiver);
        mLockDevice.cancelTiming();
        mAppManager.releaseAppTask();
        mMqttManager.notifyDeviceOFFLINE();
    }



}
