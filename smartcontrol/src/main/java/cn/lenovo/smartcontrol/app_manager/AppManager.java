package cn.lenovo.smartcontrol.app_manager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cn.lenovo.smartcontrol.service.DeviceService;
import cn.lenovo.smartcontrol.utils.FileUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by linsen3 on 2017/12/2.
 */

public class AppManager {

    private static final boolean DBG = true;
    private static final String TAG = "SC-AppManager";
    private OkHttpClient okHttpClient;
    private Context mContext;
    private Handler mHandler;
    private String mCurrentPkgName;
    private Timer mAppTimer;
    private ActivityManager manager;
    private long startAppTime = 0;
    private AppStatus mAppStatus;

    public static final int STATUS_OK = 0;
    public static final int STATUS_FAIL = -1;

    private static final String ATTR_PACKAGE_STATS="PackageStats";


    public AppManager(Context context, Handler handler){
        mContext = context;
        okHttpClient = new OkHttpClient();
        mHandler = handler;
        mAppStatus = AppStatus.getInstance();
        getInstalledApps();
        getCurrentApplicationInfo();
    }

    /**
     * get all application
     */
    public void getInstalledApps() {
        List<PackageInfo> packages = mContext.getPackageManager().getInstalledPackages(0);
        List<Map<String, Object>> listMap = new ArrayList<Map<String,Object>>(packages.size());

        ArrayList<String> app=new ArrayList<String>();
        for (int j = 0; j < packages.size(); j++) {
            Map<String, Object> map = new HashMap<String, Object>();

            PackageInfo packageInfo = packages.get(j);
            //显示非系统软件
            if((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)==0){
                map.put("img", packageInfo.applicationInfo.loadIcon(mContext.getPackageManager()).getCurrent());
                map.put("name", packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString());
                app.add(packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString());
                Log.i(TAG, packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString() + packageInfo.packageName);
                map.put("desc", packageInfo.packageName);
                listMap.add(map);
            }
        }
        for (int i = 0, j = app.size(); i < j; i++) {
            Log.d(TAG, "app name = " + app.get(i));
        }
        // report cloud
        mAppStatus.setAllAppList(listMap);
    }


    /**
     * get current app info
     */
    private void getCurrentApplicationInfo(){
        manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mAppTimer = new Timer();
        mAppTimer.schedule(new CurrentAppTimerTask(), 0, 1000);
    }

    class CurrentAppTimerTask extends TimerTask{

        @Override
        public void run() {
            List<ActivityManager.RunningTaskInfo> runningTask = manager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo runningTaskInfo = runningTask.get(0);
            String packageName = runningTaskInfo.topActivity.getPackageName();
            if(mCurrentPkgName == null){
                mCurrentPkgName = packageName;
                startAppTime = System.currentTimeMillis();
            }else if(packageName.equals(mCurrentPkgName)){
                // report cloud
                long foregroundTime = System.currentTimeMillis() - startAppTime;
                String appName = getProgramNameByPackageName(mCurrentPkgName);
                mAppStatus.setAppForeInfo(appName, foregroundTime);
            }else {
                startAppTime = System.currentTimeMillis();
                mCurrentPkgName = packageName;
            }

        }

    }

