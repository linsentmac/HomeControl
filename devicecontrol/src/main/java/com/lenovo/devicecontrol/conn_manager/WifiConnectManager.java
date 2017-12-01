package com.lenovo.devicecontrol.conn_manager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Wifi连接管理类
 * Created by linsen on 2017/11/6.
 */
public class WifiConnectManager {
    private String TAG = "WifiConnectManager";
    private WifiManager wifiManager;
    public static long startConnectTime;//连接时记录的时间点
    public static String connectingSSID;//正在连的ssid，自动连接密码错时根据此id遗忘密码，防止无限重复连接
    static int enableNetworkNum;//实际发现每次enableNetwork都会触发2次NETWORK_STATE_CHANGED_ACTION广播并且状态还是Connected，所以要过滤掉此次数（可能是注册了多个广播接收器的缘故）

    private Context mContext;
    private WifiConnectionStateListener mWifiListener;
    private WifiConnectBroadCast wifiConnectReceiver;

    // 定义几种加密方式，一种是WEP，一种是PSK，还有没有密码的情况
    private enum WifiCipherType {
        SECURITY_WEP, SECURITY_PSK, SECURITY_NONE
    }

    public static WifiConnectManager newInstance(Context mContext, WifiConnectionStateListener mWifiListener) {
        return new WifiConnectManager(mContext, mWifiListener);
    }

    // 构造函数
    private WifiConnectManager(Context mContext, WifiConnectionStateListener mWifiListener) {
        this.mContext = mContext;
        this.mWifiListener = mWifiListener;
        wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //注册网络监听
        wifiConnectReceiver = new WifiConnectBroadCast(mWifiHandler);
        mContext.registerReceiver(wifiConnectReceiver, wifiConnectReceiver.getDefaultFilter());
    }

    // 连接网络
    public void connectToWifi(final ScanResult scanResult, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - startConnectTime < 3000 || scanResult == null || password == null)
                    return;
                startConnectTime = System.currentTimeMillis();
                connectingSSID = scanResult.SSID;

                //已连上不必重连
                String ssid = getCurrSSID();
                if (!TextUtils.isEmpty(ssid) && ssid.equals(scanResult.SSID)) {
                    Log.e(TAG, "----- Already connected, please wait: ssid = " + ssid + " -----");
                    return;
                }
                Log.e(TAG, "----- Waiting to connect to the network: SSID = " + scanResult.SSID + ", password = " + password + " -----");

