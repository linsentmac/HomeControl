package com.lenovo.devicecontrol2;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoiceRecognition;

import java.io.UnsupportedEncodingException;

public class WifiConnActivity extends Activity implements View.OnClickListener,
        SinVoiceRecognition.Listener {

    private static final String TAG = "DC-WifiConnActivity";

    private Button start_Reg;
    private Button stop_Reg;
    private Button connect;
    private EditText name_Et;
    private EditText password_Et;

    private final static int MSG_SET_RECG_TEXT = 1;
    private final static int MSG_RECG_START = 2;
    private final static int MSG_RECG_END = 3;

    private final static int[] TOKENS = { 32, 32, 32, 32, 32, 32 };
    private final static int TOKEN_LEN = TOKENS.length;
    private boolean mIsReadFromFile;
    private SinVoiceRecognition mRecognition;
    private Handler mHanlder;
    private PowerManager.WakeLock mWakeLock;

    private char mRecgs[] = new char[100];
    private int mRecgCount;

    static {
        System.loadLibrary("sinvoice");
        LogHelper.d(TAG, "sinvoice jnicall loadlibrary");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_conn);
        mIsReadFromFile = false;
        initView();
        initEvents();

        mRecognition = new SinVoiceRecognition();
        mRecognition.init(this);
        mRecognition.setListener(this);

        mHanlder = new RegHandler(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mWakeLock.acquire();
    }

    private void initView(){
        start_Reg = (Button) findViewById(R.id.start_Reg);
        stop_Reg = (Button) findViewById(R.id.stop_Reg);
        connect = (Button) findViewById(R.id.wifi_Connect);
        name_Et = findViewById(R.id.name_Et);
        password_Et = findViewById(R.id.password_Et);
    }

    private void initEvents() {
        start_Reg.setOnClickListener(this);
        stop_Reg.setOnClickListener(this);
        connect.setOnClickListener(this);
    }

    private static class RegHandler extends Handler {
        private StringBuilder mTextBuilder = new StringBuilder();
        private WifiConnActivity mAct;

        public RegHandler(WifiConnActivity act) {
            mAct = act;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_RECG_TEXT:
                    char ch = (char) msg.arg1;
//                mTextBuilder.append(ch);
                    mAct.mRecgs[mAct.mRecgCount++] = ch;
                    break;

                case MSG_RECG_START:
//                mTextBuilder.delete(0, mTextBuilder.length());
                    mAct.mRecgCount = 0;
                    break;

                case MSG_RECG_END:
                    LogHelper.d(TAG, "recognition end gIsError:" + msg.arg1);
                    if ( mAct.mRecgCount > 0 ) {
                        byte[] strs = new byte[mAct.mRecgCount];
                        for ( int i = 0; i < mAct.mRecgCount; ++i ) {
                            strs[i] = (byte)mAct.mRecgs[i];
                        }
                        try {
                            String strReg = new String(strs, "UTF8");
                            if (msg.arg1 >= 0) {
                                Log.d(TAG, "reg ok!!!!!!!!!!!!");
                                if (null != mAct) {
                                    mAct.name_Et.setText(strReg);
                                    // mAct.mRegState.setText("reg ok(" + msg.arg1 + ")");
                                }
                            } else {
                                Log.d(TAG, "reg error!!!!!!!!!!!!!");
                                mAct.name_Et.setText(strReg);
                                // mAct.mRegState.setText("reg err(" + msg.arg1 + ")");
                                // mAct.mRegState.setText("reg err");
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start_Reg:
                mRecognition.start(TOKEN_LEN, mIsReadFromFile);
                break;
            case R.id.stop_Reg:
                mRecognition.stop();
                break;
            case R.id.wifi_Connect:

                break;
        }
    }

    @Override
    public void onSinVoiceRecognitionStart() {
        mHanlder.sendEmptyMessage(MSG_RECG_START);
    }

    @Override
    public void onSinVoiceRecognition(char ch) {
        mHanlder.sendMessage(mHanlder.obtainMessage(MSG_SET_RECG_TEXT, ch, 0));
    }

    @Override
    public void onSinVoiceRecognitionEnd(int result) {
        mHanlder.sendMessage(mHanlder.obtainMessage(MSG_RECG_END, result, 0));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRecognition.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecognition.uninit();
    }
}
