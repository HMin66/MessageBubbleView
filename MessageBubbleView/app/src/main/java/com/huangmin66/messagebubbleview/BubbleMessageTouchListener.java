package com.huangmin66.messagebubbleview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * 作者：Administrator on 2017/9/11 16:58
 * 描述：监听当前View的触摸事件
 */

public class BubbleMessageTouchListener implements View.OnTouchListener, MessageBubbleView.MessageBubbleListener {

    //原来需要拖动爆炸的View
    private View mStaticView;
    private WindowManager mWindowManager;
    private MessageBubbleView mMessageBubbleView;
    private WindowManager.LayoutParams mParams;

    //爆炸动画
    private FrameLayout mBombFrame;
    private ImageView mBomImage;

    private BubbleDisappearListener mDisappearListener;

    public BubbleMessageTouchListener(View view, BubbleDisappearListener listener){
        mStaticView = view;
        mWindowManager = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
        mMessageBubbleView = new MessageBubbleView(view.getContext());
        mMessageBubbleView.setMessageBubbleListener(this);
        mParams = new WindowManager.LayoutParams();
        //设置背景透明
        mParams.format = PixelFormat.TRANSPARENT;

        mBombFrame = new FrameLayout(view.getContext());
        mBomImage = new ImageView(view.getContext());
        mBomImage.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mBombFrame.addView(mBomImage);
        mDisappearListener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                //要在windowManager上面搞一个View,上一节写好的贝塞尔的View
                mWindowManager.addView(mMessageBubbleView, mParams);
                //初始化贝塞尔View的点, 不能在使用getX, 因为该获取到的坐标是相对于父容器
                //我们现在要获取屏幕上的坐标, 应该使用getRawX
//                mMessageBubbleView.initPoint(event.getX(), event.getY());
                //保证固定圆的中心在view的中心
                int[] location = new int[2];
                mStaticView.getLocationOnScreen(location);
                mMessageBubbleView.initPoint(location[0] + mStaticView.getWidth() / 2, location[1] + mStaticView.getHeight()/2 - BubbleUtils.getStatusBarHeight(mStaticView.getContext()));
                //给消息拖拽设置一个bitmap
                mMessageBubbleView.setDragBitmap(getBitmapByView(mStaticView));
                //将自已隐藏
                mStaticView.setVisibility(View.INVISIBLE);
                break;
            case MotionEvent.ACTION_MOVE:
//                mMessageBubbleView.updatePoint(event.getX(), event.getY());
                mMessageBubbleView.updatePoint(event.getRawX(), event.getRawY() - BubbleUtils.getStatusBarHeight(mStaticView.getContext()));
                break;
            case MotionEvent.ACTION_UP:
                mMessageBubbleView.handleActionUp();
                break;
        }
        return true;
    }

    private Bitmap getBitmapByView(View view) {
        view.buildDrawingCache();
        return view.getDrawingCache();
    }

    @Override
    public void restore() {
        // 把消息的View 移除
        mWindowManager.removeView(mMessageBubbleView);
        // 把原来的view 显示
        mStaticView.setVisibility(View.VISIBLE);
    }

    @Override
    public void dismiss(PointF pointF) {
        // 要执行爆炸动画 （帧动画）
        // 把消息的View 移除
        mWindowManager.removeView(mMessageBubbleView);
        // 要在mWindowManager 添加一个爆炸动画
        mWindowManager.addView(mBombFrame, mParams);
        mBomImage.setBackgroundResource(R.drawable.anim_bubble_pop);
        AnimationDrawable drawable = (AnimationDrawable) mBomImage.getBackground();
        mBomImage.setX(pointF.x - drawable.getIntrinsicWidth()/2);
        mBomImage.setY(pointF.y - drawable.getIntrinsicHeight()/2);
        drawable.start();
        // 执行完成后移除 爆炸动画 mBobFrame
        mBomImage.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWindowManager.removeView(mBombFrame);
                if (mDisappearListener != null){
                    mDisappearListener.dismiss(mStaticView);
                }
            }
        }, getAnimationDrawableTime(drawable));
    }

    private long getAnimationDrawableTime(AnimationDrawable drawable) {
        int numberOfFrames = drawable.getNumberOfFrames();
        long time = 0;
        for (int i = 0; i < numberOfFrames; i++){
            time += drawable.getDuration(i);
        }
        return time;
    }

    public interface BubbleDisappearListener{
        void dismiss(View view);
    }
}