    private String getProgramNameByPackageName(String packageName) {
        PackageManager pm = mContext.getPackageManager();
        String name = null;
        try {
            name = pm.getApplicationLabel(
                    pm.getApplicationInfo(packageName,
                            PackageManager.GET_META_DATA)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return name;
    }

    // AppStorageSettings.java
    public void getpkginfo(String pkg){
        /*PackageManager pm = mContext.getPackageManager();
        try {
            Method getPackageSizeInfo = pm.getClass().getMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
            getPackageSizeInfo.invoke(pm, pkg, new PkgSizeObserver());
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }


    /*class PkgSizeObserver extends IPackageStatsObserver.Stub {
        private String pkg;
        private PackageManager pm;
        public PkgSizeObserver(String pkg, PackageManager pm){
            this.pkg = pkg;
            this.pm = pm;
        }

        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
            String appSize = FileUtils.formatFileSize(pStats.codeSize);
            String dataSize = FileUtils.formatFileSize(pStats.dataSize);
            String cacheSize = FileUtils.formatFileSize(pStats.cacheSize);
            boolean isAppStarted = checkForceStop(pm, pkg);
            mAppStatus.setAppDetailInfo(appSize, dataSize, cacheSize, isAppStarted);
        }
    }*/

    public void clearCacheInfo(String packageName){

        //mPm.deleteApplicationCacheFiles(mPackageName, mClearCacheObserver);
    }

    public void clearDataInfo(String packageName){
        // init
        /*if (mClearDataObserver == null) {
            mClearDataObserver = new ClearUserDataObserver();
        }
        ActivityManager am = (ActivityManager)
                getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        boolean res = am.clearApplicationUserData(packageName, mClearDataObserver);*/

        // click clear data
        /*if (mAppEntry.info.manageSpaceActivityName != null) {
            if (!Utils.isMonkeyRunning()) {
                Intent intent = new Intent(Intent.ACTION_DEFAULT);
                intent.setClassName(mAppEntry.info.packageName,
                        mAppEntry.info.manageSpaceActivityName);
                startActivityForResult(intent, REQUEST_MANAGE_SPACE);
            }
        } else {
            showDialogInner(DLG_CLEAR_DATA, 0);
        }*/
    }

    /*class ClearCacheObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            final Message msg = mHandler.obtainMessage(MSG_CLEAR_CACHE);
            msg.arg1 = succeeded ? OP_SUCCESSFUL : OP_FAILED;
            mHandler.sendMessage(msg);
        }
    }

    class ClearUserDataObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            final Message msg = mHandler.obtainMessage(MSG_CLEAR_USER_DATA);
            msg.arg1 = succeeded ? OP_SUCCESSFUL : OP_FAILED;
            mHandler.sendMessage(msg);
        }
    }*/

    // ProcessStatsDetail.java
    public void killProcesses(String packageName){
        ActivityManager am = (ActivityManager)mContext.getSystemService(
                Context.ACTIVITY_SERVICE);
        //am.forceStopPackage(packageName);
    }

    private boolean checkForceStop(PackageManager pm, String packageName){
        boolean isStarted = false;
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            if((info.flags & ApplicationInfo.FLAG_STOPPED) == 0){
                isStarted = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return isStarted;
    }

    public void deleteApk(String packageName){
        /*PackageManager pm = mContext.getPackageManager();
        IPackageDeleteObserver observer = new MyPackageDeleteObserver();
        pm.deletePackage(packageName, observer, 0);*/
    }

    /**
     * downloadapk from the field of SmartCast+
     * @param packageName
     * @param apkUrl
     */
    public void downLoadApk(final String packageName, String apkUrl){
        Request request = new Request.Builder()
                .url(apkUrl)
                .header("Cookie", "channelid=18540")
                .header("User-Info","mfr=Lenovo;model=K910;devid=863664000004555;devidty=imei;osty=android;osver=6.0;userip=125.71.215.92")
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                writeApkToFile(packageName, response);
            }
        });
    }


    private void writeApkToFile(String packageName, Response response){
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        try{
            is = response.body().byteStream();
            int length = (int) response.body().contentLength();
            Log.d(TAG, "length = " + length);
            File rootFile = new File(Environment.getExternalStorageDirectory()+"/SmartShop/");
            if(!rootFile.exists()){
                rootFile.mkdirs();
            }
            File file = new File(rootFile, packageName);
            if(!file.exists()){
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            while ((len = is.read(buf)) != -1)
            {
                fos.write(buf, 0, len);
            }
            fos.flush();
            // write end and install apk
            sendInstallRequestResult(packageName, DeviceService.MSG_STORE_DOWNLOAD_APP, STATUS_OK);
            installApk(file);
        } catch (IOException e){
            e.printStackTrace();
            Log.d(TAG, "error = " + e.getMessage());
        } finally{
            try{
                if (is != null) is.close();
                if (fos != null) fos.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void installApk(File file){
        String[] args = {"pm", "install", "-r", getUriForFile(mContext, file).getPath()};
        final String[] result = {null};
        final ProcessBuilder processBuilder = new ProcessBuilder(args);
        final Process[] process = {null};
        final InputStream[] errIs = {null};
        final InputStream[] inIs = {null};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int read=-1;
                    process[0] = processBuilder.start();
                    errIs[0] = process[0].getErrorStream();
                    while((read = errIs[0].read()) != -1){
                        baos.write(read);
                    }
                    baos.write('\n');
                    inIs[0] = process[0].getInputStream();
                    while((read= inIs[0].read())!=-1){
                        baos.write(read);
                    }
                    byte[] data=baos.toByteArray();
                    result[0] = new String(data);
                } catch (IOException e) {
                    e.printStackTrace();
                    String log = e.toString() + ":" + "file is write ex!";
                    Log.e("", log);
                }
            }
        }).start();
    }

    private Uri getUriForFile(Context context, File file) {
        if (context == null || file == null) {
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(context.getApplicationContext(), "cn.lenovo.smartcontrol.fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    /**
     * send callback result for VideoProvider
     *
     * @param camCmd
     * @param ret
     */
    private void sendInstallRequestResult(String packageName, int camCmd, int ret) {
        Bundle result = new Bundle();
        result.putInt(DeviceService.DOWNLOAD_APP_CMD_RESULT, ret);
        result.putString(DeviceService.APP_NAME, packageName);
        sendResponseToSCService(camCmd, result);
    }

    /**
     * Send The Cmd Result To Service
     *
     * @param cmdId
     * @param result
     */
    private void sendResponseToSCService(int cmdId, Bundle result) {
        Message response = null;
        Intent intent = new Intent(mContext, DeviceService.class);
        intent.putExtra(DeviceService.EXTRA_CMD_ID, DeviceService.MSG_RESPONSE);
        intent.putExtra(DeviceService.EXTRA_CMD_RESPONSE, cmdId);
        intent.putExtra(DeviceService.EXTRA_RST_OBJ, result);
        response = mHandler.obtainMessage(DeviceService.MSG_RESPONSE, 0, 0, intent);
        mHandler.sendMessage(response);
    }

}
