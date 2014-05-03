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

package fi.aalto.legroup.achso.database;

import android.content.Context;

import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.util.xml.XmlObject;
import fi.aalto.legroup.achso.util.xml.XmlSerializable;

public class AnnotationBase implements XmlSerializable {
    // Modify VideoDBHelper database in case of adding/removing/modifying members of this class!

    protected long mVideoId;
    protected String mText;
    protected float mScale;
    private long mId;
    private long mStartTime;
    private long mDuration;
    private FloatPosition mPosition;

    public AnnotationBase(long videoid, long starttime, long duration, String text, FloatPosition position, float scale) {
        mVideoId = videoid;
        mStartTime = starttime;
        mDuration = duration;
        mPosition = position;
        mText = text;
        mScale = scale;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public long getId() {
        return mId;
    }

    void setId(long id) {
        mId = id;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setEndTime(int time) {
        if (time > mStartTime) {
            mDuration = time - mStartTime;
        }
    }

    public long getVideoId() {
        return mVideoId;
    }

    public FloatPosition getPosition() {
        return mPosition;
    }

    public void setPosition(FloatPosition p) {
        mPosition = p;
    }

    @Override
    public XmlObject getXmlObject(Context ctx) {
        FloatPosition pos = getPosition();
        return new XmlObject("annotation").addSubObject("text", getText()).addSubObject("x_position", Float.toString(pos.getX())).addSubObject("y_position", Float.toString(pos.getY())).addSubObject("start_time", Long.toString(getStartTime())).addSubObject("duration", Long.toString(getDuration()));
    }

    public float getScaleFactor() {
        return mScale;
    }

    public void setScaleFactor(float scaleFactor) {
        mScale = scaleFactor;
    }
}
