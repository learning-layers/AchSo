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

package fi.aalto.legroup.achso.state;

import android.location.Location;

import java.util.Date;

public class AppState {
    private static AppState mInstance = null;

    public Location last_location;
    public String last_qr_code;
    public Date last_qr_code_timestamp;

    protected AppState() {
        last_location = null;
        last_qr_code = null;
        last_qr_code_timestamp = null;
    }

    public static synchronized AppState get() {
        if (null == mInstance) {
            mInstance = new AppState();
        }
        return mInstance;
    }

}
