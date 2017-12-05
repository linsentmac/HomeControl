package cn.lenovo.smartcontrol.app_manager;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by linsen on 17-12-5.
 */

public class AppStatus implements Parcelable {

    private String appName;
    private long appforeTime;
    private List<Map<String, Object>> appList;
    private String appSize;
    private String dataSize;
    private String cacheSize;
    private boolean isAppStarted;

    private static AppStatus mInstance;

    public static AppStatus getInstance(){
        if(mInstance == null){
            mInstance = new AppStatus();
        }
        return mInstance;
    }

    AppStatus(){
        appList = new ArrayList<>();
    }

    protected AppStatus(Parcel in) {
        appName = in.readString();
        appforeTime = in.readLong();
        appList = in.readArrayList(null);
        appSize = in.readString();
        dataSize = in.readString();
        cacheSize = in.readString();
        isAppStarted = in.readInt() == 1 ? true : false;
    }

    public static final Creator<AppStatus> CREATOR = new Creator<AppStatus>() {
        @Override
        public AppStatus createFromParcel(Parcel in) {
            return new AppStatus(in);
        }

        @Override
        public AppStatus[] newArray(int size) {
            return new AppStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(appName);
        dest.writeLong(appforeTime);
        dest.writeList(appList);
        dest.writeString(appSize);
        dest.writeString(dataSize);
        dest.writeString(cacheSize);
        dest.writeInt(isAppStarted ? 1 : 0);
    }

    public void setAppForeInfo(String appName, long appforeTime){
        this.appName = appName;
        this.appforeTime = appforeTime;
    }

    public String getAppName(){
        return appName;
    }

    public long getAppforeTime(){
        return appforeTime;
    }

    public void setAllAppList(List<Map<String, Object>> appList){
        this.appList = appList;
    }

    public List<Map<String, Object>> getAppList(){
        return appList;
    }

    public void setAppDetailInfo(String appSize, String dataSize, String cacheSize, boolean isAppStarted){
        this.appSize = appSize;
        this.dataSize = dataSize;
        this.cacheSize = cacheSize;
        this.isAppStarted = isAppStarted;
    }

    public String getAppSize(){
        return appSize;
    }

    public String getDataSize(){
        return dataSize;
    }

    public String getCacheSize(){
        return cacheSize;
    }

    public boolean isAppStarted(){
        return isAppStarted;
    }

}
