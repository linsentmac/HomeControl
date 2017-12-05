package cn.lenovo.smartcontrol.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import cn.lenovo.smartcontrol.app_manager.AppStatus;
import cn.lenovo.smartcontrol.wifi_manager.WifiStatus;

/**
 * Created by linsen on 17-12-4.
 */

public class DeviceStatus implements Parcelable {

    public static WifiStatus wifi_Status = WifiStatus.getInstance();
    public static AppStatus app_Status = AppStatus.getInstance();

    private static DeviceStatus mInstance;

    public static synchronized DeviceStatus getInstance(){
        if(mInstance == null){
            mInstance = new DeviceStatus();
        }
        return mInstance;
    }

    DeviceStatus(){}

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
            jsonObject.put("wifiName", wifi_Status.getWifiName());
            jsonObject.put("wifiLevel", wifi_Status.getWifiLevel());
            jsonObject.put("foreAppName", app_Status.getAppName());
            jsonObject.put("foreAppTime", app_Status.getAppforeTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }


}
