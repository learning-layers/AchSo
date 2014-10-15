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

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SubtitleManager {
    private static SubtitleManager mInstance;
    private static TextView mSubtitleTextView;
    private static ConcurrentLinkedQueue<String> mSubtitles;

    private static HashMap<String, Integer> mColorsForText;

    static {
        mInstance = new SubtitleManager();
        mSubtitleTextView = null;
        mSubtitles = new ConcurrentLinkedQueue<String>();
    }

    private static int mColor = Color.RED;

    private SubtitleManager() {
    }

    public static void setSubtitleTextView(TextView view) {
        mSubtitleTextView = view;
    }

    public static TextView getSubtitleTextView() {
        return mSubtitleTextView;
    }

    public static void addSubtitle(String subtitle) {
        addSubtitle(subtitle, mColor);
    }

    public static void addSubtitle(String subtitle, int color) {
        if (!mSubtitles.contains(subtitle)) {
            mColorsForText.put(subtitle, color);
            mSubtitles.add(subtitle);
        }
    }

    public static void setColor(int color) {
        mColor = color;
        mSubtitleTextView.setTextColor(mColor);
    }

    public static void replaceSubtitle(String from, String to) {
        if (!mSubtitles.contains(from)) return;

        // Rebuilds the queue as elements cannot be replaced.
        // Shouldn't matter much as the queue usually contains maximum of 3 elements or so.
        ConcurrentLinkedQueue<String> newSubtitles = new ConcurrentLinkedQueue<String>();
        synchronized (mSubtitles) {
            Iterator i = mSubtitles.iterator();
            while (i.hasNext()) {
                String str = (String) i.next();
                if (str.equals(from)) newSubtitles.add(to);
                else newSubtitles.add(str);
            }
            mSubtitles = newSubtitles;
        }
    }

    public static void removeSubtitle(String subtitle) {
        mSubtitles.remove(subtitle);
        mColorsForText.remove(subtitle);
    }

    public static void updateVisibleSubtitles() {
        SpannableStringBuilder sub = new SpannableStringBuilder("");
        for (String s : mSubtitles) {

            if (s.equals("")) {
                continue;
            }
            SpannableString currentSubtitle = new SpannableString(s + "\n");
            currentSubtitle.setSpan(new ForegroundColorSpan(mColorsForText.get(s)),0, s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sub.append(currentSubtitle);
        }

        if (mSubtitleTextView != null) {
            mSubtitleTextView.setText(sub, TextView.BufferType.SPANNABLE);
        }

    }

    public static void clearSubtitles() {
        mSubtitles = new ConcurrentLinkedQueue<String>();
        mColorsForText = new HashMap<String, Integer>();
    }
}
