package cn.lenovo.smartcontrol.app_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import cn.lenovo.smartcontrol.service.DeviceService;

/**
 * Created by linsen3 on 2017/12/3.
 */

public class InstallReceiver extends BroadcastReceiver {

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        String packageName = intent.getDataString().split(":")[1];
        if(action.equals(Intent.ACTION_PACKAGE_ADDED)){
            sendInstallRequestResult(packageName, DeviceService.MSG_STORE_INSTALL_APP, AppInstall.STATUS_OK);
        }
    }

    /**
     * send callback result for VideoProvider
     *
     * @param camCmd
     * @param ret
     */
    private void sendInstallRequestResult(String packageName, int camCmd, int ret) {
        Bundle result = new Bundle();
        result.putInt(DeviceService.INSTALL_APP_CMD_RESULT, ret);
        result.putString(DeviceService.APP_NAME, packageName);
        sendInstallReusltToSCService(camCmd, result);
    }

    /**
     * Send The Cmd Result To Service
     *
     * @param cmdId
     * @param result
     */
    private void sendInstallReusltToSCService(int cmdId, Bundle result) {
        Intent serviceIntent = new Intent(mContext, DeviceService.class);
        serviceIntent.putExtra(DeviceService.EXTRA_CMD_ID, DeviceService.MSG_RESPONSE);
        serviceIntent.putExtra(DeviceService.EXTRA_CMD_RESPONSE, cmdId);
        serviceIntent.putExtra(DeviceService.EXTRA_RST_OBJ, result);
        serviceIntent.setPackage("cn.lenovo.smartcontrol.app_manager.InstallReceiver");
        mContext.startService(serviceIntent);
    }
}
