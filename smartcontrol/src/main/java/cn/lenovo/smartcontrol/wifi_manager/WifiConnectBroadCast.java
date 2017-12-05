package cn.lenovo.smartcontrol.wifi_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

/**
 * Wifi状态变化广播接收器
 */
public class WifiConnectBroadCast extends BroadcastReceiver {
    private static final String TAG = "WifiConnectBroadCast";

    public static final int WIFI_STATE = 10;
    public static final int WIFI_NETWORK_STATE = 20;
    public static final int PASSWORD_ERROR = 30;
    public static final int START_SCAN_SUCCEED = 40;
    public static final int CONNECT_WIFI_RESULT = 50;
    public static final int WIFI_NETWORK_STRENGTH = 60;

    private IntentFilter mDefFilter;
    private Handler handler;
    private WifiManager wifiManager;

    public WifiConnectBroadCast(Handler handler) {
        this.handler = handler;
        mDefFilter = new IntentFilter();
        mDefFilter.addAction(WifiManager.RSSI_CHANGED_ACTION); //信号强度变化
        mDefFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION); //网络状态变化
        mDefFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION); //wifi状态变化，指开关
        mDefFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION); //认证状态
        mDefFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION); //扫描wifi
    }

    public IntentFilter getDefaultFilter() {
        return mDefFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "receive broadcast " + action);
        if (action != null) {
            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION://网络状态变化
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    //已连接到WIFI（此判断在6.0下非常不靠谱 samsung galaxy s6却是准确的）
                    if (info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI) {
                        //消费由enableNetwork触发的2次isConnected事件
                        if (!"samsung".equals(Build.BRAND)) {
                            if (WifiConnectManager.enableNetworkNum > 0)
                                WifiConnectManager.enableNetworkNum--;
                            if (WifiConnectManager.enableNetworkNum > 0) return;
                        }

                        if (wifiManager == null) {
                            wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        }
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo == null) return;
                        String currSSID = wifiInfo.getSSID();//从连接的WIFI中获取SSID
                        if (currSSID.equals("0x") || currSSID.equals("<unknown ssid>")) return;
                        currSSID = currSSID.replace("\"", "");
                        if (!TextUtils.isEmpty(currSSID)) {
                            if (handler != null) {
                                Log.w(TAG, "----- Network state change, currSSID = " + currSSID + " -----");
                                handler.obtainMessage(CONNECT_WIFI_RESULT, currSSID).sendToTarget();
                            }
                        }
                    }
                    break;

                case WifiManager.RSSI_CHANGED_ACTION://信号强度变化
                    int strength = getStrength(context);
                    Log.d(TAG, "当前信号强度：" + strength);
                    handler.obtainMessage(WIFI_NETWORK_STRENGTH, strength).sendToTarget();
                    break;

                case WifiManager.WIFI_STATE_CHANGED_ACTION://wifi状态变化，指开关
                    int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                    if (handler != null) {
                        handler.obtainMessage(WIFI_STATE, wifiState).sendToTarget();
                    }
                    break;

                case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION://认证状态变化
                    //密码判断
                    int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);
                    if (error == WifiManager.ERROR_AUTHENTICATING) {
                        Log.e(TAG, "密码验证失败：" + WifiConnectManager.connectingSSID);
                        if (handler != null) handler.obtainMessage(PASSWORD_ERROR).sendToTarget();
                    }
                    break;

                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION://扫描wifi
                    /**
                     *An access point scan has completed, and results are available from the supplicant.
                     *Call getScanResults() to obtain the results. EXTRA_RESULTS_UPDATED indicates if the scan was completed successfully.
                     */
                    // Lookup key for a boolean representing the result of previous startScan() operation
                    /*boolean scanResult = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (scanResult) {
                        handler.obtainMessage(START_SCAN_SUCCEED).sendToTarget();
                    }*/
                    if (handler != null) {
                        handler.obtainMessage(START_SCAN_SUCCEED).sendToTarget();
                    }
                    break;
            }
        }
    }

    private int getStrength(Context context) {
        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getBSSID() != null) {
            //信号强度
            int strength = WifiManager.calculateSignalLevel(info.getRssi(), 8);
            //连接速度
            int speed = info.getLinkSpeed();
            //速度单位
            String units = WifiInfo.LINK_SPEED_UNITS;
            //Wifi名称
            String ssid = info.getSSID();

            Log.d(TAG, "ssid = " + ssid + ", strength = " + strength + ", speed = " + speed + ", units = " + units);
            return strength;
        }
        return 0;
    }

    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }
}
