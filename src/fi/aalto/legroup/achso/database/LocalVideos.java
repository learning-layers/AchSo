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

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LocalVideos {

    public static File getNewOutputFile() {
        // Files stored in this directory will not be removed when the program is uninstalled. This is the recommended location in Android SDK manuals.
        File storagedir = LocalVideos.getVideoStorage();
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
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AnnotatedVideos");
        } else return null;
    }
}
