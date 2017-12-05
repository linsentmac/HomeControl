package cn.lenovo.smartcontrol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

import cn.lenovo.smartcontrol.R;
import cn.lenovo.smartcontrol.utils.StatusBarUtil;
import cn.lenovo.smartcontrol.wifi_manager.WifiConnectManager;
import cn.lenovo.smartcontrol.wifi_manager.WifiConnectionStateListener;

public class WifiConnectActivity extends Activity implements WifiConnectManager.ScanResultListener,
        AdapterView.OnItemClickListener{

    private static final String TAG = "DC-WifiConnectActivity";

    private ListView wifiListView;
    private TextView search_hint;
    private TextView next_Btn;
    private WifiAdapter adapter;
    private WifiConnectManager wifiConnectManager;
    private List<String> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.adjustTranslentWindow(this);
        setContentView(R.layout.activity_wifi_connect);
        StatusBarUtil.setWhiteTranslucent(this);
        initView();
        initData();
        wifiConnectManager = WifiConnectManager.newInstance(this, mWifiStateListener);
        wifiConnectManager.setScanResultListener(this);
        wifiConnectManager.startScan();
        adapter = new WifiAdapter(this, mList);
        wifiListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    private void initView() {
        search_hint = findViewById(R.id.search_wlan_hint);
        next_Btn = findViewById(R.id.Next_Btn);
        wifiListView = findViewById(R.id.wlan_listView);
        wifiListView.setOnItemClickListener(this);
        next_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WifiConnectActivity.this, LoginIdActivity.class));
            }
        });
    }

    private void initData() {
        mList = new ArrayList<>();
    }

    @Override
    public void onScanResult(ArrayList<ScanResult> scanResults) {
        for(ScanResult result : scanResults){
            mList.add(result.SSID);
        }
        search_hint.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        View itemView = wifiListView.getChildAt(position - wifiListView.getFirstVisiblePosition());
        Log.d(TAG, "itemView = " + itemView + " / " + wifiListView.getChildCount());
        connected_hint = ((WifiAdapter.ViewHolder)itemView.getTag()).wifi_connected;
        String wifiName = mList.get(position);
        showPasswordDialog(wifiName);
    }

    private TextView wifiName;
    private TextView connected_hint;
    private EditText editText;
    private Button cancel;
    private Button ensure;
    private AlertDialog dialog;
    private String password;
    private void showPasswordDialog(String name){
        dialog = new AlertDialog.Builder(this).create();
        View customDialog = LayoutInflater.from(this).inflate(R.layout.wifi_password_dialog, null);

        initDialogView(customDialog);
        initDialogData(name);
        initDialogEvents(name);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(200,400);
        customDialog.setLayoutParams(params);
        dialog.setView(customDialog);
        dialog.show();
        Window dialogWindow = dialog.getWindow();
        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高度
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        p.height = (int) (d.getHeight() * 0.7); // 高度设置为屏幕的0.6，根据实际情况调整
        p.width = (int) (d.getWidth() * 0.4); // 宽度设置为屏幕的0.65，根据实际情况调整
        dialogWindow.setAttributes(p);

    }

    private void initDialogView(View customDialog) {
        wifiName = customDialog.findViewById(R.id.dialog_wifi_name);
        editText = customDialog.findViewById(R.id.password_Et);
        cancel = customDialog.findViewById(R.id.cancel_password);
        ensure = customDialog.findViewById(R.id.ensure_password);
    }

    private void initDialogData(String name) {
        wifiName.setText(name);
    }

    private void initDialogEvents(final String name) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                password = s.toString();
                Log.d(TAG, "input password = " + password);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        ensure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiConnectManager.connectWifi(name, password);
                dialog.dismiss();
            }
        });
    }

    private class WifiAdapter extends BaseAdapter{

        private Context mContext;
        private List<String> mList;

        public WifiAdapter(Context context, List<String> mList){
            mContext = context;
            this.mList = mList;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView == null){
                holder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.wifi_conn_item, parent, false);
                holder.wifi_Name = convertView.findViewById(R.id.wifi_name);
                holder.wifi_connected = convertView.findViewById(R.id.connected_tv);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }
            Log.d(TAG, "position = " + position);
            holder.wifi_Name.setText(mList.get(position));
            return convertView;
        }

        class ViewHolder{
            public TextView wifi_Name;
            public TextView wifi_connected;
            public ViewHolder(){

            }
        }

    }


    private WifiConnectionStateListener mWifiStateListener =
            new WifiConnectionStateListener(){
                @Override
                public void onWifiStartConnect() {
                    super.onWifiStartConnect();
                }

                @Override
                public void onWifiConnected() {
                    super.onWifiConnected();
                    Toast.makeText(WifiConnectActivity.this, getResources().getString(R.string.connect_success_str), Toast.LENGTH_LONG).show();
                    connected_hint.setVisibility(View.VISIBLE);
                }

                @Override
                public void onWifiDisconnected() {
                    super.onWifiDisconnected();
                }
            };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        wifiConnectManager.unRegisterReceiver();
    }
}
