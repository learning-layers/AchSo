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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.util.xml.XmlConverter;
import fi.aalto.legroup.achso.util.xml.XmlObject;
import fi.aalto.legroup.achso.util.xml.XmlSerializableFactory;

public class RemoteSemanticVideoFactory implements XmlSerializableFactory {
    private SemanticVideo.Genre getGenreFromString(String str) {
        for (Map.Entry<SemanticVideo.Genre, String> e : SemanticVideo.genreStrings.entrySet()) {
            if (e.getValue().equals(str)) {
                return e.getKey();
            }
        }
        return null;
    }

    private SemanticVideo.Genre getGenreFromEnglishString(String str) {
        for (Map.Entry<SemanticVideo.Genre, String> e : SemanticVideo.englishGenreStrings.entrySet()) {
            if (e.getValue().equals(str)) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public RemoteSemanticVideo fromXmlObject(XmlObject obj) {
        String title = null;
        SemanticVideo.Genre genre = null;
        String qrcode = null;
        String creator = null;
        Uri video_uri = null;
        Date created_at = null;
        Location location = null;
        Bitmap image = null;
        List<RemoteAnnotation> remoteAnnotations = new ArrayList<RemoteAnnotation>();
        long duration = 0;
        if (!obj.getName().equals("video")) {
            return null;
        }
        for (XmlObject o : obj.getSubObjects()) {
            String name = o.getName();
            if (name.equals("title")) {
                title = o.getText();
            } else if (name.equals("genre")) {
                genre = getGenreFromEnglishString(o.getText());
            } else if (name.equals("qr_code")) {
                qrcode = o.getText();
            } else if (name.equals("creator")) {
                creator = o.getText();
            } else if (name.equals("video_uri")) {
                video_uri = Uri.parse(o.getText());
            } else if (name.equals("video_url")) {
                video_uri = Uri.parse(o.getText());
            } else if (name.equals("created_at")) {
                created_at = new Date();
                SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                try {
                    created_at = format.parse(o.getText());
                } catch (ParseException e) {
                }
            } else if (name.equals("duration")) {
                duration = Long.parseLong(o.getText(), 10);
            } else if (name.equals("location")) {
                String provider = null;
                double latitude = 0;
                double longitude = 0;
                float accuracy = 0;
                for (XmlObject oo : o.getSubObjects()) {
                    if (oo.getName().equals("provider")) {
                        provider = oo.getText();
                    } else if (oo.getName().equals("latitude")) {
                        latitude = Double.valueOf(oo.getText());
                    } else if (oo.getName().equals("longitude")) {
                        longitude = Double.valueOf(oo.getText());
                    } else if (oo.getName().equals("accuracy")) {
                        accuracy = Float.valueOf(oo.getText());
                    }
                }
                location = new Location(provider);
                location.setAccuracy(accuracy);
                location.setLongitude(longitude);
                location.setLatitude(latitude);
            } else if (name.equals("thumb_image")) {
                String encoding = o.getAttributes().get("encoding");
                if (encoding != null && encoding.equals("base64")) {
                    image = XmlConverter.base64ToBitmap(o.getText());
                } else {
                    Bitmap bitmap = null;
                    try {
                        bitmap = BitmapFactory.decodeStream((InputStream) new URL(o.getText()).getContent());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (bitmap != null) image = bitmap;
                }
            } else if (name.equals("annotations")) {
                for (XmlObject annotation : o.getSubObjects()) {
                    remoteAnnotations.add(new RemoteAnnotationFactory().fromXmlObject(annotation));
                }
            }
        }
        RemoteSemanticVideo ret = new RemoteSemanticVideo(title, created_at, duration, video_uri, genre == null ? 0 : genre.ordinal(), image, image, qrcode, location, true, creator, remoteAnnotations);
        for (RemoteAnnotation ra : remoteAnnotations) {
            ra.setVideo(ret);
        }
        return ret;
    }
}
