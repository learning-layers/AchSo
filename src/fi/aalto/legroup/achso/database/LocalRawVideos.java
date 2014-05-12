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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class provides access to video files stored on device. These may vary on device and it
 * can be expensive to move large files around, so let this class do its thing.
 */
public class LocalRawVideos {

    public static File getNewOutputFile() {
        // Files stored in this directory will not be removed when the program is uninstalled. This is the recommended location in Android SDK manuals.
        File storagedir = LocalRawVideos.getVideoStorage();
        if(storagedir!=null && !storagedir.exists()) {
            if(!storagedir.mkdirs()) {
                Log.e("AnnotatedVideos", "failed to create directory");
                return null;
            }
        }
        else if(storagedir==null) return null;
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
        return new File(storagedir.getPath(),  timeStamp + ".mp4");
    }
    public static File getVideoStorage() {
        if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "AnnotatedVideos");
        } else return null;
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        ContentResolver cr = context.getContentResolver();

        String data = MediaStore.Images.Media.DATA;
        String[] da = {data};
        Cursor cursor = cr.query(contentUri, da, null, null, null);
        if (cursor == null) {
            data = MediaStore.Video.Media.DATA;
            String[] da2 = {data};
            cursor = cr.query(contentUri, da2, null, null, null);
        }
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(data);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        } else {
            return null;
        }
    }


    public static String getLatestVideo(Context context) {
        ContentResolver cr = context.getContentResolver();
        String data = MediaStore.Video.Media.DATA;
        String date_taken = MediaStore.Video.Media.DATE_TAKEN;
        Uri video_store = Uri.parse("content://media/external/video/media");
        String[] da = {data, date_taken};
        //Log.i("App", "looking for latest video:"+ data );
        Cursor cursor = cr.query(video_store, da, null, null, date_taken);
        if (cursor != null) {
            cursor.moveToLast();
            //Log.i("App", "Has columns:" + cursor.getColumnNames().toString());
            //Log.i("App", "DATA (0):" + cursor.getString(0));
            //Log.i("App", "DATE_TAKEN (1):" + cursor.getString(1));
            Date now = new Date();
            while (now.getTime() < cursor.getLong(1)) {
                // ignore videos in storage that have odd timestamps: timestamps in future
                Log.i("LocalRawVideos", "Skipped to previous video. now:" + now.getTime() + ", video: " + cursor.getLong(1));
                cursor.moveToPrevious();
            }
            String path = cursor.getString(0);
            cursor.close();
            return path;
        } else {
            return null;
        }
    }

}
