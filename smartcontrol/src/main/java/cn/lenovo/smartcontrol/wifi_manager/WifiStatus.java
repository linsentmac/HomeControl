package cn.lenovo.smartcontrol.wifi_manager;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by linsen on 17-12-4.
 */

public class WifiStatus implements Parcelable {

    private static WifiStatus mInstance;

    private String wifiName;
    private int wifiLevel;

    public static synchronized WifiStatus getInstance(){
        if(mInstance == null){
            mInstance = new WifiStatus();
        }
        return mInstance;
    }

    WifiStatus(){}

    private WifiStatus(Parcel in) {
        wifiName = in.readString();
        wifiLevel = in.readInt();
    }

    public static final Creator<WifiStatus> CREATOR = new Creator<WifiStatus>() {
        @Override
        public WifiStatus createFromParcel(Parcel in) {
            return new WifiStatus(in);
        }

        @Override
        public WifiStatus[] newArray(int size) {
            return new WifiStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(wifiName);
        dest.writeInt(wifiLevel);
    }

    public synchronized void setWifiInfo(String wifiName, int wifiLevel){
        this.wifiName = wifiName;
        this.wifiLevel = wifiLevel;
    }

    public String getWifiName(){
        return wifiName;
    }

    public int getWifiLevel(){
        return wifiLevel;
    }

}
