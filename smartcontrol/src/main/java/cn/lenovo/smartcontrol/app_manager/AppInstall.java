package cn.lenovo.smartcontrol.app_manager;

import android.content.Context;
import android.content.Intent;
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

import cn.lenovo.smartcontrol.service.DeviceService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by linsen3 on 2017/12/2.
 */

public class AppInstall {

    private static final boolean DBG = true;
    private static final String TAG = "SC-AppInstall";
    private OkHttpClient okHttpClient;
    private Context mContext;
    private Handler mHandler;
    private String mPackageName;

    public static final int STATUS_OK = 0;
    public static final int STATUS_FAIL = -1;


    public AppInstall(Context context, Handler handler){
        mContext = context;
        okHttpClient = new OkHttpClient();
        mHandler = handler;

    }

    public void downLoadApk(final String packageName, String apkUrl){
        mPackageName = packageName;
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
            sendInstallRequestResult(packageName, DeviceService.MSG_STARE_DOWNLOAD_APP, STATUS_OK);
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