                //WiFi是否打开
                if (openWifi()) {
                    WifiConfiguration wifiConfig = getWifiConfiguration(scanResult);
                    //如果存在配置过的信息，并且类型不为无密码，直接连接
                    if (wifiConfig != null && getSecurity(scanResult) != WifiCipherType.SECURITY_NONE) {
                        boolean enable = connectByExistConfig(wifiConfig);
                        if (!enable) {
                            Log.e(TAG, "----- WARNING: enableNetwork = false, so create new WifiConfiguration! -----");
                            WifiConfiguration newWifiConfig = createWifiConfiguration(scanResult, password);
                            connectByNewConfig(newWifiConfig);
                        }
                    } else {
                        //如果不存在配置信息，或为无密码类型，则创建一个新的配置信息
                        WifiConfiguration newWifiConfig = createWifiConfiguration(scanResult, password);
                        connectByNewConfig(newWifiConfig);
                    }
                }
                //WIFI没有打开，正在打开WIFI（需要1~3秒）
                else {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "----- After 5 seconds later, reconnect... -----");
                    connectToWifi(scanResult, password);
                }
            }
        }).start();
    }

    //使用存在的配置连接
    private boolean connectByExistConfig(WifiConfiguration wifiConfig) {
        boolean enable = wifiManager.enableNetwork(wifiConfig.networkId, true);
        if (enable) {
            enableNetworkNum = 2;
            Log.e(TAG, "----- Exist WifiConfiguration, wifiManager.enableNetwork = true" + ", networkId = " + wifiConfig.networkId + " -----");
        }
        return enable;
    }

    //使用新的配置连接
    private void connectByNewConfig(WifiConfiguration newWifiConfig) {
        int networkId = wifiManager.addNetwork(newWifiConfig);
        boolean enable = wifiManager.enableNetwork(networkId, true);
        if (enable) enableNetworkNum = 2;
        Log.e(TAG, "----- Create new WifiConfiguration, wifiManager.enableNetwork = " + enable + ", networkId = " + networkId + " -----");
    }

    //判断配置信息是否存在，存在返回配置信息，不存在返回null
    private WifiConfiguration getWifiConfiguration(ScanResult scanResult) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + scanResult.SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    //创建一个新的Wifi配置信息
    private WifiConfiguration createWifiConfiguration(ScanResult scanResult, String password) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + scanResult.SSID + "\"";
        //config.BSSID = scanResult.BSSID;
        //setMaxPriority(config);//设置优先级

        WifiCipherType type = getSecurity(scanResult);//得到网络类型
        switch (type) {
            case SECURITY_NONE:// 无密码
                config.wepKeys[0] = "\"\"";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;

            case SECURITY_WEP:// WEP
                int passwordLen = password == null ? 0 : password.length();
                if ((passwordLen == 10 || passwordLen == 26 || passwordLen == 58) && password.matches("[0-9A-Fa-f]*")) {
                    config.wepKeys[0] = password;
                } else {
                    config.wepKeys[0] = "\"" + password + "\"";
                }

                config.hiddenSSID = true;
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;

            case SECURITY_PSK:// WPA_PSK
                int passwordLength = password == null ? 0 : password.length();
                if (passwordLength != 0) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = "\"" + password + "\"";
                    }
                }

                config.hiddenSSID = true;
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);  
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.status = WifiConfiguration.Status.ENABLED;
                break;

            default:
                break;
        }
        return config;
    }

    //通过扫描结果ScanResult得到安全类型
    private WifiCipherType getSecurity(ScanResult result) {
        if (result.capabilities.toUpperCase().contains("WEP")) {
            return WifiCipherType.SECURITY_WEP;
        } else if (result.capabilities.toUpperCase().contains("PSK")) {
            return WifiCipherType.SECURITY_PSK;
        }
        return WifiCipherType.SECURITY_NONE;
    }

    // WiFi是否打开。未打开返回false，并执行打开操作
    private boolean openWifi() {
        if (!wifiManager.isWifiEnabled()) {
            int wifiStatus = wifiManager.getWifiState();
            if (wifiStatus != WifiManager.WIFI_STATE_ENABLED && wifiStatus != WifiManager.WIFI_STATE_ENABLING) {
                wifiManager.setWifiEnabled(true);
                Log.e(TAG, "----- Wifi is not open, waiting to open it... -----");
            }
            return false;//WIFI没有打开，正在打开WIFI
        }
        return true;//WIFI已经打开
    }

    //设置优先级
    private WifiConfiguration setMaxPriority(WifiConfiguration config) {
        int priority = getMaxPriority() + 1;
        if (priority > 99999) {
            priority = shiftPriorityAndSave();
        }

        config.priority = priority;
        wifiManager.updateNetwork(config);

        return config;
    }

    private int getMaxPriority() {
        List<WifiConfiguration> localList = wifiManager.getConfiguredNetworks();
        int i = 0;
        Iterator<WifiConfiguration> localIterator = localList.iterator();
        while (true) {
            if (!localIterator.hasNext()) return i;
            WifiConfiguration localWifiConfiguration = localIterator.next();
            if (localWifiConfiguration.priority <= i) continue;
            i = localWifiConfiguration.priority;
        }
    }

    private int shiftPriorityAndSave() {
        List<WifiConfiguration> localList = wifiManager.getConfiguredNetworks();
        sortByPriority(localList);
        int i = localList.size();
        for (int j = 0; ; ++j) {
            if (j >= i) {
                wifiManager.saveConfiguration();
                return i;
            }
            WifiConfiguration localWifiConfiguration = localList.get(j);
            localWifiConfiguration.priority = j;
            this.wifiManager.updateNetwork(localWifiConfiguration);
        }
    }

    private void sortByPriority(List<WifiConfiguration> paramList) {
        Collections.sort(paramList, new SjrsWifiManagerCompare());
    }

    private class SjrsWifiManagerCompare implements Comparator<WifiConfiguration> {
        public int compare(WifiConfiguration paramWifiConfiguration1, WifiConfiguration paramWifiConfiguration2) {
            return paramWifiConfiguration1.priority - paramWifiConfiguration2.priority;
        }
    }

    //得到当前连接的WIFI名称
    public String getCurrSSID() {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null) return null;
        String ssid = info.getSSID();
        if (ssid.equals("0x") || ssid.equals("<unknown ssid>")) return null;
        return ssid.replace("\"", "");
    }


    // WiFi是否打开。未打开返回false，并执行打开操作
    private boolean isWifiEnable() {
        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            int wifiStatus = wifiManager.getWifiState();
            if (wifiStatus != WifiManager.WIFI_STATE_ENABLED && wifiStatus != WifiManager.WIFI_STATE_ENABLING) {
                wifiManager.setWifiEnabled(true);
                Log.e(TAG, "----- Wifi is not open, waiting to open it... -----");
            }
            return false;// WIFI没有打开，正在打开WIFI
        }
        return wifiManager != null;// WIFI已经打开
    }

    // 定位是否打开，6.0以上扫描WiFi需开启定位
    public boolean locationIsOPen(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = false;
        boolean network = false;
        if (locationManager != null) {
            gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        return gps || network;
    }

    private final int SCAN_WIFI = 0;
    private boolean needAuthentication = false;// 是否需要去验证网页认证
    private ArrayList<ScanResult> scanResultList = new ArrayList<>();
    private ScanResult mScanResult;
    private Handler mWifiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SCAN_WIFI:
                    wifiManager.startScan();
                    Log.e(TAG, "----- Start scan WIFI... -----");
                    mWifiHandler.removeMessages(SCAN_WIFI);
                    sendEmptyMessageDelayed(SCAN_WIFI, 8 * 1000);
                    break;

                case WifiConnectBroadCast.CONNECT_WIFI_RESULT:
                    String ssid = (String) msg.obj;

                    // 专门针对无密码的WIFI，判断是否需要认证，需要则提示用户
                    if (needAuthentication && !TextUtils.isEmpty(ssid) && ssid.equals(connectName)) {
                        needAuthentication = false;
                        CheckWifiLoginTask.checkWifi(new CheckWifiLoginTask.ICheckWifiCallBack() {
                            @Override
                            public void portalNetWork(boolean isLogin) {
                                if (isLogin) {//是网页认证的WIFI
                                    Util.showAlertDialog(mContext, "提示", connectName + "需要登陆认证，暂不支持此类WiFi！", false, "关闭", "取消", false, new Util.ShowDialogInterface() {
                                        @Override
                                        public void setEnsureButton() {

                                        }

                                        @Override
                                        public void setCancelButton() {
                                        }
                                    });
                                }
                            }
                        });
                    } else if (!TextUtils.isEmpty(ssid) && ssid.equals(connectName)) {
                        // wifi connect success
                        Log.d(TAG, "connect success");
                        mWifiListener.onWifiConnected();
                        Toast.makeText(mContext, connectName + "已连接", Toast.LENGTH_LONG).show();
                    }
                    break;

                case WifiConnectBroadCast.PASSWORD_ERROR://正在获得IP地址，验证错误
                    Log.d(TAG, "Password wrong");

                    // 让手机系统遗忘掉此网络
                    boolean success = Util.removeConfiguration(mContext, connectName);
                    if (success) {
                        Toast.makeText(mContext, "密码错误", Toast.LENGTH_LONG).show();
                    } else {// 6.0有移除不成功的情况，必需去系统设置中移除
                        Toast.makeText(mContext, "此网络配置存在问题，需要修复！", Toast.LENGTH_LONG).show();
                        Util.showAlertDialog(mContext, "提示", "此网络配置存在问题！请到手机WiFi设置中删除此网络，再打开APP使用正确密码连接！", false, "前往", "取消", false, new Util.ShowDialogInterface() {
                            @Override
                            public void setEnsureButton() {
                                mContext.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));//跳转网络设置页
                            }

                            @Override
                            public void setCancelButton() {
                            }
                        });
                    }
                    break;

                case WifiConnectBroadCast.WIFI_NETWORK_STATE:
                    break;

                case WifiConnectBroadCast.WIFI_STATE:
                    break;

                case WifiConnectBroadCast.START_SCAN_SUCCEED: // 扫描结束
                    Log.e(TAG, "扫描结束");
                    List<ScanResult> list = wifiManager.getScanResults();
                    Collections.sort(list, new Comparator<ScanResult>() {
                        @Override
                        public int compare(ScanResult lhs, ScanResult rhs) {
                            int wifiLevel = WifiManager.calculateSignalLevel(lhs.level, 8);
                            int wifiLevel2 = WifiManager.calculateSignalLevel(rhs.level, 8);
                            if (wifiLevel > wifiLevel2) {
                                return -1;
                            } else if (wifiLevel < wifiLevel2) {
                                return 1;
                            }
                            return 0;
                        }
                    });

                    scanResultList.clear();
                    for (ScanResult item : list) {
                        boolean has = false;
                        for (int i = 0; i < scanResultList.size(); i++) {
                            ScanResult scanresult = scanResultList.get(i);
                            if (item.SSID.equals(scanresult.SSID)) {
                                has = true;
                                break;
                            }
                        }

                        if (!has) scanResultList.add(item);
                    }
                    if(mScanListener != null){
                        mScanListener.onScanResult(scanResultList);
                        //removeMessages(SCAN_WIFI);
                    }
                    for(ScanResult result : scanResultList){
                        if(result.SSID.equals(connectName)){
                            removeMessages(SCAN_WIFI);
                            startConnect(scanResultList);
                        }
                    }
                    break;
            }
        }
    };

    private String connectName;
    private String Password;
    public void connectWifi(String SSID, String Password){
        connectName = SSID;
        this.Password = Password;
        startScan();
    }

    public void startScan(){
        if (isWifiEnable()) {
            mWifiHandler.sendEmptyMessage(SCAN_WIFI);
        } else {
            Toast.makeText(((Activity)mContext), "WiFi is not open, waiting to open it...", Toast.LENGTH_LONG).show();
        }
    }

    private void startConnect(ArrayList<ScanResult> scanResultList){
        if (TextUtils.isEmpty(connectName)) {
            Toast.makeText(mContext, "连接名称为空", Toast.LENGTH_LONG).show();
            return;
        }

        for (ScanResult result : scanResultList) {
            if (result.SSID != null && result.SSID.equals(connectName)) {
                mScanResult = result;
                break;
            }
        }
        if (mScanResult == null) {
            Toast.makeText(mContext, "没有找到要连接的SSID", Toast.LENGTH_LONG).show();
            return;
        }

        //判断网络类型(WEP、PSK之外的算作无密码、需认证处理)
        String capability = mScanResult.capabilities.toUpperCase();
        Log.e(TAG, "----- WifiName = " + mScanResult.SSID + ", capability = " + capability + ", level = " + mScanResult.level + " -----");
        if (!capability.contains("WEP") && !capability.contains("PSK")) {
            Password = "";
            needAuthentication = true;
        }

        //进入连接流程
        mWifiHandler.removeMessages(SCAN_WIFI);// 停止扫描
        connectToWifi(mScanResult, Password);
        Toast.makeText(mContext, "正在连接" + connectName + "请稍后...", Toast.LENGTH_LONG).show();
    }

    public void unRegisterReceiver(){
        mContext.unregisterReceiver(wifiConnectReceiver);
    }

    public interface ScanResultListener{
        void onScanResult(ArrayList<ScanResult> scanResults);
    }

    private ScanResultListener mScanListener;
    public void setScanResultListener(ScanResultListener listener){
        this.mScanListener = listener;
    }

}
