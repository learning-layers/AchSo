/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.annotation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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


    private final int ORIGINAL_SIZE = 60;
    private final float max_size = 8;
    private final int DURATION = 300;
    private final int STEP_SIZE = 10;
    private final int mColorOrange;
    private final int mColorShadow;
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
        mSurface = surface;
        mListener = listener;
        mVisible = false;
        mColorOrange = ctx.getResources().getColor(R.color.orange_square);
        mColorShadow = Color.BLACK;
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
        Paint s = new Paint();
        float v = (float) counter / (float) DURATION;
        float size_m = 1 + ((max_size - 1) * (1 - v));
        p.setColor(mColorOrange);
        s.setColor(mColorShadow);
        p.setAlpha((int) (255 * v));
        s.setAlpha((int) (150 * v));
        float posx = mPosition.getX() * c.getWidth();
        float posy = mPosition.getY() * c.getHeight();
        int wh = (int) (size_m * ORIGINAL_SIZE);
        Annotation.drawAnnotationRect(c, p, s, posx, posy, wh, size_m);
    }

    public void setPosition(FloatPosition position) {
        mPosition = position;
    }
}
