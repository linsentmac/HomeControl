package com.lenovo.devicecontrol.conn_manager;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * WiFi连接检测是否需要portal认证
 * Created by chao on 2017/3/17.
 */
class CheckWifiLoginTask extends AsyncTask<Integer, Integer, Boolean> {
    private ICheckWifiCallBack mCallBack;

    private CheckWifiLoginTask(ICheckWifiCallBack mCallBack) {
        super();
        this.mCallBack = mCallBack;
    }

    @Override
    protected Boolean doInBackground(Integer... params) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return isWifiSetPortal();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (mCallBack != null) {
            mCallBack.portalNetWork(result);
        }
    }

    /**
     * 验证当前wifi是否需要Portal验证
     */
    private boolean isWifiSetPortal() {
        String mWalledGardenUrl = "http://g.cn/generate_204";
        int WALLED_GARDEN_SOCKET_TIMEOUT_MS = 10000;// 设置请求超时
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(mWalledGardenUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setConnectTimeout(WALLED_GARDEN_SOCKET_TIMEOUT_MS);
            urlConnection.setReadTimeout(WALLED_GARDEN_SOCKET_TIMEOUT_MS);
            urlConnection.setUseCaches(false);
            urlConnection.getInputStream();
            int responseCode = urlConnection.getResponseCode();
            boolean need = responseCode != 204;// 判断返回状态码是否204
            Log.e("CheckWifiLoginTask", "----- 验证当前WiFi是否需要Portal验证：code = " + responseCode + ", need = " + need + " -----");
            return need;
        } catch (IOException e) {
            Log.e("CheckWifiLoginTask", e.toString());
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();//释放资源
            }
        }
    }

    /**
     * 检测Wifi 是否需要portal 认证
     */
    static void checkWifi(ICheckWifiCallBack callBack) {
        new CheckWifiLoginTask(callBack).execute();
    }

    interface ICheckWifiCallBack {
        void portalNetWork(boolean isLogin);
    }
}
