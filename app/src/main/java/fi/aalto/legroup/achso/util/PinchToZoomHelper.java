package fi.aalto.legroup.achso.util;

import android.app.Activity;
import android.graphics.Matrix;
import android.util.FloatMath;
import android.view.MotionEvent;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages zooming and panning around for view
 * <p/>
 * Created by lassi on 23.10.14.
 */
public class PinchToZoomHelper {
    public static interface MatrixHasChangedDelegate {
        public void matrixHasChanged(PinchToZoomHelper matrix);

        public boolean onTouchEvent(MotionEvent event);

        public Activity getActivity();
    }

    public static interface OnHoldDelegate {
        public void onTouchHold(float positionX, float positionY);
    }

    public static class HoldTask extends TimerTask {
        private PinchToZoomHelper matrix;

        public HoldTask(PinchToZoomHelper matrix) {
            super();
            this.matrix = matrix;
        }

        public void run() {
            this.matrix.getMatrixHasChangedDelegate().getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    matrix.setIsHolding(true);
                }
            });
        }
    }

    private Matrix internalMatrix;
    private Matrix returnMatrix;

    private MatrixHasChangedDelegate delegate;
    private OnHoldDelegate onHoldDelegate;

    private Timer touchHoldingTimer;
    private boolean timerRunning;
    private boolean holding;
    private static int HOLD_DELAY = 700;

    private int touchState;
    private final int TOUCH_IDLE = 0;
    private final int TOUCH_DOWN = 1;
    private final int TOUCH_PINCH = 2;

    private float initialTouchDistance;
    private float minimumZoomLevel;
    private float maximumZoomLevel;
    private float currentZoomLevel;

    private float firstTouchX;
    private float firstTouchY;

    private float originalWidth;
    private float originalHeight;
    private float currentWidth;
    private float currentHeight;

    private float leftEdge;
    private float topEdge;

    public PinchToZoomHelper(MatrixHasChangedDelegate delegate) {
        this(1, 3, delegate);
    }

    public PinchToZoomHelper(float minimumZoomLevel, float maximumZoomLevel, MatrixHasChangedDelegate delegate) {
        this.delegate = delegate;

        this.touchHoldingTimer = new Timer();
        this.internalMatrix = new Matrix();
        this.returnMatrix = new Matrix();
        this.minimumZoomLevel = minimumZoomLevel;
        this.maximumZoomLevel = maximumZoomLevel;
        this.currentZoomLevel = this.minimumZoomLevel;
        this.timerRunning = false;

        this.leftEdge = 0;
        this.topEdge = 0;
        this.firstTouchX = 0;
        this.firstTouchY = 0;
    }

    public boolean isHolding() {
        return this.holding;
    }

    public MatrixHasChangedDelegate getMatrixHasChangedDelegate() {
        return this.delegate;
    }

    public float getCurrentZoomLevel() {
        return this.currentZoomLevel;
    }

    public float getOriginalHeight() {
        return this.originalHeight;
    }

    public float getOriginalWidth() {
        return this.originalWidth;
    }

    public float getCurrentHeight() {
        return this.currentHeight;
    }

    public float getCurrentWidth() {
        return this.currentWidth;
    }

    public void setIsHolding(boolean yes) {
        if (this.onHoldDelegate != null) {
            this.onHoldDelegate.onTouchHold(this.firstTouchX, this.firstTouchY);
        }
        this.holding = yes;
    }

    public void setOnHoldDelegate(OnHoldDelegate onHoldDelegate) {
        this.onHoldDelegate = onHoldDelegate;
    }

    public void setFirstTouchCoordinates(float positionX, float positionY) {
        this.holding = false;
        this.firstTouchY = positionY;
        this.firstTouchX = positionX;

        TimerTask task = new HoldTask(this);
        this.touchHoldingTimer.schedule(task, HOLD_DELAY);
        this.timerRunning = true;
    }

    public void clearTimer() {
        if (this.timerRunning) {
            this.touchHoldingTimer.cancel();
            this.touchHoldingTimer = new Timer();
            this.timerRunning = false;
        }
    }

    public void setViewportDimensions(float width, float height) {
        this.originalWidth = width;
        this.originalHeight = height;

        this.currentWidth = originalWidth * this.currentZoomLevel;
        this.currentHeight = originalHeight * this.currentZoomLevel;
    }

    public float[] mapCoordinates(float[] coordinates) {
        this.internalMatrix.mapPoints(coordinates);

        if (coordinates[0] < this.leftEdge) {
            coordinates[0] = -coordinates[0];
        }

        if (coordinates[1] < this.topEdge) {
            coordinates[1] = -coordinates[1];
        }

        return coordinates;
    }

    public boolean receiveTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        this.delegate.onTouchEvent(event);

        if (action == MotionEvent.ACTION_UP) {
            this.touchState = TOUCH_IDLE;
            this.clearTimer();
            return true;
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            this.touchState = TOUCH_PINCH;
            float distanceX = (event.getX(0) - event.getX(1));
            float distanceY = (event.getY(0) - event.getY(1));
            this.initialTouchDistance = FloatMath.sqrt(distanceX * distanceX + distanceY * distanceY);
            return true;
        }

        if (action == MotionEvent.ACTION_POINTER_UP) {
            this.touchState = TOUCH_DOWN;
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (this.touchState == TOUCH_PINCH) {
                float distanceX = (event.getX(0) - event.getX(1));
                float distanceY = (event.getY(0) - event.getY(1));
                float currentTouchDistance = FloatMath.sqrt(distanceX * distanceX + distanceY * distanceY);
                float factor = currentTouchDistance / this.initialTouchDistance;
                if (factor < 1) {
                    factor = factor + (1 - factor) / 20;
                } else {
                    factor = factor - (factor - 1) / 20;
                }

                this.scale(factor, (event.getX(0) + event.getX(1)) / 2, (event.getY(0) + event.getY(1)) / 2);
                return true;
            }

            if (this.touchState == TOUCH_DOWN) {
                this.moveTo(event.getX(), event.getY());
                return true;
            }
        }

        if (action == MotionEvent.ACTION_DOWN) {
            this.touchState = TOUCH_DOWN;
            this.setFirstTouchCoordinates(event.getX(), event.getY());
            return true;
        }

        return false;
    }

    public void scale(float factor, float positionX, float positionY) {
        this.clearTimer();
        if (factor * this.currentZoomLevel > this.maximumZoomLevel) {
            factor = this.maximumZoomLevel / this.currentZoomLevel;
        } else {
            if (factor * this.currentZoomLevel < this.minimumZoomLevel) {
                factor = this.minimumZoomLevel / this.currentZoomLevel;
            }
        }

        if (factor == 1) {
            return;
        }

        this.internalMatrix.postScale(factor, factor, positionX, positionY);
        this.currentZoomLevel = this.currentZoomLevel * factor;

        float[] topLeftPoint = {0, 0};
        this.internalMatrix.mapPoints(topLeftPoint);
        this.leftEdge = topLeftPoint[0];
        this.topEdge = topLeftPoint[1];


        this.currentWidth = this.originalWidth * this.currentZoomLevel;
        this.currentHeight = this.originalHeight * this.currentZoomLevel;

        if (this.leftEdge > 0 && this.topEdge > 0) {
            this.move(this.leftEdge, this.topEdge);
            return;
        } else {
            if (this.leftEdge > 0) {
                this.move(this.leftEdge, 0);
                return;
            } else {
                if (this.topEdge > 0) {
                    this.move(0, this.leftEdge);
                    return;
                }
            }
        }

        delegate.matrixHasChanged(this);
    }

    public void moveTo(float positionX, float positionY) {
        float diffX = (this.firstTouchX - positionX);
        float diffY = (this.firstTouchY - positionY);

        this.move(diffX / this.currentZoomLevel, diffY / this.currentZoomLevel);
    }

    public void move(float moveX, float moveY) {
        this.clearTimer();
        if (this.leftEdge - moveX > 0) {
            moveX = this.leftEdge;
        } else {
            if (-this.leftEdge + moveX + this.originalWidth > this.currentWidth) {
                moveX = this.currentWidth - (-this.leftEdge + this.originalWidth);
            }
        }

        if (this.topEdge - moveY > 0) {
            moveY = this.topEdge;
        } else {
            if (-this.topEdge + moveY + this.originalHeight > this.currentHeight) {
                moveY = this.currentHeight - (-this.topEdge + this.originalHeight);
            }
        }


        if (moveX != 0 || moveY != 0) {
            this.leftEdge -= moveX;
            this.topEdge -= moveY;

            this.firstTouchX -= moveX;
            this.firstTouchY -= moveY;

            this.internalMatrix.postTranslate(-moveX, -moveY);
            delegate.matrixHasChanged(this);
        }
    }

    public Matrix getMatrix() {
        this.returnMatrix.set(this.internalMatrix);
        return this.returnMatrix;
    }
}
