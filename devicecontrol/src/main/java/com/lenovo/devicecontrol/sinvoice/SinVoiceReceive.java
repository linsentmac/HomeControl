package com.lenovo.devicecontrol.sinvoice;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.lenovo.devicecontrol.conn_manager.WifiConnectManager;
import com.lenovo.devicecontrol.conn_manager.WifiConnectionStateListener;
import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoiceRecognition;

import java.io.UnsupportedEncodingException;

/**
 * Created by linsen on 17-11-7.
 */

public class SinVoiceReceive implements SinVoiceRecognition.Listener{

    private static final String TAG = "DC-SinVoiceReceive";

    private final static int MSG_SET_RECG_TEXT = 1;
    private final static int MSG_RECG_START = 2;
    private final static int MSG_RECG_END = 3;

    private final static int[] TOKENS = { 32, 32, 32, 32, 32, 32 };
    private final static int TOKEN_LEN = TOKENS.length;
    private boolean mIsReadFromFile = false;
    private SinVoiceRecognition mRecognition;
    private Handler mHanlder;
    private PowerManager.WakeLock mWakeLock;

    private SinVoiceReceiveListener mListener;

    private char mRecgs[] = new char[100];
    private int mRecgCount;

    public SinVoiceReceive(Context context, SinVoiceReceiveListener listener){
        mListener = listener;
        mRecognition = new SinVoiceRecognition();
        mRecognition.init(context);
        mRecognition.setListener(this);


        mHanlder = new RegHandler(this);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        mWakeLock.acquire();

    }

    public void startRecognition(){
        Log.d(TAG, "startRecognition");
        mRecognition.start(TOKEN_LEN, mIsReadFromFile);
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


    private static class RegHandler extends Handler {
        private StringBuilder mTextBuilder = new StringBuilder();
        private SinVoiceReceive mAct;

        public RegHandler(SinVoiceReceive act) {
            mAct = act;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_RECG_TEXT:
                    Log.d(TAG, "MSG_SET_RECG_TEXT");
                    char ch = (char) msg.arg1;
                    mAct.mRecgs[mAct.mRecgCount++] = ch;
                    break;

                case MSG_RECG_START:
                    mAct.mRecgCount = 0;
                    break;

                case MSG_RECG_END:
                    Log.d(TAG, "recognition end gIsError:" + msg.arg1 + " / mRecgCount = " + mAct.mRecgCount);
                    if ( mAct.mRecgCount > 0 ) {
                        byte[] strs = new byte[mAct.mRecgCount];
                        for ( int i = 0; i < mAct.mRecgCount; ++i ) {
                            strs[i] = (byte)mAct.mRecgs[i];
                        }
                        try {
                            String strReg = new String(strs, "UTF8");
                            Log.d(TAG, "arg1 = " + msg.arg1);
                            if (msg.arg1 >= 0) {
                                Log.d(TAG, "reg ok!!!!!!!!!!!!");
                                if (null != mAct) {
                                    //mAct.name_Et.setText(strReg);
                                    // mAct.mRegState.setText("reg ok(" + msg.arg1 + ")");
                                }
                            } else {
                                Log.d(TAG, "reg error!!!!!!!!!!!!!");
                                mAct.mListener.onSinReceive(strReg);
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

    public void stopRecognition(){
        mRecognition.stop();
    }

    public void releasRecognition(){
        mRecognition.uninit();
    }

}
