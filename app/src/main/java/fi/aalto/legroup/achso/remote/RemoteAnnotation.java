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

package fi.aalto.legroup.achso.remote;

import android.graphics.PointF;

import fi.aalto.legroup.achso.database.AnnotationBase;
import fi.aalto.legroup.achso.database.SemanticVideo;

public class RemoteAnnotation extends AnnotationBase {
    private SemanticVideo mVideo;

    public RemoteAnnotation(long starttime, long duration, String text, PointF position,
                            float scale, String creator) {
        super(-1, starttime, duration, text, position, scale, creator, null);
        mVideo = null;
    }

    public SemanticVideo getVideo() {
        return mVideo;
    }

    public void setVideo(SemanticVideo sv) {
        mVideo = sv;
    }
}
