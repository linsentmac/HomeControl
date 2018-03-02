package cn.lenovo.smartcontrol.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import cn.lenovo.smartcontrol.LenovoId.LoginIdActivity;
import cn.lenovo.smartcontrol.R;
import cn.lenovo.smartcontrol.utils.StatusBarUtil;


public class QRCodeActivity extends Activity implements View.OnClickListener{

    private Button pass_Bind_btn;
    private Button enter_Bind_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.adjustTranslentWindow(this);
        setContentView(R.layout.activity_qrcode);
        StatusBarUtil.setWhiteTranslucent(this);
        initViews();
        initEvents();

    }

    private void initViews() {
        pass_Bind_btn = findViewById(R.id.pass_Bind_Btn);
        enter_Bind_btn = findViewById(R.id.enter_Bind_Btn);
    }

    private void initEvents() {
        pass_Bind_btn.setOnClickListener(this);
        enter_Bind_btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.pass_Bind_Btn:
                startActivity(new Intent(this, LoginIdActivity.class));
                break;
            case R.id.enter_Bind_Btn:
                //startActivity(new Intent(this, MainActivity.class));
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
