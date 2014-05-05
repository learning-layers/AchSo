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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceView;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.AnnotationBase;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.SerializableToDB;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.util.TextSettable;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class Annotation extends AnnotationBase implements TextSettable, SerializableToDB {

    public static final long ANNOTATION_SHOW_DURATION_MILLISECONDS = 3000;
    public static final long ANNOTATION_FADE_DURATION_MILLISECONDS = 200;
    public static final int ORIGINAL_SIZE = 80;
    boolean mSelected;
    int mOpacity;
    private int mSize = 80;
    private int mColor = Color.RED;
    private int mSelectedColor = Color.BLUE;
    private boolean mVisible;
    private boolean mAlive;
    private Bitmap mBitmap;
    private FloatPosition mRememberedPosition;
    private float mRememberedScaleFactor;

    public Annotation(Context ctx, long videoid, long starttime, String text,
                      FloatPosition position, float scale, String creator) {
        super(videoid, starttime, ANNOTATION_SHOW_DURATION_MILLISECONDS, text, position, scale,
                creator);
        mSelected = false;
        mVisible = false;
        mAlive = true;
        mOpacity = 100;
        createAnnotationBitmap(ctx);
    }

    public Annotation(Context ctx, SemanticVideo sv, AnnotationBase base) {
        super(-1, base.getStartTime(), base.getDuration(), base.getText(), base.getPosition(),
                (float) 1.0, base.getCreator());
        createAnnotationBitmap(ctx);
        mSelected = false;
        mVisible = false;
        mAlive = true;
        mOpacity = 100;
    }

    private void createAnnotationBitmap(Context ctx) {
        Bitmap tmp = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.square_large);
        mBitmap = Bitmap.createScaledBitmap(tmp, mSize, mSize, false);
        tmp.recycle();
    }

    public SemanticVideo.Genre getGenre() {
        return VideoDBHelper.getById(getVideoId()).getGenre();
    }

    public int getColor() {
        return mSelected ? mSelectedColor : mColor;
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

    public long getDuration() {
        return super.getDuration();
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(boolean visible) {
        this.mVisible = visible;
        if (mVisible) {
            mOpacity = 100;
        } else {
            mOpacity = 0;
            if (mText != null) SubtitleManager.removeSubtitle(mText);
        }
    }

    public long getStartTime() {
        return super.getStartTime();
    }

    public boolean isAlive() {
        return mAlive;
    }

    public void setAlive(boolean b) {
        if (!b) {
            if (mVisible && mText != null) {
                SubtitleManager.removeSubtitle(mText);
            }
        }
        mAlive = b;
    }

    public FloatPosition getPosition() {
        return super.getPosition();
    }

    public void setPosition(FloatPosition p) {
        super.setPosition(p);
    }

    public long getVideoId() {
        return super.getVideoId();
    }

    public void setText(String text) {
        if (mText != null && mVisible && text != null) {
            SubtitleManager.replaceSubtitle(mText, text);
        }
        appendLog(String.format("Edited annotation text at %s to %s", this.toString(), text) );
        super.setText(text);
    }

    public void draw(Canvas c) {
        if (mVisible) {
            Paint p = new Paint();
            p.setColor(mColor);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3.0f);
            p.setAlpha((int) (mOpacity * 2.55));
            FloatPosition pos = getPosition();
            float posx = pos.getX() * c.getWidth();
            float posy = pos.getY() * c.getHeight();
            //Log.i("Annotation", "Drawing annotation -- canvas y:" + posy + "(" + pos.getY() + ") canvas height:" + c.getHeight());
            //c.drawBitmap(mBitmap, posx - (mSize / 2), posy - (mSize / 2), p);

            mSize = (int) (mScale * ORIGINAL_SIZE);
            int wh2 = mSize / 2;
            Rect nr = new Rect((int) posx - wh2, (int) posy - wh2, (int) posx + wh2, (int) posy + wh2);
            c.drawBitmap(mBitmap, null, nr, p);
            if (mText != null) SubtitleManager.addSubtitle(mText);

        }
    }

    public RectF getBounds(SurfaceView drawnTo) {
        float x = getPosition().getX() * drawnTo.getWidth();
        float y = getPosition().getY() * drawnTo.getHeight();
        return new RectF(x - mSize, y - mSize, x + mSize, y + mSize);
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
