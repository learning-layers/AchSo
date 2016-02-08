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
import android.net.Uri;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;

/**
 * TODO: Why not read these directly?
 */
public final class OIDCConfig {

    private static String clientId = null;
    private static String clientSecret = null;
    private static int tokenVersion = 0;

    private OIDCConfig() {
        // Static access only
    }

    public static boolean isReady() {
        return clientId != null && clientSecret != null;
    }

    private static class OIDCTokenResponse implements JsonSerializable {
        public String client_id;
        public String client_secret;
    }

    public static void setTokens(String newClientId, String newClientSecret) {
        tokenVersion++;
        clientId = newClientId;
        clientSecret = newClientSecret;
    }

    private static Request createRetrieveOIDCTokensRequest(Context context) {
        Uri achrailsUrl = App.getAchRailsUrl(context);
        Uri endpointUrl = achrailsUrl.buildUpon().appendPath("oidc_tokens").build();

        Request request = new Request.Builder()
                .url(endpointUrl.toString())
                .get()
                .build();

        return request;
    }

    public interface TokenCallback {
        void tokensRetrieved();
    }

    public static void retrieveOIDCTokens(final Context context, final TokenCallback callback) {

        tokenVersion++;
        final int expectVersion = tokenVersion;

        Request request = createRetrieveOIDCTokensRequest(context);
        App.httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                // TODO?
            }

            @Override
            public void onResponse(Response response) throws IOException {

                if (tokenVersion != expectVersion)
                    return;

                OIDCTokenResponse tokens = App.jsonSerializer.read(OIDCTokenResponse.class, response.body().byteStream());
                setTokens(tokens.client_id, tokens.client_secret);
                callback.tokensRetrieved();
            }
        });
    }

    public static void retrieveOIDCTokensBlocking(final Context context) throws IOException {
        if (isReady())
            return;

        Request request = createRetrieveOIDCTokensRequest(context);
        Response response = App.httpClient.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Failed to get tokens");
        OIDCTokenResponse tokens = App.jsonSerializer.read(OIDCTokenResponse.class, response.body().byteStream());
        setTokens(tokens.client_id, tokens.client_secret);
    }

    public static String getClientId(Context context) throws OIDCNotReadyException {
        if (clientId == null) throw new OIDCNotReadyException();
        return clientId;
    }

    public static String getClientSecret(Context context) throws OIDCNotReadyException {
        if (clientSecret == null) throw new OIDCNotReadyException();
        return clientSecret;
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
