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
    private String mCreator;
    private long mId;
    private long mStartTime;
    private long mDuration;
    private FloatPosition mPosition;

    public AnnotationBase(long videoid, long starttime, long duration, String text,
                          FloatPosition position, float scale, String creator) {
        mVideoId = videoid;
        mCreator = creator;
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

    public String getCreator() { return mCreator; }

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
        if (p.getY() > 1.0f) {
            p.setY(1.0f);
        } else if (p.getY() < 0f) {
            p.setY(0f);
        }
        if (p.getX() > 1.0f) {
            p.setX(1.0f);
        } else if (p.getX() < 0f) {
            p.setX(0f);
        }

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
