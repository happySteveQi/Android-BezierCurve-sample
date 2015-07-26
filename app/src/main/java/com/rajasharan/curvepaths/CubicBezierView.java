package com.rajasharan.curvepaths;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by rajasharan on 7/26/15.
 */
public class CubicBezierView extends View implements Animator.AnimatorListener {
    private static final String TAG = "CubicBeizerView";
    private static final int MAX_COUNT = 4;

    private SparseArray<Point> mTouches;
    private int mCurrentTouchIndex;
    private float mRadius;
    private float[] mAnimatedRadius;
    private Path mPath;
    private Paint mFillPaint;
    private Paint mCurvePaint;
    private ViewConfiguration mViewConfigs;
    private ObjectAnimator mAnim;

    public CubicBezierView(Context context) {
        this(context, null);
    }

    public CubicBezierView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CubicBezierView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        mTouches = new SparseArray<>(MAX_COUNT);
        mRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
        mAnimatedRadius = new float[] {0f, 0f, 0f, 0f};
        mCurrentTouchIndex = -1;
        mViewConfigs = ViewConfiguration.get(context);

        mPath = new Path();
        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(Color.GRAY);

        mCurvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurvePaint.setStyle(Paint.Style.STROKE);
        mCurvePaint.setARGB(128, 128, 128, 128);

        PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofInt("paintAlpha", 255, 0);
        PropertyValuesHolder pvhRadius = PropertyValuesHolder.ofFloat("radius", 0f, 2f);
        mAnim = ObjectAnimator.ofPropertyValuesHolder(this, pvhAlpha, pvhRadius);
        mAnim.setDuration(500);
        mAnim.setInterpolator(new DecelerateInterpolator(2f));
        mAnim.addListener(this);
    }

    private void setPaintAlpha(int a) {
        mFillPaint.setAlpha(a);
        /* invalidate not needed because alpha is running simultaneously with radius */
        //invalidateTouch();
    }

    private void setRadius(float r) {
        if (mCurrentTouchIndex != -1) {
            mAnimatedRadius[mCurrentTouchIndex] = mRadius * r;
            invalidateTouch();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawAnimatedTouches(canvas);
        drawPath(canvas);
    }

    private void drawAnimatedTouches(Canvas canvas) {
        for (int i = 0; i < MAX_COUNT; i++) {
            Point p = mTouches.get(i);
            if (p != null) {
                canvas.drawCircle(p.x, p.y, mAnimatedRadius[i], mFillPaint);
            }
        }
    }

    private void drawPath(Canvas canvas) {
        Point p0, p1, p2, p3;
        p0 = mTouches.get(0);
        p1 = mTouches.get(1);
        p2 = mTouches.get(2);
        p3 = mTouches.get(3);

        if (p3 != null && p2 != null && p1 != null && p0 != null) {
            mPath.rewind();
            mPath.moveTo(p0.x, p0.y);
            mPath.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
            canvas.drawPath(mPath, mCurvePaint);
        }
        else if (p2 != null && p1 != null && p0 != null) {
            mPath.rewind();
            mPath.moveTo(p0.x, p0.y);
            mPath.quadTo(p1.x, p1.y, p2.x, p2.y);
            canvas.drawPath(mPath, mCurvePaint);
        }
        else if (p1 != null && p0 != null) {
            mPath.rewind();
            mPath.moveTo(p0.x, p0.y);
            mPath.lineTo(p1.x, p1.y);
            canvas.drawPath(mPath, mCurvePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                registerTouch(x, y);
                invalidate();
                //invalidateTouch(x, y);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                Point prev = mTouches.get(mCurrentTouchIndex);
                int slop = mViewConfigs.getScaledTouchSlop();
                if (Math.abs(prev.x - x) > slop || Math.abs(prev.y - y) > slop) {
                    Log.d(TAG, String.format("pointerId, pointerIndex: %d, %d", pointerId, index));
                    updateTouch(x, y, pointerId);
                    invalidate();
                    //invalidateTouch(x, y);
                    //invalidateTouch(prev.x, prev.y);
                    return true;
                }
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
            }
            case MotionEvent.ACTION_POINTER_UP: {
            }
            case MotionEvent.ACTION_UP: {
            }
            case MotionEvent.ACTION_CANCEL: {
            }
        }
        return super.onTouchEvent(event);
    }

    private void registerTouch(int x, int y) {
        mCurrentTouchIndex++;
        mCurrentTouchIndex = mCurrentTouchIndex % MAX_COUNT;
        if (mCurrentTouchIndex == 0) {
            mTouches.clear();
        }
        mTouches.put(mCurrentTouchIndex, new Point(x, y));

        if (mAnim.isStarted() || mAnim.isRunning()) {
            mAnim.cancel();
        }
        mAnim.start();
    }

    private void updateTouch(int x, int y, int pointerId) {
        if (pointerId == 0) {
            mTouches.put(mCurrentTouchIndex, new Point(x, y));
        }
        else if (pointerId > 0 && pointerId < MAX_COUNT) {
            mTouches.put(pointerId, new Point(x, y));
        }
    }

    private void invalidateTouch() {
        int radius = (int) mRadius * 2;
        Point p = mTouches.get(mCurrentTouchIndex);
        if (p == null) {
            return;
        }
        invalidate(p.x - radius, p.y - radius, p.x + radius, p.y + radius);
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }
    @Override
    public void onAnimationEnd(Animator animation) {
        for (int i=0; i<mAnimatedRadius.length; i++) {
            mAnimatedRadius[i] = 0f;
        }
    }
    @Override
    public void onAnimationRepeat(Animator animation) {
    }
    @Override
    public void onAnimationCancel(Animator animation) {

    }
}