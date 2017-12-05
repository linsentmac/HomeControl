package cn.lenovo.smartcontrol.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

import cn.lenovo.smartcontrol.R;
import cn.lenovo.smartcontrol.service.DeviceService;
import cn.lenovo.smartcontrol.utils.SCSharePrefrence;

public class LockDeviceActivity extends Activity {

    private LockHandler mHandler;
    private HandlerThread lockThread;
    private Timer lockTimer;
    private long currentTime;
    private long lockTime;
    private static final int MSG_UNLOCK_DEVICE = 0;
    private static final long PERIOD = 1000;
    public static LockDeviceActivity mInstance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_device);
        mInstance = this;
        lockDevice(getIntent());

    }

    private void lockDevice(Intent intent){
        lockTime = intent.getLongExtra(DeviceService.LOCK_TIME, 0);
        /*lockThread = new HandlerThread("lockDevice");
        lockThread.start();
        mHandler = new LockHandler(lockThread.getLooper());
        mHandler.sendEmptyMessageDelayed(MSG_UNLOCK_DEVICE, lockTime);*/

        lockTimer = new Timer();
        if(lockTime > 0){
            lockTimer.schedule(new LockTimerTask(lockTime), 0, PERIOD);
        }
    }

    class LockTimerTask extends TimerTask{
        private long lockTime;
        public LockTimerTask(long time){
            lockTime = time;
        }

        @Override
        public void run() {
            if(currentTime >= lockTime){
                SCSharePrefrence.getInstance(LockDeviceActivity.this).setLockTime(0);
                finish();
            }else {
                currentTime += PERIOD;
                // storage time
                SCSharePrefrence.getInstance(LockDeviceActivity.this).setLockTime(lockTime - currentTime);
            }
        }
    }

    class LockHandler extends Handler{

        public LockHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == MSG_UNLOCK_DEVICE){
                finish();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mInstance = null;
    }



}
