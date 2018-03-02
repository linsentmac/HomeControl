package cn.lenovo.smartcontrol.app_manager;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.FileProvider;
import android.util.Log;

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

import cn.lenovo.smartcontrol.mqtt.MqttManager;
import cn.lenovo.smartcontrol.service.DeviceService;
import cn.lenovo.smartcontrol.utils.FileUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static cn.lenovo.smartcontrol.service.DeviceService.MSG_STORE_DOWNLOAD_PERCENT;

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
    private CurrentAppTimerTask mAppTimerTask;
    private ActivityManager manager;
    private PackageManager pm;
    private long startAppTime = 0;
    private AppStatus mAppStatus;

    public static final int STATUS_OK = 0;
    public static final int STATUS_FAIL = -1;

    private static final String ATTR_PACKAGE_STATS = "PackageStats";


    public AppManager(Context context, Handler handler){
        mContext = context;
        okHttpClient = new OkHttpClient();
        mHandler = handler;
        mAppStatus = AppStatus.getInstance();
        pm = mContext.getPackageManager();
        mAppTimer = new Timer();
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
        mAppTimerTask = new CurrentAppTimerTask();
        mAppTimer.schedule(mAppTimerTask, 0, 1000);
    }

    class CurrentAppTimerTask extends TimerTask{

        @Override
        public void run() {
            List<ActivityManager.RunningTaskInfo> runningTask = manager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo runningTaskInfo = runningTask.get(0);
            String packageName = runningTaskInfo.baseActivity.getPackageName();
            Log.d(TAG, "packageName = " + packageName);
            if(mCurrentPkgName == null){
                mCurrentPkgName = packageName;
                startAppTime = System.currentTimeMillis();
            }else if(packageName.equals(mCurrentPkgName)){
                // report cloud
                long foregroundTime = System.currentTimeMillis() - startAppTime;
                String appName = getProgramNameByPackageName(mCurrentPkgName);
                //Log.d(TAG, "report current app name = " + appName + "../ ..pkgName" + mCurrentPkgName);
                mAppStatus.setAppForeInfo(appName, mCurrentPkgName, foregroundTime);
            }else {
                startAppTime = System.currentTimeMillis();
                mCurrentPkgName = packageName;
            }

            /*UsageStatsManager m = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
            if (m != null) {
                long now = System.currentTimeMillis();
                //获取60秒之内的应用数据
                List<UsageStats> stats = m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 60 * 1000, now);
                Log.i(TAG, "Running app number in last 60 seconds : " + stats.size());

                String topActivity = "";

                //取得最近运行的一个app，即当前运行的app
                if ((stats != null) && (!stats.isEmpty())) {
                    int j = 0;
                    for (int i = 0; i < stats.size(); i++) {
                        if (stats.get(i).getLastTimeUsed() > stats.get(j).getLastTimeUsed()) {
                            j = i;
                        }
                    }
                    topActivity = stats.get(j).getPackageName();
                }
                Log.i(TAG, "top running app is : "+topActivity);
            }*/

        }

    }

    private String getProgramNameByPackageName(String packageName) {
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

    /**
     * get App/Data/Cache Size
     * @param pkg
     */
    public void getpkginfo(String pkg){
        //pm.getPackageSizeInfo(pkg, new PkgSizeObserver(pkg));
        try {
            //通过反射机制获得该隐藏函数
            Method getPackageSizeInfo = pm.getClass().getDeclaredMethod("getPackageSizeInfo", String.class,IPackageStatsObserver.class);
            //调用该函数，并且给其分配参数 ，待调用流程完成后会回调PkgSizeObserver类的函数
            getPackageSizeInfo.invoke(pm, pkg, new PkgSizeObserver(pkg));
        } catch(Exception ex){
            Log.e(TAG, "NoSuchMethodException") ;
            ex.printStackTrace() ;
        }
    }


    class PkgSizeObserver extends IPackageStatsObserver.Stub {
        private String pkg;
        public PkgSizeObserver(String pkg){
            this.pkg = pkg;
        }

        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
            String appSize = FileUtils.formatFileSize(pStats.codeSize);
            String dataSize = FileUtils.formatFileSize(pStats.dataSize);
            String cacheSize = FileUtils.formatFileSize(pStats.cacheSize);
            boolean isAppStarted = checkForceStop(pkg);
            Log.d(TAG, "appSize = " + appSize + "\n"
                    + "dataSize = " + dataSize + "\n"
                    + "cacheSize = " + cacheSize + "\n"
                    + "isAppStarted = " + isAppStarted);
            mAppStatus.setAppDetailInfo(appSize, dataSize, cacheSize, isAppStarted);
        }
    }


    /**
     * clear data File
     */
    private ClearUserDataObserver mClearDataObserver;
    public void clearDataInfo(String packageName){
        // init
        if (mClearDataObserver == null) {
            mClearDataObserver = new ClearUserDataObserver();
        }
        ActivityManager am = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        //boolean res = am.clearApplicationUserData(packageName, mClearDataObserver);

        try {
            //通过反射机制获得该隐藏函数
            Method clearApplicationUserData = am.getClass().getDeclaredMethod("clearApplicationUserData", String.class, IPackageDataObserver.class);
            //调用该函数，并且给其分配参数 ，待调用流程完成后会回调PkgSizeObserver类的函数
            clearApplicationUserData.invoke(am, packageName, mClearDataObserver);
        } catch(Exception ex){
            Log.e(TAG, "NoSuchMethodException") ;
            ex.printStackTrace() ;
        }
    }

    class ClearUserDataObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            Log.d(TAG, packageName + " clear data success " + succeeded);
        }
    }

    /**
     * clear Cache File
     */
    private ClearCacheObserver mClearCacheObserver;
    public void clearCacheInfo(String packageName){
        if (mClearCacheObserver == null) {
            mClearCacheObserver = new ClearCacheObserver();
        }
        //pm.deleteApplicationCacheFiles(packageName, mClearCacheObserver);

        try {
            //通过反射机制获得该隐藏函数
            Method deleteAppCache = pm.getClass().getDeclaredMethod("deleteApplicationCacheFiles", String.class, IPackageDataObserver.class);
            //调用该函数，并且给其分配参数 ，待调用流程完成后会回调PkgSizeObserver类的函数
            deleteAppCache.invoke(pm, packageName, mClearCacheObserver);
        } catch(Exception ex){
            Log.e(TAG, "NoSuchMethodException") ;
            ex.printStackTrace() ;
        }
    }

    class ClearCacheObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            Log.d(TAG, packageName + " clear cache success " + succeeded);
        }
    }

    /**
     * kill app process
     * @param packageName
     */
    public void killProcesses(String packageName){
        ActivityManager am = (ActivityManager)mContext.getSystemService(
                Context.ACTIVITY_SERVICE);
        if(checkForceStop(packageName)){
            //am.forceStopPackage(packageName);

            try {
                //通过反射机制获得该隐藏函数
                Method forceStopPackage = am.getClass().getDeclaredMethod("forceStopPackage", String.class);
                //调用该函数，并且给其分配参数 ，待调用流程完成后会回调PkgSizeObserver类的函数
                forceStopPackage.invoke(am, packageName);
            } catch(Exception ex){
                Log.e(TAG, "NoSuchMethodException") ;
                ex.printStackTrace() ;
            }
        }
    }

    /**
     * check app is running
     * @param packageName
     * @return
     */
    private boolean checkForceStop(String packageName){
        boolean isStarted = false;
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            if((info.flags & ApplicationInfo.FLAG_STOPPED) == 0){
                isStarted = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "app isRunning " + isStarted);
        return isStarted;
    }


    /**
     * uninstall apk
     * @param packageName
     */
    public void deleteApk(String packageName){
        IPackageDeleteObserver observer = new MyPackageDeleteObserver();
        //pm.deletePackage(packageName, observer, 0);

        try {
            //通过反射机制获得该隐藏函数
            Method deletePackage = pm.getClass().getDeclaredMethod("deletePackage", String.class, IPackageDeleteObserver.class, Integer.class);
            //调用该函数，并且给其分配参数 ，待调用流程完成后会回调PkgSizeObserver类的函数
            deletePackage.invoke(pm, packageName, observer, 0);
        } catch(Exception ex){
            Log.e(TAG, "NoSuchMethodException") ;
            ex.printStackTrace() ;
        }
    }

    class MyPackageDeleteObserver extends IPackageDeleteObserver.Stub{
        public void packageDeleted(final String packageName, final int returnCode) {
            Log.d(TAG, packageName + " delete success " + returnCode);
        }
    }



    /**
     * downloadapk from the field of SmartCast+
     * @param packageName
     * @param apkUrl
     */
    public void downLoadApk(final String packageName, String apkUrl){
        Log.d(TAG, "downLoadApk ............");
        Request request = new Request.Builder()
                .url(apkUrl)
                .header("Cookie", "channelid=18540")
                .header("User-Info","mfr=Lenovo;model=K910;devid=863664000004555;devidty=imei;osty=android;osver=6.0;userip=125.71.215.92")
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "http request failure " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "http request success ......");
                writeApkToFile(packageName, response);
            }
        });
    }


    private final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory()+"/SmartControl/";
    private void writeApkToFile(String packageName, Response response){
        Log.d(TAG, "writeApkToFile ......");
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        try{
            is = response.body().byteStream();
            int length = (int) response.body().contentLength();
            Log.d(TAG, "length = " + length);
            File rootFile = new File(DOWNLOAD_PATH);
            if(!rootFile.exists()){
                rootFile.mkdirs();
            }
            File file = new File(rootFile, packageName);
            if(!file.exists()){
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            int total = 0;
            int prePrecent = 0;
            while ((len = is.read(buf)) != -1)
            {
                fos.write(buf, 0, len);
                total += len;
                int percent = total/(length/100);
                if(percent > (prePrecent + 10)){
                    sendInstallRequestResult(packageName, MSG_STORE_DOWNLOAD_PERCENT, percent);
                    prePrecent = percent;
                }

            }
            fos.flush();
            Log.d(TAG, "download Success , Start install apk");
            // write end and install apk
            sendInstallRequestResult(packageName, DeviceService.MSG_STORE_DOWNLOAD_APP, STATUS_OK);
            //installApk(file);
            installSilentWithReflection(mContext, file);
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

    public void installSilentWithReflection(final Context context, File apkFile) {
        try {
            PackageManager packageManager = context.getPackageManager();
            Method method = packageManager.getClass().getDeclaredMethod("installPackage",
                    new Class[] {Uri.class, IPackageInstallObserver.class, int.class, String.class} );
            method.setAccessible(true);
            Uri apkUri = Uri.fromFile(apkFile);
            Log.d(TAG, "apkUri = " + apkUri + " /package = " + apkFile.getName());
            method.invoke(packageManager, new Object[] {apkUri, new IPackageInstallObserver.Stub() {
                @Override
                public void packageInstalled(String pkgName, int resultCode) throws RemoteException {
                    Log.d(TAG, "packageInstalled = " + pkgName + "; resultCode = " + resultCode);
                    sendInstallRequestResult(pkgName, DeviceService.MSG_STORE_INSTALL_APP, STATUS_OK);
                    deleteInstallPackage(pkgName);
                }
            }, Integer.valueOf(2), apkFile.getName()});
            Log.d(TAG, "install ending ......");
            //PackageManager.INSTALL_REPLACE_EXISTING = 2;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.d(TAG, "NoSuchMethodException " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "install error " + e.getMessage());
        }
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

    public void deleteInstallPackage(String packageName){
        //String[] sourceStrArray = packageName.split(":");
        File file = new File(DOWNLOAD_PATH, packageName);
        try {
            if(file.exists()){
                file.delete();
                Log.d(TAG, "delete success");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * send callback result for AppManager
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

    public void releaseAppTask(){
        if(mAppTimerTask != null){
            mAppTimerTask.cancel();
            mAppTimerTask = null;
        }
        if(mAppTimer != null){
            mAppTimer.cancel();
            mAppTimer.purge();
            mAppTimer = null;
        }
    }

}
