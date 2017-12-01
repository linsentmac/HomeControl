package com.lenovo.devicecontrol;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lenovo.devicecontrol.R;
import com.lenovo.devicecontrol.sinvoice.SinVoiceReceive;
import com.lenovo.devicecontrol.sinvoice.SinVoiceReceiveListener;
import com.lenovo.devicecontrol.utils.StatusBarUtil;
import com.lenovo.devicecontrol.view.SeismicWave;
import com.lenovo.devicecontrol.view.WaveLineView;
import com.libra.sinvoice.LogHelper;

public class UserBindActivity extends Activity implements View.OnClickListener,
        SinVoiceReceiveListener{

    private static final String TAG = "DC-UserBindActivity";
    private WaveLineView waveLineView;
    private SeismicWave seismicWave;
    private TextView bind_Success;
    private TextView tv_bind_hint;
    private TextView tv_user_msg;
    private Button btn_go_Home;

    private SinVoiceReceive sinVoiceReceive;

    static {
        System.loadLibrary("sinvoice");
        LogHelper.d(TAG, "sinvoice jnicall loadlibrary");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.adjustTranslentWindow(this);
        setContentView(R.layout.activity_user_bind);
        StatusBarUtil.setWhiteTranslucent(this);
        initViews();
        initEvents();
        sinVoiceReceive = new SinVoiceReceive(this, this);
        sinVoiceReceive.startRecognition();
    }

    private void initViews() {
        waveLineView = findViewById(R.id.mWaveLineView);
        seismicWave = findViewById(R.id.seismicwave);
        bind_Success = findViewById(R.id.tv_bind_success);
        tv_bind_hint = findViewById(R.id.tv_bind_hint);
        tv_user_msg = findViewById(R.id.tv_bind_sin_msg);
        btn_go_Home = findViewById(R.id.go_home_btn);
    }

    private void initEvents() {
        btn_go_Home.setOnClickListener(this);
        seismicWave.reStart().start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.go_home_btn:
                finish();
                break;
        }
    }

    @Override
    public void onSinReceive(String result) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sinVoiceReceive.stopRecognition();
        sinVoiceReceive.releasRecognition();
    }
}
