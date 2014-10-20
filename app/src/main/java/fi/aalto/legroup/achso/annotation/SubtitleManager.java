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

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import fi.aalto.legroup.achso.R;

public class SubtitleManager {

    private static LinkedList<TextView> freeTextViews = new LinkedList<TextView>();
    private static Map<Annotation, TextView> textViewsInUse = new HashMap<Annotation, TextView>();
    private static LinearLayout subtitleContainer;
    private static LayoutInflater inflater;

    public static TextView textViewForSubtitle(Annotation a) {
        if (textViewsInUse.containsKey(a)) {
            return textViewsInUse.get(a);
        }

        TextView text;

        if (freeTextViews.isEmpty()) {
            text = (TextView) inflater.inflate(R.layout.subtitle, subtitleContainer, false);
        } else {
            text = freeTextViews.pop();
        }

        textViewsInUse.put(a, text);

        return text;
    }

    public static void setSubtitleContainer(LinearLayout container) {
        subtitleContainer = container;
        inflater = LayoutInflater.from(subtitleContainer.getContext());
    }

    public static void addSubtitleForAnnotation(Annotation a) {
        TextView text = textViewForSubtitle(a);

        text.setTextColor(a.getColor());
        text.setText(a.getText());
        text.setVisibility(View.VISIBLE);

        if (text.getParent() != subtitleContainer) {
            subtitleContainer.addView(text);
        }
    }

    public static void removeSubtitleForAnnotation(Annotation a) {
        if (textViewsInUse.containsKey(a)) {
            TextView text = textViewsInUse.get(a);

            subtitleContainer.removeView(text);
            textViewsInUse.remove(a);
            freeTextViews.add(text);
        }
    }

    public static void clearAllSubtitles() {
        if (subtitleContainer != null) {
            subtitleContainer.removeAllViews();
        }

        for (TextView text : textViewsInUse.values()) {
            freeTextViews.add(text);
        }

        textViewsInUse.clear();
    }

}
