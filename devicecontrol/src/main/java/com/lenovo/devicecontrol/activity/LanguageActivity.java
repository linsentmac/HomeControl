package com.lenovo.devicecontrol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.lenovo.devicecontrol.R;
import com.lenovo.devicecontrol.utils.StatusBarUtil;

import java.util.ArrayList;

public class LanguageActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "DC-LanguageActivity";
    private ListView mListView;
    private TextView ensure_Tv;
    private LanguageAdapter mAdapter;
    private ArrayList<String> mList;
    private String languageName;
    public static String zh_CN = "中文(简体)";
    public static String US = "English";
    private String Key = "language";
    private Intent languageIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.adjustTranslentWindow(this);

        setContentView(R.layout.activity_language);
        StatusBarUtil.setWhiteTranslucent(this);

        initViews();
        initEvent();
        initData();
        languageIntent = new Intent("android.intent.action.SWITCH_LANGUAGE");
        languageIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        mAdapter = new LanguageAdapter(this, mList);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    private void initViews() {
        mListView = findViewById(R.id.listview_language);
        ensure_Tv = findViewById(R.id.ensure_language_Tv);
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                View itemView;
                ImageView ensure_language;
                for(int i = 0; i < mList.size(); i++){
                    itemView = mListView.getChildAt(i);
                    ensure_language = ((LanguageAdapter.ViewHolder)itemView.getTag()).ensure_language;
                    if(i == position){
                        ensure_language.setVisibility(View.VISIBLE);
                        languageName = ((LanguageAdapter.ViewHolder)itemView.getTag()).language_name.getText().toString();
                        Log.d(TAG, "languageName = " + languageName);
                    }else {
                        ensure_language.setVisibility(View.GONE);
                    }
                }
            }
        });
        ensure_Tv.setOnClickListener(this);
    }

    private void initData() {
        mList = new ArrayList<>();
        mList.add(zh_CN);
        mList.add(US);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ensure_language_Tv:
                if(languageName.equals(zh_CN)){
                    languageIntent.putExtra(Key, zh_CN);
                }else {
                    languageIntent.putExtra(Key, US);
                }
                sendBroadcast(languageIntent);
                startActivity(new Intent(this, QRCodeActivity.class));
                break;
        }
    }

    private class LanguageAdapter extends BaseAdapter{

        private ArrayList<String> list;
        private Context context;

        public LanguageAdapter(Context context, ArrayList<String> list){
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            Log.d(TAG, "name = " + list.size());
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
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
                convertView = LayoutInflater.from(context).inflate(R.layout.language_item, parent, false);
                holder.language_name = convertView.findViewById(R.id.language_name_tv);
                holder.ensure_language = convertView.findViewById(R.id.ensure_language_iv);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.language_name.setText(list.get(position));
            Log.d(TAG, "name = " + list.get(position));
            return convertView;
        }

        class ViewHolder{
            public TextView language_name;
            public ImageView ensure_language;

            public ViewHolder(){}
        }

    }



}
