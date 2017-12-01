package com.lenovo.devicecontrol;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lenovo.devicecontrol.conn_manager.WifiConnectManager;
import com.lenovo.devicecontrol.conn_manager.WifiConnectionStateListener;
import com.lenovo.devicecontrol.sinvoice.SinVoiceReceive;
import com.lenovo.devicecontrol.sinvoice.SinVoiceReceiveListener;
import com.lenovo.devicecontrol.utils.StatusBarUtil;
import com.lenovo.devicecontrol.view.SeismicWave;
import com.lenovo.devicecontrol.view.WaveLineView;
import com.libra.sinvoice.Common;
import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoicePlayer;

import java.io.UnsupportedEncodingException;


public class MainActivity extends Activity implements View.OnClickListener,
        SinVoiceReceiveListener{

    private static final String TAG = "DC-MainActivity";

    private final static int[] TOKENS = { 32, 32, 32, 32, 32, 32 };
    private final static int TOKEN_LEN = TOKENS.length;

    private WaveLineView waveLineView;
    private SeismicWave seismicWave;
    private RelativeLayout next_Layout;
    private TextView connect_Success;
    private TextView tv_hint;
    private TextView tv_sin_msg;
    private Button btn_Bind;
    private Button btn_Pass;
    private Button btn_Next;

    private String wifiName;

    protected WifiConnectManager mWifiConnectManager;


    private SinVoiceReceive sinVoiceReceive;
    private SinVoicePlayer mSinVoicePlayer;

    static {
        System.loadLibrary("sinvoice");
        LogHelper.d(TAG, "sinvoice jnicall loadlibrary");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.adjustTranslentWindow(this);
        setContentView(R.layout.activity_main);
        StatusBarUtil.setWhiteTranslucent(this);
        initView();
        initEvents();
        sinVoiceReceive = new SinVoiceReceive(this, this);
        mWifiConnectManager = WifiConnectManager.newInstance(this, mWifiListener);

        mSinVoicePlayer = new SinVoicePlayer();
        mSinVoicePlayer.init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mWakeLock.acquire();
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "只有允许使用该设备的位置信息才能搜索到WiFi", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void initView(){
        next_Layout = findViewById(R.id.next_layout);
        waveLineView = findViewById(R.id.mWaveLineView);
        seismicWave = findViewById(R.id.seismicwave);
        connect_Success = findViewById(R.id.tv_isSuccess);
        tv_hint = findViewById(R.id.tv_hint);
        tv_sin_msg = findViewById(R.id.tv_sin_msg);
        btn_Bind = findViewById(R.id.start_Conn);
        btn_Pass = findViewById(R.id.pass_Btn);
        btn_Next = findViewById(R.id.next_Btn);
    }

    private void initEvents() {
        btn_Bind.setOnClickListener(this);
        btn_Pass.setOnClickListener(this);
        btn_Next.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start_Conn:
                sinVoiceReceive.startRecognition();
                tv_hint.setVisibility(View.VISIBLE);
                btn_Bind.setVisibility(View.GONE);
                seismicWave.setVisibility(View.VISIBLE);
                seismicWave.reStart().start();
                break;
            case R.id.pass_Btn:
                finish();
                break;
            case R.id.next_Btn:
                startActivity(new Intent(this, UserBindActivity.class));
                finish();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mRecognition.stop();
        mSinVoicePlayer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mRecognition.uninit();
        mSinVoicePlayer.uninit();
        sinVoiceReceive.stopRecognition();
        sinVoiceReceive.releasRecognition();
        mWifiConnectManager.unRegisterReceiver();
    }

    // 请求定位权限结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

    }

    private WifiConnectionStateListener mWifiListener =
            new WifiConnectionStateListener(){
                @Override
                public void onWifiStartConnect() {
                    super.onWifiStartConnect();
                    btn_Bind.setVisibility(View.GONE);
                    seismicWave.setVisibility(View.GONE);
                    waveLineView.setVisibility(View.VISIBLE);
                    tv_hint.setVisibility(View.GONE);
                    tv_sin_msg.setVisibility(View.VISIBLE);
                }

                @Override
                public void onWifiConnected() {
                    super.onWifiConnected();
                    waveLineView.setVisibility(View.GONE);
                    connect_Success.setVisibility(View.VISIBLE);
                    tv_sin_msg.setVisibility(View.GONE);
                    next_Layout.setVisibility(View.VISIBLE);
                    playResponseVoice();
                }

                @Override
                public void onWifiDisconnected() {
                    super.onWifiDisconnected();
                }
            };

    private final String wifiNameMark = "#w";
    private final String passwardMark = "#p";
    @Override
    public void onSinReceive(String result) {
        /*if(result.contains("Name")){
            wifiName = result.substring(4);
            Log.d(TAG, "wifiName = " + wifiName);
        }else if(result.contains("Password")){
            String password = result.substring(8);
            Log.d(TAG, "password = " + password);
            if(wifiName != null && password != null){
                mWifiConnectManager.connectWifi(wifiName, password);
                mWifiListener.onWifiStartConnect();
            }
        }*/
        sinVoiceReceive.stopRecognition();
        if(result.contains(wifiNameMark)
                && result.contains(passwardMark)){
            int wifiNameLastindex = result.indexOf(passwardMark);
            String wifiName = result.substring(wifiNameMark.length(), wifiNameLastindex);
            String Password = result.substring(wifiNameLastindex + passwardMark.length());
            Log.d(TAG, "wifiName = " + wifiName + "\n" + "Password = " + Password);
            if(wifiName != null && Password != null){
                mWifiConnectManager.connectWifi(wifiName, Password);
                mWifiListener.onWifiStartConnect();
            }
        }
    }

    private void playResponseVoice(){
        String response = "Wifi已连接";
        try {
            byte[] strs = response.getBytes("UTF8");
            if ( null != strs ) {
                int len = strs.length;
                int []tokens = new int[len];
                int maxEncoderIndex = mSinVoicePlayer.getMaxEncoderIndex();
                LogHelper.d(TAG, "maxEncoderIndex:" + maxEncoderIndex);
                String encoderText = response.toString();
                for ( int i = 0; i < len; ++i ) {
                    if ( maxEncoderIndex < 255 ) {
                        tokens[i] = Common.DEFAULT_CODE_BOOK.indexOf(encoderText.charAt(i));
                    } else {
                        tokens[i] = strs[i];
                    }
                }
                mSinVoicePlayer.play(tokens, len, false, 2000);
            } else {
                mSinVoicePlayer.play(TOKENS, TOKEN_LEN, false, 2000);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
