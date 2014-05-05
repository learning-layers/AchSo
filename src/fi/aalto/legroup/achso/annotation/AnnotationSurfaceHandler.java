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

package fi.aalto.legroup.achso.annotation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.view.VideoControllerView;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class AnnotationSurfaceHandler {

    private SurfaceView mAnnotationSurface;
    private ConcurrentLinkedQueue<Annotation> mAnnotations;
    private long mVideoId;
    private Context mContext;
    private IncomingAnnotationMarker mIncoming;

    public AnnotationSurfaceHandler(Context c, SurfaceView surface, long videoid) {
        Log.i("AnnotationSurfaceHandler", "Creating instance of AnnotationSurfaceHandler");
        mContext = c;
        this.mAnnotationSurface = surface;
        VideoDBHelper vdb = new VideoDBHelper(c);
        mAnnotations = new ConcurrentLinkedQueue<Annotation>();
        for (Annotation a : vdb.getAnnotations(videoid)) {
            mAnnotations.add(a);
        }
        vdb.close();
        mVideoId = videoid;
    }

    public void draw() {
        SurfaceHolder sh = mAnnotationSurface.getHolder();
        if (sh != null) {
            Canvas c = sh.lockCanvas();
            if (c != null) {
                //Log.i("AnnotationSurfaceHandler", String.format("draw(): SurfaceHolder: %s , canvas w: %s, h: %s", sh.toString(), c.getWidth(), c.getHeight()));
                c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                for (Annotation ann : mAnnotations) {
                    ann.draw(c);
                }
                if (mIncoming != null) {
                    mIncoming.draw(c);
                }
                sh.unlockCanvasAndPost(c);
            }
        }
        SubtitleManager.updateVisibleSubtitles();
        TextView sv = SubtitleManager.getSubtitleTextView();
        // add here code to analyse if subtitles overlap with annotations and move subtitles if necessary

    }

    public void draw(Annotation a) {
        SurfaceHolder sh = mAnnotationSurface.getHolder();
        if (sh != null) {
            Canvas c = sh.lockCanvas();
            if (c != null) {
                //Log.i("AnnotationSurfaceHandler", String.format("draw(a): SurfaceHolder: %s , canvas w: %s, h: %s", sh.toString(), c.getWidth(), c.getHeight()));
                a.draw(c);
                sh.unlockCanvasAndPost(c);
            }
        }
        SubtitleManager.updateVisibleSubtitles();
        TextView sv = SubtitleManager.getSubtitleTextView();
        // add here code to analyse if subtitles overlap with annotations and move subtitles if necessary

    }

    public void drawIncoming() {
        SurfaceHolder sh = mAnnotationSurface.getHolder();
        if (sh != null) {
            Canvas c = sh.lockCanvas();
            if (c!= null) {
                mIncoming.draw(c);
                sh.unlockCanvasAndPost(c);
            }
        }
    }

    public Annotation addAnnotation(long time, FloatPosition pos) {
        VideoDBHelper vdb = new VideoDBHelper(mContext);
        Annotation a = new Annotation(mContext, mVideoId, time, "", pos, (float) 1.0,
                App.getUsername());
        a.setVisible(true);
        mAnnotations.add(a);
        vdb.insert(a);
        vdb.close();
        return a;
    }

    public Annotation getAnnotation(float annotationId) {
        for (Annotation a : mAnnotations) {
            if (a.getId() == annotationId)
                return a;
        }
        return null;
    }

    public void removeAnnotation(Annotation a) {
        a.setAlive(false);
        mAnnotations.remove(a);
        VideoDBHelper vdb = new VideoDBHelper(mContext);
        vdb.delete(a);
        vdb.close();
    }

    public void select(Annotation a) {
        if (a == null) {
            for (Annotation ann : mAnnotations) {
                ann.mSelected = false;
            }
        } else if (!a.isSelected()) {
            for (Annotation ann : mAnnotations) {
                ann.mSelected = false;
            }
            a.mSelected = true;
        }
    }

    public ConcurrentLinkedQueue<Annotation> getAnnotations() {
        return mAnnotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        mAnnotations.clear();
        for (Annotation a : annotations) {
            mAnnotations.add(a);
        }
    }

    public Annotation getAnnotation(FloatPosition pos) {
        for (Annotation a : mAnnotations) {
            if (a.getBounds(mAnnotationSurface).contains(pos.getX() * mAnnotationSurface.getWidth(),
                    pos.getY() * mAnnotationSurface.getHeight())) {
                if (a.isVisible())
                    return a;
            }
        }
        return null;
    }

    public void show(Annotation ann) {
        for (Annotation a : mAnnotations) {
            if (a.equals(ann)) {
                a.setVisible(true);
            } else
                a.setVisible(false);
        }
    }

    public void showMultiple(List<Annotation> annotationList) {
        for (Annotation a : mAnnotations) {
            if (annotationList.contains(a)) {
                a.setVisible(true);
            } else
                a.setVisible(false);
        }
    }

    public void startRectangleAnimation(FloatPosition position,
                                        VideoControllerView controller) {
        if (mIncoming != null) {
            stopRectangleAnimation();
            Log.i("AnnotationSurfaceHandler", "Stopping ongoing animation");
        }
        mIncoming = new IncomingAnnotationMarker(mContext, this, controller, position);
        mIncoming.startAnimation();

    }

    public void stopRectangleAnimation() {
        if (mIncoming != null) {
            mIncoming.stopAnimation();
            mIncoming = null;
        }
    }

    public void moveRectangleAnimation(FloatPosition position) {
        if (mIncoming != null) {
            mIncoming.setPosition(position);
        }
    }

    public List<Annotation> getAnnotationsAppearingBetween(long prev_moment, long now) {
        List<Annotation> result = new ArrayList<Annotation>();
        for (final Annotation a : getAnnotations()) {
            final long aTime = a.getStartTime();
            // mLastPos is the position from previous onProgressChanged
            // This returns true for position that is after the annotation start point,
            // but where previous position was before the annotation start point.
            if (now >= aTime && prev_moment < aTime && a.isAlive()) {
                result.add(a);
            } else {
                a.setVisible(false);
            }
        }
        return result;
    }

    public void hideAnnotationsNotAppearingBetween(long prev_moment, long now) {
        for (final Annotation a : getAnnotations()) {
            final long aTime = a.getStartTime();
            a.setVisible((now >= aTime && prev_moment < aTime && a.isAlive()));
        }

    }
}
