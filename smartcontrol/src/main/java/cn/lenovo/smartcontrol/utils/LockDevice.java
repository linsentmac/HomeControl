package cn.lenovo.smartcontrol.utils;

import android.content.Context;
import android.content.Intent;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import cn.lenovo.smartcontrol.activity.LockDeviceActivity;

/**
 * Created by linsen on 17-12-5.
 */

public class LockDevice {

    private static LockDevice mInstance;
    private Context mContext;
    private Timer timer;
    private lockTimingTask task;
    private List<String> weekList;

    private LockDevice(Context context){
        mContext = context;
        timer = new Timer();
    }

    public static LockDevice getInstance(Context context){
        if(mInstance == null){
            mInstance = new LockDevice(context);
        }
        return mInstance;
    }

    public void setDeviceTiming(String startTime, String endTime, String[] workDay){
        weekList = new ArrayList<>();
        for(String day : workDay){
            weekList.add(day);
        }
        task = new lockTimingTask(startTime, endTime, weekList);
        timer.schedule(task, 0, 1000);
    }

    class lockTimingTask extends TimerTask{

        private TimeZone tz;
        private Calendar c;
        private DateFormat df;
        private String startTime;
        private String endTime;
        private List<String> workDay = new ArrayList<>();
        private String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

        public lockTimingTask(String startTime, String endTime, List<String> workDay){
            this.startTime = startTime;
            this.endTime = endTime;
            this.workDay.addAll(workDay);
            tz = TimeZone.getTimeZone("GMT");
            c = Calendar.getInstance(tz);
            df = new SimpleDateFormat("HH:mm");
        }

        @Override
        public void run() {
            String today = weekDays[c.get(Calendar.DAY_OF_WEEK) - 1];
            if(workDay.contains(today)){
                Date currentDate = new Date(System.currentTimeMillis());
                try {
                    Date startDate = df.parse(startTime);
                    Date endDate = df.parse(endTime);
                    if((startDate.getTime() < currentDate.getTime())
                            && (currentDate.getTime() < endDate.getTime())){
                        // unlock Device
                        LockDeviceActivity.mInstance.finish();
                    }else {
                        // lock Device
                        lockDevice();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }else {
                // lock Device
                lockDevice();
            }
        }

        private void lockDevice(){
            mContext.startActivity(new Intent(mContext, LockDeviceActivity.class));
        }
    }

    public void cancelTiming(){
        if(LockDeviceActivity.mInstance != null){
            LockDeviceActivity.mInstance.finish();
        }
        if(task != null){
            task.cancel();
            task = null;
        }
        if(timer != null){
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }


}
