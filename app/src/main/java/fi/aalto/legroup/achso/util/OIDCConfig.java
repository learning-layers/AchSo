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

package fi.aalto.legroup.achso.util;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OIDCConfig {

    private final static String TAG = "OIDCConfig";

    // These are the defaults and won't be accepted by any server. You should put real values to
    // assets/OIDCsettings.properties
    public String clientId = "foobar";
    public String clientSecret = "xyzzy";

    public String authorizationServerUrl = "https://www.example.com/oauth2/authorize";
    public String tokenServerUrl = "https://www.example.com/oauth2/token";
    public String userInfoUrl = "https://www.example.com/oauth2/userinfo";

    // This URL doesn't really have a use with native apps and basically just signifies the end
    // of the authorisation process. It doesn't have to be a real URL, but it does have to be the
    // same URL that is registered with your provider.
    public String redirectUrl = "app://achso.legroup.aalto.fi";

    // The `offline_access` scope enables us to request Refresh Tokens, so we don't have to ask the
    // user to authorise us again every time the tokens expire. Some providers might have an
    // `offline` scope instead. If you get an `invalid_scope` error when trying to authorise the
    // app, try changing it to `offline`.
    public String[] scopes = {"openid", "profile", "offline_access"};

    public OIDCConfig(String file_name) {
        Properties props=new Properties();
        AssetManager assetManager = App.getContext().getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(file_name + ".properties");
            props.load(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't find OIDC settings from assets/" + file_name +
                    ".properties");
            e.printStackTrace();
        }
        clientId = props.getProperty("clientId", clientId);
        clientSecret = props.getProperty("clientSecret", clientSecret);
        authorizationServerUrl = props.getProperty("authorizationServerUrl", authorizationServerUrl);
        tokenServerUrl = props.getProperty("tokenServerUrl", tokenServerUrl);
        userInfoUrl = props.getProperty("userInfoUrl", userInfoUrl);
        redirectUrl = props.getProperty("redirectUrl", redirectUrl);
        String scope_string = props.getProperty("scopes", "openid, profile, offline_access");
        scope_string = scope_string.replace(" ", "");
        scopes = scope_string.split(",");
        Log.i(TAG, clientId);
        Log.i(TAG, clientSecret);
        Log.i(TAG, authorizationServerUrl);
        Log.i(TAG, tokenServerUrl);
        Log.i(TAG, redirectUrl);
        Log.i(TAG, scopes.toString());
    }
}
