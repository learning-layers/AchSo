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

package fi.aalto.legroup.achso.authentication;

import android.content.Context;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;

/**
 * TODO: Why not read these directly?
 */
public class OIDCConfig {

    public static String getClientId(Context context) {
        return context.getString(R.string.oidcClientId);
    }

    public static String getClientSecret(Context context) {
        return context.getString(R.string.oidcClientSecret);
    }

    public static String getAuthorizationServerUrl(Context context) {
        String path = context.getString(R.string.oidcAuthorizationServerUrl);
        return App.getLayersServiceUrl(path).toString();
    }

    public static String getTokenServerUrl(Context context) {
        String path = context.getString(R.string.oidcTokenServerUrl);
        return App.getLayersServiceUrl(path).toString();
    }

    public static String getUserInfoUrl(Context context) {
        String path = context.getString(R.string.oidcUserInfoUrl);
        return App.getLayersServiceUrl(path).toString();
    }

    public static String getRedirectUrl(Context context) {
        return context.getString(R.string.oidcRedirectUrl);
    }

    public static String[] getScopes(Context context) {
        return context.getResources().getStringArray(R.array.oidcScopes);
    }

}
