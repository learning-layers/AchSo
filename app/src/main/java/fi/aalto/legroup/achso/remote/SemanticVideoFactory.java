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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.xml.XmlConverter;
import fi.aalto.legroup.achso.util.xml.XmlObject;
import fi.aalto.legroup.achso.util.xml.XmlSerializableFactory;

public class SemanticVideoFactory implements XmlSerializableFactory {
    private SemanticVideo.Genre getGenreFromString(String str) {
        for (Map.Entry<SemanticVideo.Genre, String> e : SemanticVideo.genreStrings.entrySet()) {
            if (e.getValue().equals(str)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static SemanticVideo.Genre getGenreFromEnglishString(String str) {
        for (Map.Entry<SemanticVideo.Genre, String> e : SemanticVideo.englishGenreStrings.entrySet()) {
            if (e.getValue().equals(str)) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public SemanticVideo fromXmlObject(XmlObject obj) {
        Log.i("RemoteSemanticVideoFactory", "Parsing xmlobject:" + obj.toString());
        String title = null;
        SemanticVideo.Genre genre = null;
        String qrcode = null;
        String creator = null;
        String remote_video = null;
        Date created_at = null;
        Location location = null;
        Bitmap image = null;
        String key = null;
        String remote_thumbnail = null;
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
                remote_video = o.getText();
            } else if (name.equals("video_url")) {
                remote_video = o.getText();
            } else if (name.equals("created_at")) {
                created_at = new Date();
                //SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSZZZZZ");
                // Example1: 2014-03-07T11:23:51:724+01:00
                // Example2: 2013-12-10T21:34:51:186+01:00
                // Pattern:  yyyy-MM-dd'T'HH:mm:ss:SSSZZZZZ
                try {
                    created_at = format.parse(o.getText());
                } catch (ParseException e) {
                    Log.i("RemoteSemanticVideoFactory", "Date parsing error:" + e.getMessage());
                    Log.i("RemoteSemanticVideoFactory", o.getText());
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
            } else if (name.equals("thumbnail")) {
                remote_thumbnail = o.getText();
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
            } else if (name.equals("key")) {
                key = o.getText();
            }
        }
        SemanticVideo sem = new SemanticVideo(-1, title, created_at, duration, null,
                genre, image, image, qrcode, location,
                SemanticVideo.UPLOADED, creator, key, remote_video, remote_thumbnail);

        if (remoteAnnotations != null && remoteAnnotations.size() > 0) {
            sem.setRemoteAnnotations(remoteAnnotations);
            for (RemoteAnnotation ra : remoteAnnotations) {
                ra.setVideo(sem);
            }

        }
        return sem;
    }

    public static SemanticVideo buildFromJSON(JSONObject obj) {
        String title = null;
        SemanticVideo.Genre genre = null;
        String qrcode = null;
        String creator = null;
        String remote_video = null;
        Date created_at = null;
        Location location = null;
        String key = null;
        String remote_thumbnail = null;
        long duration = 0;
        try {
            if (obj.has("title")) {
                title = obj.getString("title");
            }
            if (obj.has("genre")) {
                genre = getGenreFromEnglishString(obj.getString("genre"));
            }
            if (obj.has("qr_code")) {
                qrcode = obj.getString("qr_code");
            }
            if (obj.has("creator")) {
                creator = obj.getString("creator");
            }
            if (obj.has("video_uri")) {
                remote_video = obj.getString("video_uri");
            }
            if (obj.has("thumb_uri")) {
                remote_thumbnail = obj.getString("thumb_uri");
                Log.i("SemanticVideoFactory", "Thumb uri: "+remote_thumbnail);
            }
            if (obj.has("created_at")) {
                String datestring = obj.getString("created_at");
                datestring = datestring.replaceAll("Z$", "+0000");
                try {
                    created_at = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).parse
                            (datestring);
                } catch (ParseException e) {
                    e.printStackTrace();
                    created_at = new Date();
                }
            }
            if (obj.has("location")) {
                JSONArray location_array = obj.getJSONArray("location");
                location = new Location("AchSo");
                location.setLongitude(location_array.getDouble(0));
                location.setLatitude(location_array.getDouble(1));
            }
            if (obj.has("key")) {
                key = obj.getString("key");
            }
            if (obj.has("duration")) {
                duration = obj.getLong("duration");
            }


        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        SemanticVideo sv = VideoDBHelper.getByKey(key);
        if (sv != null) { // Update objects that are also in local database
            sv.setInCloud(true);
            sv.setInLocalDB(true);
            sv.setTitle(title);
            sv.setGenre(genre);
            sv.setQrCode(qrcode);
            sv.setLocation(location);
            sv.setUploadStatus(SemanticVideo.UPLOADED);
            sv.setRemoteVideo(remote_video);
            sv.setRemoteThumbnail(remote_thumbnail);
            VideoDBHelper vdb = new VideoDBHelper(App.getContext());
            vdb.update(sv);
            vdb.close();
        } else { // Create new objects, don't put them to local database
            sv = new SemanticVideo(-1, title, created_at, duration, null, genre, null,
                    null, qrcode, location, SemanticVideo.UPLOADED, creator, key, remote_video,
                    remote_thumbnail);
            sv.setInCloud(true);
            sv.setInLocalDB(false);
        }

        return sv;
    }
}
