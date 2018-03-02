package cn.lenovo.smartcontrol.service;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import cn.lenovo.smartcontrol.app_manager.AppStatus;
import cn.lenovo.smartcontrol.device.SysStatus;
import cn.lenovo.smartcontrol.wifi_manager.WifiStatus;

/**
 * Created by linsen on 17-12-4.
 */

public class DeviceStatus implements Parcelable {

    private static final String TAG = "SC-DeviceStatus";

    private static Context mContext;
    private static DeviceStatus mInstance;


    public static WifiStatus wifi_Status = WifiStatus.getInstance();
    public static AppStatus app_Status = AppStatus.getInstance();
    public static SysStatus sys_Status;


    public static synchronized DeviceStatus getInstance(Context context){
        if(mInstance == null){
            mInstance = new DeviceStatus(context);
        }
        return mInstance;
    }

    DeviceStatus(Context context){
        mContext = context;
        sys_Status = SysStatus.getInstance(mContext);
    }

    protected DeviceStatus(Parcel in) {
        wifi_Status = in.readParcelable(null);
        app_Status = in.readParcelable(null);
    }

    public static final Creator<DeviceStatus> CREATOR = new Creator<DeviceStatus>() {
        @Override
        public DeviceStatus createFromParcel(Parcel in) {
            return new DeviceStatus(in);
        }

        @Override
        public DeviceStatus[] newArray(int size) {
            return new DeviceStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(wifi_Status, flags);
        dest.writeParcelable(app_Status, flags);
    }

    public static String toStatusJson(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(DeviceService.REPORT_MSG_TYPE, DeviceService.REPORT_DEVICE_MSG);
            jsonObject.put("lenovoID", sys_Status.getLenovoID());
            jsonObject.put("deviceName", "SmartCast+");
            jsonObject.put("deviceID", sys_Status.getDeviceID());
            jsonObject.put("batteryLevel", sys_Status.getBatteryLevel());
            jsonObject.put("isCharge", sys_Status.isCharging());
            jsonObject.put("versionName", sys_Status.getVersionName());
            jsonObject.put("versionBuildTime", sys_Status.getVersionBuildTime());
            jsonObject.put("wifiName", wifi_Status.getWifiName());
            jsonObject.put("wifiLevel", wifi_Status.getWifiLevel());
            jsonObject.put("foreAppName", app_Status.getCurrentAppName());
            jsonObject.put("forePkgName", app_Status.getCurrentPkgName());
            jsonObject.put("foreAppTime", app_Status.getAppforeTime());
            jsonObject.put("currentTime", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, jsonObject.toString());
        return jsonObject.toString();
    }


}
