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

package fi.aalto.legroup.achso.remote;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.database.SemanticVideo;

public class RemoteSemanticVideo extends SemanticVideo {
    private List<RemoteAnnotation> mRemoteAnnotations;
    private List<Annotation> mAnnotations;

    RemoteSemanticVideo(String title, Date createdat, long duration, Uri uri, int genreInt, Bitmap mini, Bitmap micro,
                        String qrcode, Location location, boolean uploaded, String creator, List<RemoteAnnotation> remoteAnnotations) {
        super(-1, title, createdat, duration, uri, genreInt, mini, micro, qrcode, location, uploaded, creator);
        mRemoteAnnotations = remoteAnnotations;
        mAnnotations = null;
    }

    public List<Annotation> getAnnotations(Context ctx) {
        if (mAnnotations == null) {
            mAnnotations = new ArrayList<Annotation>();
            for (RemoteAnnotation ra : mRemoteAnnotations) {
                mAnnotations.add(new Annotation(ctx, ra.getVideo(), ra));
            }
        }
        return mAnnotations;
    }
}
