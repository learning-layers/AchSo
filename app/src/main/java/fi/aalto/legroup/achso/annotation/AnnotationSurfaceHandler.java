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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.view.VideoControllerView;

public class AnnotationSurfaceHandler {
    public static final String[] ANNOTATION_COLORS = {"#607d8b", "#9c27b0", "#673ab7", "#259b24", "#cddc39", "#ffeb3b", "#ff5722"};

    private SurfaceView mAnnotationSurface;
    private ConcurrentLinkedQueue<Annotation> mAnnotations;
    private long mVideoId;
    private ArrayList<String> mAnnotationCreators;
    private Context mContext;
    private IncomingAnnotationMarker mIncoming;

    public AnnotationSurfaceHandler(Context c, SurfaceView surface, long videoId) {
        Log.i("AnnotationSurfaceHandler", "Creating instance of AnnotationSurfaceHandler");
        mContext = c;
        this.mAnnotationSurface = surface;
        VideoDBHelper vdb = new VideoDBHelper(c);
        mAnnotationCreators = new ArrayList<String>();
        mAnnotations = new ConcurrentLinkedQueue<Annotation>();
        for (Annotation a : vdb.getAnnotationsById(videoId)) {
            mAnnotations.add(a);
            String currentCreator = a.getCreator();
            if (mAnnotationCreators.indexOf(currentCreator) == -1) {
                mAnnotationCreators.add(currentCreator);
            }

            a.setColor(this.colorForCreator(currentCreator));
        }
        vdb.close();
        mVideoId = videoId;
    }

    public int colorForCreator(String creator) {
        JsonObject userInfo = App.loginManager.getUserInfo();
        String currentUser = null;
        if (userInfo != null && userInfo.has("preferred_username")) {
            currentUser = userInfo.get("preferred_username").getAsString();
            if (currentUser.equals(creator)) {
                return  Color.parseColor(ANNOTATION_COLORS[0]);
            }
        }

        int pos = mAnnotationCreators.indexOf(creator);
        pos++;
        while (pos > ANNOTATION_COLORS.length) {
            pos = pos - ANNOTATION_COLORS.length;
        }
        return Color.parseColor(ANNOTATION_COLORS[pos]);
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
        // add here code to analyse if subtitles overlap with annotations and move subtitles if necessary

    }

    public Annotation addAnnotation(long time, FloatPosition pos) {
        VideoDBHelper vdb = new VideoDBHelper(mContext);
        JsonObject userInfo = App.loginManager.getUserInfo();
        String creator = null;

        if (userInfo != null && userInfo.has("preferred_username")) {
            creator = userInfo.get("preferred_username").getAsString();
        }

        // FIXME: This null has to be replaced with a proper key if we're annotating a remote video
        Annotation a = new Annotation(mVideoId, time, "", pos, 1.0f, creator, null);

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
        for (Annotation ann : mAnnotations) {
            ann.setSelected(false);
        }
        if (a != null) {
            a.setSelected(true);
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
        int x = (int) (pos.getX() * mAnnotationSurface.getWidth());
        int y = (int) (pos.getY() * mAnnotationSurface.getHeight());
        Annotation found = null;
        for (Annotation a : mAnnotations) {
            if (a.isVisible() && a.getBounds(mAnnotationSurface).contains(x, y)) {
                if (found != null) {
                    if (found.getScaleFactor() > a.getScaleFactor()) {
                        found = a;
                    }
                } else {
                    found = a;
                }
            }
        }
        return found;
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
        if (annotationList == null) return;
        for (Annotation a : mAnnotations) {
            if (annotationList.contains(a)) {
                a.setVisible(true);
            } else
                a.setVisible(false);
        }
    }

    public void showAnnotationsAt(int pos) {
        long lpos = (long) pos;
        for (Annotation a : mAnnotations) {
            if (a.getStartTime() == lpos) {
                a.setVisible(true);
            } else {
                a.setVisible(false);
            }
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

    public void hideAllAnnotations() {
        for (final Annotation a : getAnnotations()) {
            a.setVisible(false);
        }
    }

    public List<Annotation> getAnnotationsAt(long pos) {
        List<Annotation> result = new ArrayList<Annotation>();

        for (final Annotation a : mAnnotations) {
            if (pos >= a.getStartTime() && !a.isSeen()) {
                a.setSeen(true);
                result.add(a);
            } else {
                a.setVisible(false);
            }
        }

        return result;
    }

    public void resetSeenFlagsAfterSeek(long pos) {
        for (Annotation annotation : mAnnotations) {
            if (annotation.getStartTime() > pos) {
                annotation.setSeen(false);
            } else {
                annotation.setSeen(true);
            }
        }
    }

}
