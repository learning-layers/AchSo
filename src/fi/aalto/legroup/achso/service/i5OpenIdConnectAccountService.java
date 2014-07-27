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
package fi.aalto.legroup.achso.service;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

    /*
     * Step 1

     First we need to compose a URL that contains the following:

     Redirect URI
     Response Token
     Scope
     ClientID
     State
     Nonce
     Here ClientID is the ID assigned to your application, and Response type is the application code.

     Attach this query string to the openID connect service endpoint: "cloud15.dbis.rwth-aachen.de:9085/openid-connect-server-webapp/authorize" with a '?' in between.

     When the user will visit this link he will be prompted with the login, and will be redirected to the Redirect URI. After the redirect the page will have a code in the URL response, which is a one time usable code, and now you can proceed to step 2 and utilize this code.

     Step 2

     The code will be used to retrieve an Access Token which is should be stored on the client side (for the RESTFul service) so that user does not have to login again.

     Cloud Video Transcoder composes a HttpRequest for getting the Access Token.

     Compose an Access Token request:
     accessTokenRequest = new TokenRequest( tokenEndpointURL, clientAuth, new AuthorizationCodeGrant(code, new URI(), clientID));

     clientAuth is declared as follows: ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

     and tokenEndpointURL: http://10.255.255.17:9085/openid-connect-server-webapp/token

     For the clientID in the query, there is a simple workout that needs to be done: String modifiedQuery = httpRequest.getQuery().split("&client_id")[0];
     httpRequest.setQuery(modifiedQuery);

     Now our request is ready and after sending it, the Access Token should be received. This should be sent to the user and be saved (e.g. in the local browser storage), for later use (with further API calls).

     Step 3

     As a next step, information about the user who logged in for the service should be retrieved.

     Its a simple HttpRequest which is composed as follows: UserInfoRequest userInfoRequest = new UserInfoRequest(userinfoEndpointURL, accessToken);

     Here userinfoEndpointURL is: http://10.255.255.17:9085/openid-connect-server-webapp/userinfo

     and accessToken is the one that was received in step 2.

     In response of this request we will get back the user information like their username, name , email, etc.

     Note

     We simply store the Access Token in the user's browser and send it to the service whenever user tries to access the service. The service tries to get the user information with that Access Token and if it successfully gets the user information with that accessToken then the user is allowed to access the service.
     If it receives any error with the request then it parses the error. If the user is not authorized, or if the Access Token has been expired, then the user is redirected to the login page.

     String auth_example = "https://server.example" +
     ".com/authorize?response_type=code&client_id=s6BhdRkqt3" +
     "&redirect_uri=https%3A%2F%2Fclient.example" +
     ".org%2Fcb&scope=openid%20profile&state=af0ifjsldkj";

     String base_url = "cloud15.dbis.rwth-aachen.de:9085/openid-connect-server-webapp/authorize";
     String response_type = "code";
     String client_id = "";
     String redirect_uri = "";
     String scope = "openid profile";
     String state = UUID.randomUUID().toString().substring(0,8);

     */


/**
 * This is the service to launch
 * Created by purma on 23.6.2014.
 *
 */
public class i5OpenIdConnectAccountService extends Service {

    public static final String ACCOUNT_NAME = "sync";
    public static final String ACHSO_ACCOUNT_TYPE = "fi.aalto.legroup.achso.i5account";
    public static final String ACHSO_AUTH_TOKEN_TYPE = "i5cloud";
    private static final String TAG = "i5CloudAccountService";
    private i5OpenIdConnectAuthenticator mAuthenticator;
    private Context mContext;

    /**
     * Obtain a handle to the {@link android.accounts.Account} used for sync in this application.
     *
     * @return Handle to application's account (not guaranteed to resolve unless CreateSyncAccount()
     * has been called)
     */
    public static Account GetAccount() {
        // Note: Normally the account name is set to the user's identity (username or email
        // address). However, since we aren't actually using any user accounts, it makes more sense
        // to use a generic string in this case.
        //
        // This string should *not* be localized. If the user switches locale, we would not be
        // able to locate the old account, and may erroneously register multiple accounts.
        final String accountName = ACCOUNT_NAME;
        return new Account(accountName, ACHSO_ACCOUNT_TYPE);
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "AccountService created");
        mAuthenticator = new i5OpenIdConnectAuthenticator(this);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }


    // Authenticator implementation is based on
    // https://udinic.wordpress.com/2013/04/24/write-your-own-android-authenticator/

}



