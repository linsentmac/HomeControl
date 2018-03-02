package cn.lenovo.smartcontrol.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import cn.lenovo.smartcontrol.device.SysStatus;

/**
 * Created by linsen on 17-12-6.
 */

public class AndroidSystemReceiver extends BroadcastReceiver {

    private static final String TAG = "AndroidSystemReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_BATTERY_CHANGED)){
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean ischarging;
            int level = intent.getIntExtra("level", 0);
            //ischanging = ((level == FULL_BATTERY) && (intent.getBooleanExtra("EXTRA_USB_HW_DISCONNECTED", false))) ? true : ((status == BatteryManager.BATTERY_STATUS_CHARGING) ? true : false);
            ischarging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            Log.d(TAG, "battryLevel = " + level + " status = " + status + " ischanging = " + ischarging);
            SysStatus.getInstance(context).setBatteryInfo(level, ischarging);
        }

    }
}
