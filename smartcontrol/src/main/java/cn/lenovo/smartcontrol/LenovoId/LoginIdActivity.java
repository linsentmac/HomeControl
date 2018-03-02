package cn.lenovo.smartcontrol.LenovoId;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.lenovo.lsf.lenovoid.LOGIN_STATUS;
import com.lenovo.lsf.lenovoid.LenovoIDApi;
import com.lenovo.lsf.lenovoid.OnAuthenListener;
import com.lenovo.lsf.lenovoid.OnLogoutFinishListener;

import cn.lenovo.smartcontrol.R;
import cn.lenovo.smartcontrol.service.DeviceService;

public class LoginIdActivity extends AppCompatActivity {

    public static final String TAG = "LoginIdActivity";
    private static final int REQUEST_CODE_ASK_PERMISSIONS_ACCOUNTMANAGER = 0x00000001;
    private static final String RID = "smart.lenovo.com";

    private LoginStatusReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_id);

        //检测AccountManager权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.GET_ACCOUNTS)) {
                    Toast.makeText(this, "只有允许设备获取账号列表权限才能使用", Toast.LENGTH_LONG).show();
                }
                requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_CODE_ASK_PERMISSIONS_ACCOUNTMANAGER);
            } else {
                init();
            }
        } else {
            init();
        }
    }

    private void init() {
        LenovoIDApi.init(this, RID, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.lenovo.lsf.id.action.LOGIN_SUCCESS");
        filter.addAction("com.lenovo.lsf.id.action.LOGIN_FAILED");
        filter.addAction("com.lenovo.lsf.id.action.LOGIN_CANCEL");
        filter.addAction("android.intent.action.LENOVOUSER_STATUS");
        registerReceiver(receiver = new LoginStatusReceiver(), filter);

        setLogoutListener();

        LOGIN_STATUS status = LenovoIDApi.getStatus(this);
        if (status == LOGIN_STATUS.OFFLINE) {
            getStOneKeyApk();
        } else {
            LenovoIDApi.showAccountPage(this, RID);
        }
    }

    public class LoginStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onReceive: " + intent.getAction());
            if ("android.intent.action.LENOVOUSER_STATUS".equals(intent.getAction())) {
                String status = intent.getStringExtra("status");
                String finalStatus;
                if (status.equalsIgnoreCase("1")) {
                    finalStatus = "OFFLINE";
                    Log.e(TAG, "LENOVOUSER_STATUS = " + status + ", finalStatus = " + finalStatus);
                } else {
                    finalStatus = "ONLINE";
                    int signInType = intent.getIntExtra("type", -100);
                    Log.e(TAG, "LENOVOUSER_STATUS = " + status + ", finalStatus = " + finalStatus + ", signInType = " + signInType);
                }
            } else if ("com.lenovo.lsf.id.action.LOGIN_SUCCESS".equals(intent.getAction())) {
                Log.e(TAG, "LOGIN_SUCCESS");
            } else if ("com.lenovo.lsf.id.action.LOGIN_FAILED".equals(intent.getAction())) {
                Log.e(TAG, "LOGIN_FAILED");
            } else if ("com.lenovo.lsf.id.action.LOGIN_CANCEL".equals(intent.getAction())) {
                Log.e(TAG, "LOGIN_CANCEL");
            }
        }
    }

    private void getStOneKeyApk() {
        OnAuthenListener callback = new OnAuthenListener() {
            @Override
            public void onFinished(boolean ret, String data) {
                if (ret) {
                    String username = LenovoIDApi.getUserName(LoginIdActivity.this);
                    //添加账号
                    Account account = new Account(username, "cn.lenovo.smartcontrol.type");
                    AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                    if (accountManager != null) {
                        accountManager.addAccountExplicitly(account, null, null);
                    }
                    LenovoIDApi.showAccountPage(LoginIdActivity.this, RID);
                    Log.d(TAG, "login success to startService ");
                    Intent intent = new Intent(LoginIdActivity.this, DeviceService.class);
                    intent.putExtra(DeviceService.EXTRA_CMD_ID, DeviceService.MSG_NOTIFY_ONLINE);
                    startService(intent);
                }
            }
        };
        Bundle bundle = new Bundle();
        bundle.putBoolean(LenovoIDApi.PRE_AUTO_ONEKEY_LOGIN, true);
        LenovoIDApi.getStData(LoginIdActivity.this, RID, callback, true, bundle);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS_ACCOUNTMANAGER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                Toast.makeText(this, "需要访问账户列表权限，否则无法使用", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setLogoutListener() {
        LenovoIDApi.setLogoutFinishListener(new OnLogoutFinishListener() {
            @Override
            public void onLogoutFinish() {
                Log.e(TAG, "LogoutFinishListener");
                getStOneKeyApk();
                Intent intent = new Intent(LoginIdActivity.this, DeviceService.class);
                intent.putExtra(DeviceService.EXTRA_CMD_ID, DeviceService.MSG_NOTIFY_OFFLINE);
                startService(intent);
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        finish();
        Log.e(TAG, "onRestart and finish");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
