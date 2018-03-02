package cn.lenovo.smartcontrol.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lenovo.lsf.lenovoid.LOGIN_STATUS;
import com.lenovo.lsf.lenovoid.LenovoIDApi;

import cn.lenovo.smartcontrol.service.DeviceService;

/**
 * Created by linsen on 18-1-6.
 */

public class SCBroadCastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            LOGIN_STATUS status = LenovoIDApi.getStatus(context);

            if (status == LOGIN_STATUS.ONLINE) {
                context.startService(new Intent(context, DeviceService.class));
            }
        }
    }
}
