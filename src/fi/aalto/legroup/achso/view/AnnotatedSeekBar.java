/**
 * Copyright 2013 Aalto university, see AUTHORS
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
import android.widget.SeekBar;

import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.annotation.AnnotationSurfaceHandler;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;

public class AnnotatedSeekBar extends SeekBar {

    public OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            AnnotatedSeekBar sb = (AnnotatedSeekBar) seekBar;
            if (fromUser && mAnnotationSurfaceHandler.getAnnotations() != null) {
                long maxRange = 500;
                long closestAnnotationx = 0;
                Annotation closestAnnotation = null;
                for (Annotation a : mAnnotationSurfaceHandler.getAnnotations()) {
                    SemanticVideo sv = VideoDBHelper.getById(a.getVideoId());
                    long videoDuration = sv.getDuration(getContext());
                    long progressx = (progress * videoDuration) / 1000L;
                    int range = Math.abs((int) progressx - (int) a.getStartTime());
                    if (range <= maxRange) {
                        closestAnnotationx = (int) (((float) a.getStartTime() / videoDuration) * 1000);
                        maxRange = range;
                        closestAnnotation = a;
                    }
                }
                if (closestAnnotation != null) {
                    sb.setProgress((int) closestAnnotationx);
                    mAnnotationSurfaceHandler.show(closestAnnotation);
                } else {
                    mAnnotationSurfaceHandler.show(null);
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
    private AnnotationSurfaceHandler mAnnotationSurfaceHandler;

    public AnnotatedSeekBar(Context context) {
        super(context);
        setOnSeekBarChangeListener(seekBarChangeListener);
    }

    public AnnotatedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnSeekBarChangeListener(seekBarChangeListener);
    }

    public AnnotatedSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnSeekBarChangeListener(seekBarChangeListener);
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
        if (mAnnotationSurfaceHandler != null) {
            if (mAnnotationSurfaceHandler.getAnnotations() != null) {
                for (Annotation a : mAnnotationSurfaceHandler.getAnnotations()) {
                    SemanticVideo sv = VideoDBHelper.getById(a.getVideoId());
                    int thumbx = (int) (((float) a.getStartTime() / sv.getDuration(getContext())) * this.getWidth());
                    c.drawCircle(thumbx, this.getHeight() / 2, 4.0f, p);
                    p.setColor(0x88EAB674);
                    c.drawCircle(thumbx, this.getHeight() / 2, 14.0f, p);
                }
            }
        }
    }
}
