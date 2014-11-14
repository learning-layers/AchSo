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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.SurfaceView;

import fi.aalto.legroup.achso.database.AnnotationBase;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.SerializableToDB;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.ColorGenerator;
import fi.aalto.legroup.achso.util.TextSettable;

public class Annotation extends AnnotationBase implements TextSettable, SerializableToDB {

    public static final long ANNOTATION_SHOW_DURATION_MILLISECONDS = 3000;
    public static final long MINIMUM_DISPLAY_DURATION = 2000;
    public static final long DISPLAY_DURATION_PER_CHAR = 100;
    public static final long ANNOTATION_FADE_DURATION_MILLISECONDS = 200;
    public static final int ORIGINAL_SIZE = 60;
    boolean mSelected;
    int mOpacity;
    private int mSize = 60;
    private int mColor = Color.RED; // override as orange when instantiated
    private int mColorShadow = Color.BLACK;
    private int mSelectedColor = Color.BLUE;
    private boolean mVisible;
    private boolean mAlive;
    private PointF mRememberedPosition;
    private float mRememberedScaleFactor;
    private boolean mIsSeen = false;

    public Annotation(long videoid, long starttime, String text, PointF position,
                      float scale, String creator, String video_key) {
        super(videoid, starttime, ANNOTATION_SHOW_DURATION_MILLISECONDS, text, position, scale,
                creator, video_key);
        mSelected = false;
        mVisible = false;
        mAlive = true;
        mOpacity = 100;
    }

    public Annotation(Context ctx, SemanticVideo sv, AnnotationBase base) {
        super(-1, base.getStartTime(), base.getDuration(), base.getText(), base.getPosition(),
                (float) 1.0, base.getCreator(), sv.getKey());
        mSelected = false;
        mVisible = false;
        mAlive = true;
        mOpacity = 100;
    }

    public SemanticVideo.Genre getGenre() {
        return VideoDBHelper.getById(getVideoId()).getGenre();
    }

    public int getColor() {
        return ColorGenerator.getSeededColor(mCreator);
    }

    public int getOpacity() {
        return mOpacity;
    }

    public void setOpacity(int opacity) {
        mOpacity = opacity;
    }

    public boolean isSelected() {
        return this.mSelected;
    }

    public void setSelected(boolean value) {
        mSelected = value;
    }

    public long getDuration() {
        return super.getDuration();
    }

    public boolean isVisible() {
        return mVisible;
    }

    public boolean isSeen() {
        return mIsSeen;
    }

    public void setSeen(boolean mIsSeen) {
        this.mIsSeen = mIsSeen;
    }

    public void setVisible(boolean visible) {
        this.mVisible = visible;
        if (mVisible) {
            mOpacity = 100;
        } else {
            mOpacity = 0;
        }
    }

    public long getStartTime() {
        return super.getStartTime();
    }

    public boolean isAlive() {
        return mAlive;
    }

    public void setAlive(boolean b) {
        mAlive = b;
    }

    public PointF getPosition() {
        return super.getPosition();
    }

    public void setPosition(PointF position) {
        super.setPosition(position);
    }

    public long getVideoId() {
        return super.getVideoId();
    }

    public static void drawAnnotationRect(Canvas c, Paint color, Paint shadow, Float posx,
                                          Float posy, int wh, float scale) {
        int wh2 = wh / 2;
        int tilt = wh / 8;
        int adjust = 4; //wh / 16;
        int madjust = (int) (4 * (scale * 2));
        //int madjust = (int) ((wh / 16) * (scale * 2) );
        color.setStyle(Paint.Style.STROKE);
        color.setAntiAlias(true);
        color.setStrokeWidth(8);
        shadow.setStyle(Paint.Style.STROKE);
        shadow.setStrokeWidth(8);
        shadow.setAntiAlias(true);
        float tlx, ty, trx, blx, by, brx;
        tlx = posx - wh2 + tilt;
        ty = posy - wh2;
        trx = posx + wh2 - tilt;
        blx = posx - wh2;
        by = posy + wh2;
        brx = posx + wh2;

        Path p = new Path();
        p.moveTo(tlx + madjust, ty + adjust);
        p.lineTo(trx + madjust, ty + adjust - 4);
        p.lineTo(brx + madjust, by + adjust);
        p.lineTo(blx + madjust, by + adjust);
        p.lineTo(tlx + madjust, ty + adjust);
        p.close();
        c.drawPath(p, shadow);
        p.reset();
        p.moveTo(tlx, ty);
        p.lineTo(trx, ty - 4);
        p.lineTo(brx, by);
        p.lineTo(blx, by);
        p.lineTo(tlx, ty);
        p.close();
        c.drawPath(p, color);

    }

    public void draw(Canvas c, int color) {
        if (mVisible) {
            Paint p = new Paint();
            Paint s = new Paint();
            p.setColor(color);
            s.setColor(mColorShadow);
            int a = (int) (mOpacity / 100f) * 255;
            p.setAlpha(a);
            s.setAlpha((int) (a * 0.7f));
            PointF pos = getPosition();
            float posx = pos.x * c.getWidth();
            float posy = pos.y * c.getHeight();

            mSize = (int) (mScale * ORIGINAL_SIZE);
            drawAnnotationRect(c, p, s, posx, posy, mSize, 1f);
            if (isSelected()) {
                p.setStrokeWidth(0);
                p.setShadowLayer(2, 2, 2, mColorShadow);
                c.drawLine(posx, posy + (mSize / 2) + 2, c.getWidth() / 2, c.getHeight() - 54, p);
            }
        }
    }

    public void draw(Canvas c) {
        this.draw(c, mColor);
    }

    public RectF getBounds(SurfaceView drawnTo) {
        float x = getPosition().x * drawnTo.getWidth();
        float y = getPosition().y * drawnTo.getHeight();
        int s2 = mSize / 2;
        return new RectF(x - s2, y - s2, x + s2, y + s2);
    }

    public void setScaleFactor(float scaleFactor) {
        super.setScaleFactor(scaleFactor);
    }

    public float getScaleFactor() {
        return super.getScaleFactor();
    }

    // If editing is canceled, these are the values to revert to.
    public void rememberState() {
        mRememberedPosition = getPosition();
        mRememberedScaleFactor = getScaleFactor();
    }

    // When editing is canceled:
    public void revertToRemembered() {
        setPosition(mRememberedPosition);
        setScaleFactor(mRememberedScaleFactor);
    }


    public static enum Button {
        Left,
        Right
    }
}
