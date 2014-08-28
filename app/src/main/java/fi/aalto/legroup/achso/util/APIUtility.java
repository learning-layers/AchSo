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

import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;

/**
 * An incomplete class that illustrates how to make API requests with the ID Token.
 *
 * @author Leo Nikkilä
 */
public class APIUtility {

    private static final String TAG = "APIUtility";

    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;

    /**
     * Makes a GET request and parses the received JSON string as a Map.
     */
    public static Map getJson(Context context, String url, String idToken)
            throws IOException {

        String jsonString = makeRequest(context, HttpRequest.METHOD_GET, url, idToken);
        Log.i(TAG, jsonString);
        return new Gson().fromJson(jsonString, Map.class);
    }

    /**
     * Makes an arbitrary HTTP request.
     *
     * If the request doesn't execute successfully on the first try, the tokens will be refreshed
     * and the request will be retried. If the second try fails, an exception will be raised.
     */
    public static String makeRequest(Context context, String method, String url, String idToken)
            throws IOException {

        return makeRequest(context, method, url, idToken, true);
    }

    private static String makeRequest(Context context, String method, String url, String idToken,
                                     boolean doRetry) throws IOException {

        AccountManager accountManager = AccountManager.get(context);

        HttpRequest request = new HttpRequest(url, method);
        request = OIDCUtils.prepareApiRequest(request, idToken);

        if (request.ok()) {
            return request.body();
        } else {
            int code = request.code();

            if (doRetry && (code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN)) {
                // We're being denied access on the first try, let's renew the token and retry
                accountManager.invalidateAuthToken(App.ACHSO_ACCOUNT_TYPE,
                        idToken);
                return makeRequest(context, method, url, idToken, false);
            } else {
                // An unrecoverable error or the renewed token didn't work either
                Log.i(TAG, "Request url: "+url +" , idToken: "+ idToken);
                throw new IOException(request.code() + " " + request.message());
            }
        }
    }

}
