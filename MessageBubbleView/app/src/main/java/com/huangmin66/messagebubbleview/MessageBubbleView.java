package com.huangmin66.messagebubbleview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * 作者：Administrator on 2017/9/11 10:11
 * 描述：
 */

public class MessageBubbleView extends View {

    //固定圆心 和 拖拽圆心
    private PointF mFixationPoint, mDragPoint;
    //拖拽圆的半径
    private int mDragRadius = 10;
    //固定圆的半径
    private int mFixationRadiusMax = 7;
    private int mFixationRadiusMin = 3;
    private int mFixationRadius;
    //画笔
    private Paint mPaint;
    private Bitmap mDragBitmap;

    public MessageBubbleView(Context context) {
        this(context, null);
    }

    public MessageBubbleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessageBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDragRadius = dip2px(mDragRadius);
        mFixationRadiusMax = dip2px(mFixationRadiusMax);
        mFixationRadiusMin = dip2px(mFixationRadiusMin);
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
    }

    private int dip2px(int radius) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radius, getResources().getDisplayMetrics());
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                //指定当前位置
//                initPoint(event.getX(), event.getY());
//                break;
//            case MotionEvent.ACTION_UP:
//
//                break;
//            case MotionEvent.ACTION_MOVE:
//                updatePoint(event.getX(), event.getY());
//                break;
//            case MotionEvent.ACTION_CANCEL:
//                break;
//        }
//        //更新界面
//        invalidate();
//        return true;
//    }

    /**
     * 初始化位置
     *
     * @param x
     * @param y
     */
    public void initPoint(float x, float y) {
        mFixationPoint = new PointF(x, y);
        mDragPoint = new PointF(x, y);
        invalidate();
    }

    /**
     * 更新拖拽圆心
     *
     * @param x
     * @param y
     */
    public void updatePoint(float x, float y) {
        mDragPoint.x = x;
        mDragPoint.y = y;
        //更新界面
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mFixationPoint == null || mDragPoint == null) {
            return;
        }
        //画两个圆
        //拖拽圆
        canvas.drawCircle(mDragPoint.x, mDragPoint.y, mDragRadius, mPaint);
        //固定圆 有一个初始化大小 而且他的半径随着距离的增大而减小 小到一定程度就不见了
        Path bezierPath = getBezeierPath();

        if (bezierPath != null) {
            canvas.drawCircle(mFixationPoint.x, mFixationPoint.y, mFixationRadius, mPaint);

            canvas.drawPath(bezierPath, mPaint);
        }

        //画图片 位置也是手指移动的位置，中心位置才是手指拖动的位置
        if (mDragBitmap != null){
            canvas.drawBitmap(mDragBitmap, mDragPoint.x - mDragBitmap.getWidth()/2, mDragPoint.y - mDragBitmap.getHeight()/2, null);
        }
    }

    /**
     * 获取两个圆心之间的距离
     *
     * @param point1
     * @param point2
     * @return
     */
    private float getDistance(PointF point1, PointF point2) {
        return (float) Math.sqrt((point2.x - point1.x) * (point2.x - point1.x) +
                (point2.y - point1.y) * (point2.x - point1.x));
    }

    /**
     * 获取贝塞尔路径
     *
     * @return
     */
    public Path getBezeierPath() {
        //两点之间的距离
        float distance = getDistance(mFixationPoint, mDragPoint);
        mFixationRadius = (int) (mFixationRadiusMax - distance / 21);

        if (mFixationRadius < mFixationRadiusMin) {
            //超过一定距离 贝塞尔和固定圆都不要画了
            return null;
        }

        Path bezeierPath = new Path();
        // 求角度a
        // 求斜率
        float tanA = (mDragPoint.y - mFixationPoint.y) / (mDragPoint.x - mFixationPoint.x);
        double arcTanA = Math.atan(tanA);
        // p0点
        float p0X = (float) (mFixationPoint.x + mFixationRadius * Math.sin(arcTanA));
        float p0Y = (float) (mFixationPoint.y - mFixationRadius * Math.cos(arcTanA));
        // p1点
        float p1X = (float) (mDragPoint.x + mDragRadius * Math.sin(arcTanA));
        float p1Y = (float) (mDragPoint.y - mDragRadius * Math.cos(arcTanA));
        // p2点
        float p2X = (float) (mDragPoint.x - mDragRadius * Math.sin(arcTanA));
        float p2Y = (float) (mDragPoint.y + mDragRadius * Math.cos(arcTanA));
        // p3点
        float p3X = (float) (mFixationPoint.x - mFixationRadius * Math.sin(arcTanA));
        float p3Y = (float) (mFixationPoint.y + mFixationRadius * Math.cos(arcTanA));

        //拼接 贝塞尔的曲线路径
        bezeierPath.moveTo(p0X, p0Y);
        //绘制一阶贝塞尔曲线 参数为控制点，结束点
        PointF controlPoint = getControlPoint();
        bezeierPath.quadTo(controlPoint.x, controlPoint.y, p1X, p1Y);
        //绘制二阶贝塞尔曲线 参数为控制点，结束点
//        bezeierPath.cubicTo();
        //画第二条
        bezeierPath.lineTo(p2X, p2Y);
        bezeierPath.quadTo(controlPoint.x, controlPoint.y, p3X, p3Y);
        bezeierPath.close();

        return bezeierPath;
    }

    public PointF getControlPoint() {
        return new PointF(mFixationPoint.x + (mDragPoint.x - mFixationPoint.x) / 2, mFixationPoint.y + (mDragPoint.y - mFixationPoint.y) / 2);
    }

    /**
     * 绑定可以拖拽的控件
     * @param view
     * @param listener
     */
    public static void attach(View view, BubbleMessageTouchListener.BubbleDisappearListener listener) {
        if (view == null){
            return;
        }
        view.setOnTouchListener(new BubbleMessageTouchListener(view, listener));
    }

    public void setDragBitmap(Bitmap dragBitmap) {
        this.mDragBitmap = dragBitmap;
    }

    /**
     * 处理手指松开
     */
    public void handleActionUp() {
        if (mFixationRadius > mFixationRadiusMin){
            //回弹 valueAnimator 值变化的动画 0 - 1
            ValueAnimator animator = ObjectAnimator.ofFloat(1);
            animator.setDuration(350);
            final PointF start = new PointF(mDragPoint.x, mDragPoint.y);
            final PointF end = new PointF(mFixationPoint.x, mFixationPoint.y);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float percent = (float) animation.getAnimatedValue();
                    PointF pointF = BubbleUtils.getPointByPercent(start, end, percent);
                    //用代码更新拖拽点
                    updatePoint(pointF.x, pointF.y);
                }
            });
            animator.setInterpolator(new OvershootInterpolator(3f));
            animator.start();
            //还要通知 TouchListener 移除当前View 然后显示静态的 View
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mListener != null){
                        mListener.restore();
                    }
                }
            });
        } else {
            //爆炸
            if (mListener != null){
                mListener.dismiss(mDragPoint);
            }
        }
    }



    private MessageBubbleListener mListener;
    public void setMessageBubbleListener(MessageBubbleListener listener){
        mListener = listener;
    }

    public interface MessageBubbleListener{
        //还原
        public void restore();
        //消失
        public void dismiss(PointF pointF);
    }
}
