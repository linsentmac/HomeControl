package cn.lenovo.smartcontrol.wifi_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by linsen on 17-12-4.
 */

public class WifiControl {

    private static final boolean DBG = true;
    private static final String TAG = "SC-WifiControl";

    private static Context mContext;
    private static int preLevel;

    public WifiControl(Context context){
        mContext = context;
    }

    public static void getWifiInfo() {
        WifiManager wifiControl = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiControl.getConnectionInfo();
        if (wifiInfo.getBSSID() != null) {
            // wifi name
            String ssid = wifiInfo.getSSID();
            //wifi level
            int signalLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
            //wifi speed
            int speed = wifiInfo.getLinkSpeed();
            if(DBG) Log.d(TAG, "ssid="+ssid+",signalLevel="+signalLevel+",speed="+speed+"Mbps");
            WifiStatus.getInstance().setWifiInfo(ssid, signalLevel);
            if((signalLevel - preLevel) > 10){
                preLevel = signalLevel;
                // report cloud
                WifiStatus.getInstance().setWifiInfo(ssid, signalLevel);
            }
        }
    }

    public static class WifiStateReceiver extends BroadcastReceiver{

        private Context mContext;
        private int preLevel;

        @Override
        public void onReceive(Context context, Intent intent) {
            mContext = context;
            String action = intent.getAction();
            if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                getWifiInfo();
            }
        }

        /*private void getWifiInfo() {
            WifiManager wifiControl = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiControl.getConnectionInfo();
            if (wifiInfo.getBSSID() != null) {
                // wifi name
                String ssid = wifiInfo.getSSID();
                //wifi level
                int signalLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
                //wifi speed
                int speed = wifiInfo.getLinkSpeed();
                if(DBG) Log.d(TAG, "ssid="+ssid+",signalLevel="+signalLevel+",speed="+speed+"Mbps");
                if((signalLevel - preLevel) > 10){
                    preLevel = signalLevel;
                    // report cloud
                    WifiStatus.getInstance().setWifiInfo(ssid, signalLevel);
                }
            }
        }*/
    }

}
