package cn.lenovo.smartcontrol.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.lenovo.lsf.lenovoid.LOGIN_STATUS;
import com.lenovo.lsf.lenovoid.LenovoIDApi;

import cn.lenovo.smartcontrol.LenovoId.LoginIdActivity;
import cn.lenovo.smartcontrol.R;
import cn.lenovo.smartcontrol.service.DeviceService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        LOGIN_STATUS status = LenovoIDApi.getStatus(this);

        if (status == LOGIN_STATUS.ONLINE) {
            startActivity(new Intent(this, LoginIdActivity.class));
        }else {
            startActivity(new Intent(this, QRCodeActivity.class));
        }
    }
}
