package com.lenovo.devicecontrol.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 仿支付宝咻一咻 地震波视图
 * Created by linsen on 2017/11/8.
 */
public class SeismicWave extends View {
    private Paint paint;
    private int maxRadius;// 这个半径决定你想要多大的扩散面积
    private float gradientRatio;// alpha与圆半径的比例，代表1px渐变多少alpha

    private boolean isStarting = false;// 是否运行
    private List<Integer> alphaList = new ArrayList<>();
    private List<Integer> radiusList = new ArrayList<>();

    public SeismicWave(Context context) {
        super(context);
    }

    public SeismicWave(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SeismicWave(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        maxRadius = dp_to_px(context, 150 / 2); //根据XML的宽高150dp得出最大半径
        gradientRatio = 255f / maxRadius;// 渐变比例

        setBackgroundColor(Color.TRANSPARENT);// 颜色：完全透明
        paint = new Paint();
        paint.setColor(0x002693FF);// 设置波纹的颜色

        //进界面时，初始化4个圆。如不需要初始化，保留最后一个
        alphaList.add(102);
        radiusList.add(maxRadius / 5 * 3);

        alphaList.add(153);
        radiusList.add(maxRadius / 5 * 2);

        alphaList.add(204);
        radiusList.add(maxRadius / 5);

        alphaList.add(255);// 圆心的不透明度
        radiusList.add(0);// 圆半径
    }

    public Paint getPaint() {
        return paint;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 依次绘制 同心圆
        for (int i = 0; i < alphaList.size(); i++) {
            int alpha = alphaList.get(i);
            int radius = radiusList.get(i);
            paint.setAlpha(alpha);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, paint);

            // 同心圆扩散
            if (alpha > 0 && radius < maxRadius) {
                alphaList.set(i, alpha - (int) (gradientRatio + 1));// 尽量多减，以免最后一个圆有残留
                radiusList.set(i, radius + 1);
            }
        }

        // 渐变梯度
        if (isStarting && radiusList.get(radiusList.size() - 1) == maxRadius / 5) {
            alphaList.add(255);
            radiusList.add(0);
        }
        // 同心圆数量达到10个，删除最外层圆
        if (isStarting && radiusList.size() == 20) {
            radiusList.remove(0);
            alphaList.remove(0);
        }
        // 刷新界面
        invalidate();
    }

    // 执行动画
    public void start() {
        isStarting = true;
    }

    // 停止动画
    public void stop() {
        isStarting = false;
    }

    // 判断是否在执行
    public boolean isStarting() {
        return isStarting;
    }

    // 重启动画时，初始化3个圆
    public SeismicWave reStart() {
        if (!alphaList.contains(255) && !radiusList.contains(0)) {
            alphaList.add(153);
            radiusList.add(maxRadius / 5 * 2);

            alphaList.add(204);
            radiusList.add(maxRadius / 5);

            alphaList.add(255);
            radiusList.add(0);
        }
        return this;
    }

    /**
     * DP转像素
     */
    public static int dp_to_px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) (dpValue * (scale / 160) + 0.5f);
    }

    // 点击事件，每点击一次增加一个圆
    private boolean isClick = true;

    public void doClick() {
        if (isClick) {
            alphaList.add(255);
            radiusList.add(0);
            isClick = false;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isClick = true;
                }
            }, 333);
        }
    }
}