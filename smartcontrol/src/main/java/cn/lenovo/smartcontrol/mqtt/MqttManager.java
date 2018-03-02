package cn.lenovo.smartcontrol.mqtt;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.lenovo.lsf.lenovoid.LenovoIDApi;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cn.lenovo.smartcontrol.LenovoId.LoginIdActivity;
import cn.lenovo.smartcontrol.device.SysStatus;
import cn.lenovo.smartcontrol.service.DeviceService;
import cn.lenovo.smartcontrol.service.DeviceStatus;

import static cn.lenovo.smartcontrol.service.DeviceService.APP_ADDRESS;
import static cn.lenovo.smartcontrol.service.DeviceService.APP_NAME;
import static cn.lenovo.smartcontrol.service.DeviceService.MSG_STORE_INSTALL_APP;

/**
 * Created by linsen on 18-1-5.
 */

public class MqttManager {

    private static final String TAG = "SC-MqttManager";

    private String serverUri = "tcp://120.27.213.153:1883";
    public String subscriptionTopic;
    public static String publishTopic;
    public static String appStoreTopic = "APP/STORE/STATUS";
    private String ONLINETopic = "$SYS/NOTICE/STATUS/ONLINE";
    private String OFFLINETopic = "$SYS/NOTICE/STATUS/OFFLINE";
    private MqttAndroidClient mqttAndroidClient;
    private String clientId;

    private static MqttManager mInstance;
    private Context mContext;
    private Handler mDeviceHandler;

    private MqttManager(Context context, Handler handler){
        mContext = context;
        mDeviceHandler = handler;
        clientId = DeviceStatus.sys_Status.getDeviceID();
        subscriptionTopic = LenovoIDApi.getUserName(mContext) + "@Smartcast";
        publishTopic = "Smartcast@" + LenovoIDApi.getUserName(mContext);
        initConnect();
    }

    public static MqttManager getInstance(Context context, Handler handler){
        if(mInstance == null){
            mInstance = new MqttManager(context, handler);
        }
        return mInstance;
    }

    private void initConnect(){
        mqttAndroidClient = new MqttAndroidClient(mContext.getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                SERVER_URL = serverURI;
                if (reconnect) {
                    //addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                    reportLenovoID();
                    reportDeviceStatus();
                    mHandler.sendEmptyMessage(RECONNECT);
                } else {
                    //addToHistory("Connected to: " + serverURI);
                    mHandler.sendEmptyMessage(CONNECT);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //addToHistory("The Connection was lost.");
                releaseTimerTask();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //addToHistory("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);


        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Mqtt connect success");
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                    reportLenovoID();
                    //reportDeviceStatus();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //addToHistory("Failed to connect to: " + serverUri);
                    Log.d(TAG, "Mqtt connect onFailure");
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mHandler.sendEmptyMessage(SUBSCRIBED);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    mHandler.sendEmptyMessage(SUBSCRIBED_FAILED);
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
                    String msg = new String(message.getPayload());
                    JSONObject jsonObject = new JSONObject(msg);
                    sendSubscribeMsg(jsonObject);
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void sendSubscribeMsg(JSONObject jsonObject){
        try {
            int cmd = jsonObject.getInt("msgType");
            switch (cmd){
                case MSG_STORE_INSTALL_APP:
                    sendCmdToSCService(jsonObject.getString(APP_NAME), jsonObject.getString(APP_ADDRESS));
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send The Cmd To Service
     *
     * @param packageName
     * @param apkUrl
     */
    private void sendCmdToSCService(String packageName, String apkUrl) {
        Log.d(TAG, "sendCmdToSCService : pkg = " + packageName + "\n" + "apkUrl = " + apkUrl);
        Bundle result = new Bundle();
        result.putString(DeviceService.APP_ADDRESS, apkUrl);
        result.putString(APP_NAME, packageName);
        Message message = null;
        Intent intent = new Intent(mContext, DeviceService.class);
        intent.putExtra(DeviceService.EXTRA_CMD_OBJ, result);
        message = mDeviceHandler.obtainMessage(DeviceService.MSG_STORE_INSTALL_APP, 0, 0, intent);
        mDeviceHandler.sendMessage(message);
    }


    private static final int CONNECT = 0;
    private static final int RECONNECT = 1;
    private static final int CONNECTIONLOST = 2;
    private static final int MSGARRIVED = 3;
    private static final int CONN_FAILED = 4;
    private static final int PUBLISH = 5;
    private static final int SUBSCRIBED = 6;
    private static final int SUBSCRIBED_FAILED = 7;
    private String SERVER_URL;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case CONNECT:
                    showToast("Connected to : " + SERVER_URL);
                    break;
                case RECONNECT:
                    showToast("Reconnected to : " + SERVER_URL);
                    break;
                case CONNECTIONLOST:
                    showToast("The Connection was lost.");
                    break;
                case MSGARRIVED:

                    break;
                case CONN_FAILED:
                    showToast("Failed to connect to: " + SERVER_URL);
                    break;
                case PUBLISH:
                    //showToast("Message Published");
                    break;
                case SUBSCRIBED:
                    showToast("Subscribed !");
                    break;
                case SUBSCRIBED_FAILED:
                    showToast("Failed to subscribe");
                    break;
            }
        }
    };

    private void showToast(String text){
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
    }

    public void publishMessage(String topic, String publishMessage){
        if(mqttAndroidClient != null && mqttAndroidClient.isConnected()){
            Log.d(TAG, "publishMessage = " + publishMessage);
            try {
                MqttMessage message = new MqttMessage();
                message.setPayload(publishMessage.getBytes());
                mqttAndroidClient.publish(topic, message);
                mHandler.sendEmptyMessage(PUBLISH);
                if(!mqttAndroidClient.isConnected()){
                    Toast.makeText(mContext, mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.", Toast.LENGTH_LONG).show();
                }
            } catch (MqttException e) {
                System.err.println("Error Publishing: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String account;
    public void reportLenovoID(){
        account = LenovoIDApi.getUserName(mContext);
        SysStatus.getInstance(mContext).setLenovoID(account);
        Log.d(TAG, "account = " + account);
        JSONObject jsonObject = new JSONObject();
        if(mqttAndroidClient != null && mqttAndroidClient.isConnected()){
            try {
                jsonObject.put("LenovoID", account);
                //publishMessage(ONLINETopic, jsonObject.toString());
                try {
                    mqttAndroidClient.publish(ONLINETopic, jsonObject.toString().getBytes(), 1, true);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                mHandler.sendEmptyMessage(PUBLISH);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyDeviceOFFLINE(){
        Log.d(TAG, "account = " + account);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("LenovoID", account);
            publishMessage(OFFLINETopic, jsonObject.toString());
            releaseTimerTask();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private TimerTask task;
    private Timer timer;
    private final long DELAY = 0;
    private final long PERIOD = 2000;
    public void reportDeviceStatus(){
        Log.d(TAG, "publishTopic = " + publishTopic);
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                publishMessage(publishTopic, DeviceStatus.toStatusJson());
            }
        };
        timer.schedule(task, DELAY, PERIOD);
    }

    private void releaseTimerTask(){
        if(task != null){
            task.cancel();
            task = null;
        }
        if(timer != null){
            timer.purge();
            timer.cancel();
            timer = null;
        }
    }

}
