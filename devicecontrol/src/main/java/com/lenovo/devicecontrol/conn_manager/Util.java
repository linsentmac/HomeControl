package com.lenovo.devicecontrol.conn_manager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.Html;
import android.util.Log;

import java.util.List;

public class Util {
    public static final String TAG = "Util";

    /**
     * 系统自带默认的确定|取消对话框，全部可控
     */
    public static AlertDialog showAlertDialog(Context context, String title, String message, boolean cancelable, String positiveButtonMessage, String negativeButton, boolean withCancelButton, final ShowDialogInterface showDialogInterface) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Html.fromHtml(title));
        builder.setMessage(Html.fromHtml(message));
        builder.setCancelable(cancelable);
        builder.setPositiveButton(Html.fromHtml(positiveButtonMessage), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDialogInterface.setEnsureButton();
            }
        });
        if (withCancelButton) {
            builder.setNegativeButton(Html.fromHtml(negativeButton), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showDialogInterface.setCancelButton();
                }
            });
        }
        return builder.show();
    }

    /**
     * 系统对话框回调接口
     */
    public interface ShowDialogInterface {
        //确定按钮
        void setEnsureButton();

        //取消按钮
        void setCancelButton();
    }

    //移除网络配置
    public static boolean removeConfiguration(Context context, String ssid) {
        boolean isSuccess = false;
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + ssid + "\"")) {
                mWifiManager.disableNetwork(existingConfig.networkId);
                boolean success = mWifiManager.removeNetwork(existingConfig.networkId);
                mWifiManager.saveConfiguration();
                if (success) isSuccess = true;//有可能匹配到多个，只要一个移除成功就返回true
                Log.e(TAG, "----- Remove network: isSuccess = " + isSuccess + ", networkId = " + existingConfig.networkId + ", SSID = " + ssid + " -----");
            }
        }
        return isSuccess;//遍历整个集合后，还是没有移除成功，返回false
    }
}
