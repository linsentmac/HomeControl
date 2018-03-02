package cn.lenovo.smartcontrol.device;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by linsen on 17-12-6.
 */

public class SysStatus implements Parcelable {

    private static String TAG = "SC-SysStatus";
    private static SysStatus mInstance;
    private Context mContext;

    private String lenovoID;
    private String deviceID;
    private int batteryLevel;
    private boolean isCharging;

    public static SysStatus getInstance(Context context){
        Log.d(TAG, "mInstance = " + mInstance);
        if(mInstance == null){
            mInstance = new SysStatus(context);
        }
        return mInstance;
    }

    SysStatus(Context context){
        mContext = context;
    }

    protected SysStatus(Parcel in) {
        lenovoID = in.readString();
        deviceID = in.readString();
        batteryLevel = in.readInt();
        isCharging = in.readInt() == 1 ? true : false;
    }

    public static final Creator<SysStatus> CREATOR = new Creator<SysStatus>() {
        @Override
        public SysStatus createFromParcel(Parcel in) {
            return new SysStatus(in);
        }

        @Override
        public SysStatus[] newArray(int size) {
            return new SysStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(lenovoID);
        dest.writeString(deviceID);
        dest.writeInt(batteryLevel);
        dest.writeInt(isCharging() ? 1 : 0);
    }

    public String getDeviceID(){
        deviceID = Settings.Secure.getString(
                mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG, "deviceID = " + deviceID);
        return deviceID;
    }

    public void setBatteryInfo(int batteryLevel, boolean isCharging){
        this.batteryLevel = batteryLevel;
        this.isCharging = isCharging;
    }

    public int getBatteryLevel(){
        return batteryLevel;
    }

    public boolean isCharging(){
        return isCharging;
    }

    public void setLenovoID(String lenovoID){
        this.lenovoID = lenovoID;
    }

    public String getLenovoID(){
        return lenovoID;
    }

    public String getVersionName(){
        return Build.DISPLAY;
    }

    public long getVersionBuildTime(){
        return Build.TIME;
    }

}
