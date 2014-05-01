package fi.aalto.legroup.achso.annotation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.view.VideoControllerView;

/**
 * Created by purma on 30.4.2014.
 */
public class IncomingAnnotationMarker {


    private final Bitmap mBitmap;
    private final int ORIGINAL_SIZE = 80;
    private final float max_size = 8;
    private final int DURATION = 400;
    private final int STEP_SIZE = 20;
    private FloatPosition mPosition;
    private int counter;
    private AnnotationSurfaceHandler mSurface;
    private VideoControllerView mListener;
    private Handler handler;
    private boolean mVisible;
    private Runnable animationFrame = new Runnable() {
        @Override
        public void run() {
            counter += STEP_SIZE;
            if (counter >= DURATION) {
                mVisible = false;
                mListener.addAnnotationToPlace(mPosition);
            } else {
                mSurface.draw();
                handler.postDelayed(this, STEP_SIZE);
            }
        }
    };

    public IncomingAnnotationMarker(Context ctx, AnnotationSurfaceHandler surface, VideoControllerView listener, FloatPosition position) {
        mPosition = position;
        Bitmap tmp = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.square_large);
        mBitmap = Bitmap.createScaledBitmap(tmp, ORIGINAL_SIZE, ORIGINAL_SIZE, false);
        tmp.recycle();
        mSurface = surface;
        mListener = listener;
        mVisible = false;
    }

    public void startAnimation() {
        if (handler == null) {
            handler = new Handler();
        }
        counter = 0;
        mVisible = true;
        handler.postDelayed(animationFrame, STEP_SIZE);
    }

    public void stopAnimation() {
        handler.removeCallbacks(animationFrame);
        mVisible = false;
        mSurface.draw();
    }

    public void draw(Canvas c) {
        if (!mVisible) {
            return;
        }
        Paint p = new Paint();
        float v = (float) counter / (float) DURATION;
        float size_m = 1 + ((max_size - 1) * (1 - v));
        p.setAlpha((int) (255 * v));
        float posx = mPosition.getX() * c.getWidth();
        float posy = mPosition.getY() * c.getHeight();
        int wh = (int) (size_m * ORIGINAL_SIZE);
        int wh2 = wh / 2;
        Rect nr = new Rect((int) posx - wh2, (int) posy - wh2, (int) posx + wh2, (int) posy + wh2);
        c.drawBitmap(mBitmap, null, nr, p);
    }

    public void setPosition(FloatPosition position) {
        mPosition = position;
    }
}
