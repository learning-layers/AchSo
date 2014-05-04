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
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;


import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.adapter.VideoThumbAdapter;
import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.util.App;


public class RemoteSemanticVideo extends SemanticVideo {
    private List<RemoteAnnotation> mRemoteAnnotations;
    private List<Annotation> mAnnotations;
    private Uri mThumbnailUri;

    public RemoteSemanticVideo(String title, Date createdat, long duration, Uri uri, int genreInt, Bitmap mini, Bitmap micro, String qrcode, Location location, int uploadStatus, String creator, String key, Uri thumbnail_uri, List<RemoteAnnotation> remoteAnnotations) {
        super(-1, title, createdat, duration, uri, genreInt, mini, micro, qrcode, location, uploadStatus, creator, key);
        mRemoteAnnotations = remoteAnnotations;
        mAnnotations = null;
        mThumbnailUri = thumbnail_uri;
        mInLocalDB = false;
        mInCloud = true;
    }

    /**
     * Used to construct remote videos from thin information available from SeViAnno2
     * @param title
     * @param video_url
     * @param thumb_url
     * @param author
     */
    public RemoteSemanticVideo(String title, String video_url, String thumb_url, String author) {
        this(title, new Date(), 0, Uri.parse(video_url), 0, null, null, null, null, 0,
                author, null, Uri.parse(thumb_url), new ArrayList<RemoteAnnotation>());
    }

    public List<Annotation> getAnnotations(Context ctx) {
        if (mAnnotations == null) {
            mAnnotations = new ArrayList<Annotation>();
            if (mRemoteAnnotations != null) {
                for (RemoteAnnotation ra : mRemoteAnnotations) {
                    mAnnotations.add(new Annotation(ctx, ra.getVideo(), ra));
                }
            }
        }
        return mAnnotations;
    }

    public void putThumbnailTo(VideoThumbAdapter.ViewHolder thumbnail_holder) {
        //Log.i("RemoteSemanticVideo", "Loading thumbnail from " + mThumbnailUri.toString() + ", " +
        //        "putting to: " + thumbnail_holder.toString());
        Picasso.with(App.getContext())
                .load(mThumbnailUri)
                .placeholder(R.drawable.circle)
                .error(R.drawable.cross)
                .resize(96,96)
                .centerInside()
                .into(thumbnail_holder);
    }
}
/*
        App.getImageLoader().loadImage(mThumbnailUri.toString(), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                BitmapDrawable thumb = new BitmapDrawable(App.getContext().getResources(), loadedImage);
                Log.i("RemoteSemanticVideo", "got thumbnail, putting to: " + mTempViewHolder.toString());
                VideoThumbAdapter.setThumbBackground(mTempViewHolder, thumb);
                mTempViewHolder = null;
            }
            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                try
                {
                    String fail = failReason.getType().toString();
                    String fail4 = failReason.getCause().toString();
                    String sum = fail + " " + fail4;
                    Log.i("RemoteSemanticVideo", "thumb loading failed:" + sum);
                }
                catch (Exception e)
                {
                    Log.i("RemoteSemanticVideo", "thumb loading failed and failed:" + e.getMessage());
                }
            }
        });
*/
