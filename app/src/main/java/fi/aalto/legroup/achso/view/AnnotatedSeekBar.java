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

package fi.aalto.legroup.achso.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

import java.util.List;

import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.annotation.AnnotationSurfaceHandler;

public class AnnotatedSeekBar extends SeekBar {
    //private Annotation lastTouched = null;
    private VideoControllerView mController;
    public boolean suggests_position = false;


    public void setController(VideoControllerView controller) {
        mController = controller;
    }

    public VideoControllerView getController() {
        return this.mController;
    }

    public OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mAnnotationSurfaceHandler.getAnnotations() != null) {
                Annotation closestAnnotation = getAnnotationUnderThumb(progress);
                if (closestAnnotation != null) {
                    suggests_position = true;
                    int new_time = (int) closestAnnotation.getStartTime();
                    setProgress(new_time);
                    mController.playerSeekTo(new_time);
                    // now there may be more than one annotations to show, get them as list
                    final List<Annotation> annotationsToShow = mAnnotationSurfaceHandler
                            .getAnnotationsAppearingBetween(new_time - 10, new_time);
                    mAnnotationSurfaceHandler.showMultiple(annotationsToShow);
                } else {
                    mAnnotationSurfaceHandler.show(null);
                    suggests_position = false;
                }
                mAnnotationSurfaceHandler.draw();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    public Annotation getAnnotationUnderThumb(int progress) {
        Annotation closestAnnotation = null;
        int videoDuration = mController.getDuration();
        int maxRange;
        int w = this.getWidth();
        if (w == 0) { // when called after rotate there is no width
            maxRange = 500;
        } else {
            maxRange = (videoDuration / (this.getWidth() - (this.getThumbOffset() * 2)
            )) * 14;
        }
        for (Annotation a : mAnnotationSurfaceHandler.getAnnotations()) {
            int range = Math.abs(progress - (int) a.getStartTime());
            if (range <= maxRange) {
                maxRange = range;
                closestAnnotation = a;
            }
        }
        return closestAnnotation;
    }

    private AnnotationSurfaceHandler mAnnotationSurfaceHandler;

    public AnnotatedSeekBar(Context context) {
        super(context);
        setOnSeekBarChangeListener(seekBarChangeListener);
        //lastTouched = null;
    }

    public AnnotatedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnSeekBarChangeListener(seekBarChangeListener);
        //lastTouched = null;
    }

    public AnnotatedSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnSeekBarChangeListener(seekBarChangeListener);
        //lastTouched = null;
    }

    public void setAnnotationSurfaceHandler(AnnotationSurfaceHandler h) {
        mAnnotationSurfaceHandler = h;
    }


    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        Paint p = new Paint();
        p.setColor(0xFFEAB674);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setStrokeWidth(1.0f);
        p.setFlags(Paint.ANTI_ALIAS_FLAG);
        int w = this.getWidth() - (this.getThumbOffset() * 2);

        if (mAnnotationSurfaceHandler != null) {
            if (mAnnotationSurfaceHandler.getAnnotations() != null) {
                int video_duration = mController.getDuration();
                for (Annotation a : mAnnotationSurfaceHandler.getAnnotations()) {
                    int thumbx = (int) (((float) a.getStartTime() / video_duration) * w) + this
                            .getThumbOffset();
                    c.drawCircle(thumbx, this.getHeight() / 2, 4.0f, p);
                    p.setColor(0x88EAB674);
                    c.drawCircle(thumbx, this.getHeight() / 2, 14.0f, p);
                }
            }
        }
    }
}
