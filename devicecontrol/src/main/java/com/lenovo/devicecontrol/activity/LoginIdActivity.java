package com.lenovo.devicecontrol.activity;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.lenovo.devicecontrol.R;
import com.lenovo.devicecontrol.UserBindActivity;
import com.lenovo.lsf.lenovoid.LOGIN_STATUS;
import com.lenovo.lsf.lenovoid.LenovoIDApi;
import com.lenovo.lsf.lenovoid.OnAuthenListener;

public class LoginIdActivity extends AppCompatActivity {

    private static final String RID = "devicecontrol.lenovo.com";

    private Button mBtnStatus= null;
    private Button mBtnapk= null;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_ACCOUNTMANAGER = 0x00000001;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_SEND_SMS = 0x00000002;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_SEND_SMS_THREAD = 0x00000003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_id);
        //检测AccountManager权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasAccountManagerPermission = checkSelfPermission(Manifest.permission.GET_ACCOUNTS);
            if (hasAccountManagerPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_CODE_ASK_PERMISSIONS_ACCOUNTMANAGER);
                return;
            } else {
                initViews();

            }
        } else {
            initViews();
        }
        LOGIN_STATUS status = LenovoIDApi.getStatus(this);
        if (status == LOGIN_STATUS.OFFLINE) {
            getStOneKeyApk();
        }else{
            LenovoIDApi.showAccountPage(this, RID);
        }
        mBtnStatus = (Button) findViewById(R.id.btn_status);
        mBtnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGIN_STATUS status = LenovoIDApi.getStatus(LoginIdActivity.this);
                String login_status = status == LOGIN_STATUS.ONLINE ? "Login status:online" : "Login status:offline";
                Toast.makeText(LoginIdActivity.this, login_status, Toast.LENGTH_SHORT).show();
            }
        });
        mBtnapk = (Button) findViewById(R.id.btn_apk);
        mBtnapk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getStOneKeyApk();
            }
        });
        finish();
    }

    private void initViews() {
        LenovoIDApi.init(this, RID, null);
        IntentFilter iarif = new IntentFilter();
        iarif.addAction("com.lenovo.lsf.id.action.LOGIN_SUCCESS");
        iarif.addAction("com.lenovo.lsf.id.action.LOGIN_FAILED");
        iarif.addAction("com.lenovo.lsf.id.action.LOGIN_CANCEL");
        iarif.addAction("android.intent.action.LENOVOUSER_STATUS");
        this.registerReceiver(iar, iarif);
    }
    private IntentActionReceiver iar = new IntentActionReceiver();
    public static class IntentActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.LENOVOUSER_STATUS".equals(intent.getAction())) {
                String temp = intent.getStringExtra("status");
                String status = "DEFAULT";
                Integer signInType = 0;
                Log.i("status", temp);
                if (temp.equalsIgnoreCase("1")) {
                    status = "OFFLINE";
                    Log.i("status", intent.getAction() + status);
                } else {
                    status = "ONLINE";
                    signInType = intent.getIntExtra("type", -100);
                    Log.i("status", intent.getAction() + status + "  :  " + signInType);
                }

            } else if ("com.lenovo.lsf.id.action.LOGIN_SUCCESS".equals(intent.getAction())) {
                Log.i("123", intent.getAction());
            } else if ("com.lenovo.lsf.id.action.LOGIN_FAILED".equals(intent.getAction())) {
                Log.i("123", intent.getAction());
            } else if ("com.lenovo.lsf.id.action.LOGIN_CANCEL".equals(intent.getAction())) {
                Log.i("123", intent.getAction());
            }
        }
    }
    private void getStOneKeyApk() {
        OnAuthenListener callback = new OnAuthenListener() {
            @Override
            public void onFinished(boolean ret, String data) {
                if (ret) {
                    //  Toast.makeText(MainActivity.this, "St:" + data, Toast.LENGTH_SHORT).show();
                    //添加账号
                    String username = LenovoIDApi.getUserName(LoginIdActivity.this);
                    Account account = new Account(username, "com.lenovo.devicecontrol.type");

                    AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                    //
                    Account[] g = accountManager.getAccounts();
                    //
                    accountManager.addAccountExplicitly(account, null, null);
                } else {
                    //Toast.makeText(MainActivity.this, "St error:" + data, Toast.LENGTH_SHORT).show();
                }
                //finish();
                startActivity(new Intent(LoginIdActivity.this, UserBindActivity.class));
            }
        };
        Bundle bundle = new Bundle();
        bundle.putBoolean(LenovoIDApi.PRE_AUTO_ONEKEY_LOGIN, true);
        LenovoIDApi.getStData(LoginIdActivity.this, RID, callback, true, bundle);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //finish();
        startActivity(new Intent(this, UserBindActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(iar);
    }
}
