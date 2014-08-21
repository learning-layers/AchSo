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

package fi.aalto.legroup.achso.state;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.ActionbarActivity;
import fi.aalto.legroup.achso.activity.ApprovalActivity;
import fi.aalto.legroup.achso.util.App;

import static fi.aalto.legroup.achso.service.i5OpenIdConnectAccountService.ACHSO_ACCOUNT_TYPE;
import static fi.aalto.legroup.achso.service.i5OpenIdConnectAccountService.ACHSO_AUTH_TOKEN_TYPE;
import static fi.aalto.legroup.achso.util.App.appendLog;


// Cannot use nimbus oauth2 -libraries in Android, gives
// java.lang.NoSuchMethodError: org.apache.commons.codec.binary.Base64.encodeBase64String
// as libraries call apache.commons.codec , expecting it to be version that has encodeBase64String,
// but android's library is a version that doesn't have it. see:
// http://stackoverflow.com/questions/2047706/apache-commons-codec-with-android-could-not-find-method

// nimbus oauth2 -libraries are stupid anyways. Hiding pretty far what they are doing, it is clearer
// to write out the http gets and posts .

/**
 * Created by purma on 1.7.2014.
 */
public class i5OpenIdConnectLoginState implements LoginState {
    private static final String TAG = "i5OpenIdConnectLoginState";
    private final Context ctx;
    private String mAuthToken;
    private String mUser;
    private String mFullname;
    private String mEmail;
    private int mIn;
    private Account mAccount;
    private static DefaultHttpClient http = new DefaultHttpClient();
    public static final int OK = 1;
    public static final int APPROVAL_NEEDED = 2;
    public static final int AUTHENTICATION_FAILED = 3;
    private static final int ERROR = 4;
    private boolean disable_autologin_for_session;
    private boolean mUserdataUpdated;
    private String mIdToken;
    private String mScope;
    private String mTempState;
    private static final String OIDCAPI = "http://cloud15.dbis.rwth-aachen.de:9085/oidc/";

    public i5OpenIdConnectLoginState(Context ctx) {
        this.ctx = ctx;
        this.mIn = LOGGED_OUT;
        this.mUserdataUpdated = false;
    }

    /** Sign in to Aachen's Open Id server. This must be run in a non-main thread!

     * @param userName - String
     * @param userPass - String
     * @return Bundle (success*, session_id, error)
     */
    public static Bundle userSignIn(String userName, String userPass) {
        // Here implement server calls
        Bundle result = new Bundle();
        result.putBoolean("success", false);
        String login_form_action = OIDCAPI + "j_spring_security_check";
        String login_success_suffix = "oidc/";
        String login_fail_suffix = "login?error=failure";
        String domain = "cloud15.dbis.rwth-aachen.de";
        HttpResponse response = null;
        Cookie cookie = null;
        HttpParams params = new BasicHttpParams();
        HttpClientParams.setRedirecting(params, false);
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("j_username", userName));
        pairs.add(new BasicNameValuePair("j_password", userPass));
        Log.i(TAG, "Logging in with " + userName + " : " + userPass);
        pairs.add(new BasicNameValuePair("submit", "1"));
        HttpPost post = new HttpPost(login_form_action);
        post.setParams(params);
        post.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            post.setEntity(new UrlEncodedFormEntity(pairs));
        } catch (UnsupportedEncodingException e) {
            result.putBoolean("success", false);
            result.putString("error", "Url encoding error when filling login form");
            e.printStackTrace();
        }
        try {
            response = http.execute(post);
            Log.i(TAG, "Response (sent data) status code + reason : " + response.getStatusLine()
                    .getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
            //Header[] headers = response.getAllHeaders();
            //for (Header h: headers) {
            //    Log.i(TAG, "Header (response): " + h.getName() + " value: " + h.getValue());
            //}
            if (response.getStatusLine().getStatusCode() == 404) {
                Log.i(TAG, "Authentication server doesn't respond.");
                result.putString("error", "Authentication server returns 404");
            } else {
                for (Cookie c : http.getCookieStore().getCookies()) {
                    //Log.i(TAG, "Cookie (after): " + c.getDomain() + " value: " + c.getValue());
                    if (c.getDomain().equals(domain)) {
                        cookie = c;
                        Log.i(TAG, "Found cookie:" + c.toString());
                    }
                }
                String location = response.getFirstHeader("Location").getValue();
                Log.i(TAG, "Pretty sure that location is: " + location);
                if (location.endsWith(login_fail_suffix)) {
                    Log.i(TAG, "Login failed");
                    result.putString("failed", "Login failed");
                } else if (location.endsWith(login_success_suffix) && cookie != null) {
                    Log.i(TAG, "Success -- keep the cookie");
                    result.putString("session_id", cookie.getValue());
                    result.putBoolean("success", true);
                } else {
                    Log.e(TAG, "Don't know what happened with login: " + location);
                    result.putString("error", "Unknown error");
                }
            }
            response.getEntity().consumeContent();
        } catch (IOException e) {
            result.putString("error", "IO exception");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Sends registration to Aachen's server (fakes the filling of the registration form). This must
     * be run in a non-main thread!
     * @param fullname - String, optional for server, mandatory for us?
     * @param email - String, optional for server, mandatory for us?
     * @param username - String, mandatory
     * @param pass - String, optional for server, mandatory for us.
     * @return session_id - String
     */
    public static Bundle userRegister(String fullname, String email, String username, String pass) {
        Bundle result = new Bundle();
        result.putBoolean("success", false);
        String register_form_action = "http://cloud15.dbis.rwth-aachen.de:9086/register2.php";
        String registration_success_substring = "REGISTRATION WAS SUCCESSFUL";
        String registration_fail_substring = "Duplicate entry ";
        boolean success = false;
        HttpResponse response;
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("fullname", fullname));
        pairs.add(new BasicNameValuePair("uname", username));
        pairs.add(new BasicNameValuePair("email", email));
        pairs.add(new BasicNameValuePair("pass", pass));
        pairs.add(new BasicNameValuePair("submit", "submit"));
        HttpPost post = new HttpPost(register_form_action);
        post.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            post.setEntity(new UrlEncodedFormEntity(pairs));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            response = http.execute(post);
            Log.i(TAG, "Response (sent data) status code + reason : " + response.getStatusLine()
                    .getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
            InputStream content = response.getEntity().getContent();

            BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
            String html = "";
            String s;
            while ((s = buffer.readLine()) != null) {
                html += s;
            }
            if (html.contains(registration_success_substring)) {
                success = true;
                result.putBoolean("success", true);
            } else if (html.contains(registration_fail_substring)) {
                success = false;
                result.putString("fail", "Registration failed: Username exists already.");
            }
            response.getEntity().consumeContent();
            Log.i(TAG, html);
            Log.i(TAG, "Registration success: " + success);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (success) {
            return userSignIn(username, pass);
        }
        return result;
    }


    /**
     * First asks ClViTra to provide the authorization link, then follows it. The purpose is to
     * receive the temporary short code required for creating the access token. There may be
     * complications.
     * Networking! Must be run in separate thread, not in main thread.
     * @return Bundle:
     *  "status":OK
     *      "url": redirect_uri + code as url parameter
     *  "status":APPROVAL_NEEDED
     *      "url": url of approval page
     *      "cookie_value": session_id cookie if such is required for getting to approval page
     *      "cookie_url": domain for session_id cookie.
     *  "status":AUTHENTICATION_FAILED
     *      "status_code": response's status code, e.g. 304, 404
     *      "html": response page dumped as string
     *  "status":ERROR
     *      "error": error message
     *  "state": state -string sent to server. It will be compared to result.
     */
    private static Bundle getAuthorizationFromClViTra() {
        Log.i(TAG, "getAuthorizationFromClViTra");
        String url = "http://137.226.58.27:9080/ClViTra_2.0/rest/openIDauth";
        Bundle result = new Bundle();
        HttpResponse response = null;
        HttpGet get = new HttpGet(url);
        try {
            response = http.execute(get);
        } catch (IOException e) {
            e.printStackTrace();
            result.putInt("status", ERROR);
            result.putString("error", e.getMessage());
        }
        if (response == null) {
            result.putInt("status", ERROR);
            result.putString("error", "null response");
            return result;
        }
        InputStream content = null;
        String auth_url = "";
        try {
            content = response.getEntity().getContent();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
            String s;
            while ((s = buffer.readLine()) != null) {
                auth_url += s;
            }
            response.getEntity().consumeContent();
        } catch (IOException e) {
            e.printStackTrace();
            result.putInt("status", ERROR);
            result.putString("error", e.getMessage());
        }
        // Find the state -parameter so we can verify it later.
        Uri auth_uri = Uri.parse(auth_url);
        Log.i(TAG, "Got auth uri: "+ auth_url);
        result.putString("state", auth_uri.getQueryParameter("state"));

        get = new HttpGet(auth_url);

        HttpParams params = new BasicHttpParams();
        HttpClientParams.setRedirecting(params, false);
        get.setParams(params);


        try {
            response = http.execute(get);
            Log.i(TAG, "Response (sent data) status code + reason : " + response.getStatusLine()
                    .getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
            Header location = response.getFirstHeader("Location");
            int status_code = response.getStatusLine().getStatusCode();
            if (status_code == 302) {
                // this should be redirecting to redirect_uri, with an access code in url parameters
                // Main thread will launch the next step, resumeAuthentication with url
                result.putInt("status", OK);
                result.putString("url", location.getValue());
            } else if (status_code == 200) {
                // this is Approve Access -page.
                Log.i(TAG, "Needs to ask approval before continuing");
                String domain =  Uri.parse(auth_url).getHost();
                Cookie cookie = null;
                for (Cookie c: http.getCookieStore().getCookies()) {
                    if (c.getDomain().equals(domain)) {
                        cookie = c;
                    }
                }
                // Main thread will launch ApprovalActivity, we need to provide arguments for it.
                result.putInt("status", APPROVAL_NEEDED);
                result.putString("url", auth_url);
                if (cookie != null) {
                    result.putString("cookie_value", cookie.getValue());
                    result.putString("cookie_url", cookie.getDomain());
                }
            } else {
                // Failing, provide something useful for debugging
                Log.i(TAG, "Strange response: " + status_code);
                result.putInt("status", AUTHENTICATION_FAILED);
                result.putInt("status_code", status_code);
                content = response.getEntity().getContent();
                BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                String html = "";
                String s;
                while ((s = buffer.readLine()) != null) {
                    html += s;
                }
                Log.i(TAG, "Received html: " + html);
                result.putString("html", html);
            }
            response.getEntity().consumeContent();
        } catch (IOException e) {
            e.printStackTrace();
            result.putInt("status", ERROR);
            result.putString("error", e.getMessage());
        }
        return result;
    }

    /**
     * Gets the temporary short code required for creating the access token. There may be
     * complications
     * Networking! Must be run in separate thread, not in main thread.
     * @return Bundle:
     *  "status":OK
     *      "url": redirect_uri + code as url parameter
     *  "status":APPROVAL_NEEDED
     *      "url": url of approval page
     *      "cookie_value": session_id cookie if such is required for getting to approval page
     *      "cookie_url": domain for session_id cookie.
     *  "status":AUTHENTICATION_FAILED
     *      "status_code": response's status code, e.g. 304, 404
     *      "html": response page dumped as string
     *  "status":ERROR
     *      "error": error message
     *  "state": state -string sent to server. It will be compared to result.
     */
    private static Bundle getAuthorizationFromi5OIDC() {
        Log.i(TAG, "getAuthorizationFromi5OIDC");
        String url = OIDCAPI + "authorize";
        // following addresses are kind of pointless, but we need some access-restricted service
        // url to start with.
        String redirect_uri = "http://137.226.58.27:9080/ClViTra_2.0/FileUpload.html";
        String client_id = "clvitra";
        Bundle result = new Bundle();
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        String state = UUID.randomUUID().toString().substring(0,8);
        String nonce = UUID.randomUUID().toString().substring(0, 8);
        pairs.add(new BasicNameValuePair("response_type", "code"));
        pairs.add(new BasicNameValuePair("client_id", client_id));
        pairs.add(new BasicNameValuePair("redirect_uri", redirect_uri));
        pairs.add(new BasicNameValuePair("scope", "openid+email+profile"));
        pairs.add(new BasicNameValuePair("state", state));
        pairs.add(new BasicNameValuePair("nonce", nonce));

        String paramString = URLEncodedUtils.format(pairs, "utf-8");
        if (!url.endsWith("?")) {
            url += "?";
        }
        url += paramString;
        Log.i(TAG, "Trying to send get to: " + url);

        HttpResponse response;
        HttpGet get = new HttpGet(url);
        HttpParams params = new BasicHttpParams();
        HttpClientParams.setRedirecting(params, false);
        get.setParams(params);
        get.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            response = http.execute(get);
            Log.i(TAG, "Response (sent data) status code + reason : " + response.getStatusLine()
                    .getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
            Header location = response.getFirstHeader("Location");
            int status_code = response.getStatusLine().getStatusCode();
            if (status_code == 302) {
                // this should be redirecting to redirect_uri, with an access code in url parameters
                // Main thread will launch the next step, resumeAuthentication with url
                result.putInt("status", OK);
                result.putString("url", location.getValue());
            } else if (status_code == 200) {
                // this is Approve Access -page.
                Log.i(TAG, "Needs to ask approval before continuing");
                String domain =  Uri.parse(url).getHost();
                Cookie cookie = null;
                for (Cookie c: http.getCookieStore().getCookies()) {
                    if (c.getDomain().equals(domain)) {
                        cookie = c;
                    }
                }
                // Main thread will launch ApprovalActivity, we need to provide arguments for it.
                result.putInt("status", APPROVAL_NEEDED);
                result.putString("url", url);
                if (cookie != null) {
                    result.putString("cookie_value", cookie.getValue());
                    result.putString("cookie_url", cookie.getDomain());
                }
            } else {
                // Failing, provide something useful for debugging
                Log.i(TAG, "Strange response: " + status_code);
                result.putInt("status", AUTHENTICATION_FAILED);
                result.putInt("status_code", status_code);
                InputStream content = response.getEntity().getContent();
                BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                String html = "";
                String s;
                while ((s = buffer.readLine()) != null) {
                    html += s;
                }
                Log.i(TAG, "Received html: " + html);
                result.putString("html", html);
            }
            response.getEntity().consumeContent();
        } catch (IOException e) {
            e.printStackTrace();
            result.putInt("status", ERROR);
            result.putString("error", e.getMessage());
        }
        result.putString("state", state);
        return result;
    }

    /**
     * Fetches the actual AccessToken using ClViTra-services interface. Cannot be run
     * in a main thread!
     * @param code
     */
    private static String getAccessTokenFromClViTra(String code) {
        Log.i(TAG, "getAccessTokenFromClViTra");
        String url = "http://137.226.58.27:9080/ClViTra_2.0/rest/getAccessToken";
        HttpGet get = new HttpGet(url);
        get.setHeader("Code", code);
        HttpResponse response;
        try {
            response = http.execute(get);
            InputStream content = response.getEntity().getContent();

            BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
            String auth_token = "";
            String s;
            while ((s = buffer.readLine()) != null) {
                auth_token += s;
            }
            response.getEntity().consumeContent();
            if (response.getStatusLine().getStatusCode() == 200) {
                // this is what we want
                return auth_token;
            } else {
                Log.i(TAG, "Response (sent data) status code + reason : " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                Log.i(TAG, auth_token);
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Fetches the AccessToken from OAuth2 server, claims to be clvitra in order to do that.
     * Cannot be run in a main thread!
     * @param code
     */
    private static String getAccessTokenFromi5OIDC(String code) {
        Log.i(TAG, "getAccessTokenFromi5OIDC");
        String tokenEndpoint = OIDCAPI + "token";
        String target = "http://137.226.58.27:9080/ClViTra_2.0/FileUpload.html";
        String client_id = "clvitra";
        String auth = "Basic " + Base64.encodeToString((client_id + ":" + client_id).getBytes(), Base64.NO_WRAP);
        HttpPost post = new HttpPost(tokenEndpoint);
        post.setHeader("Content-type", "application/x-www-form-urlencoded");
        post.setHeader("Authorization", auth);
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
        pairs.add(new BasicNameValuePair("code", code));
        pairs.add(new BasicNameValuePair("redirect_uri", target));
        try {
            post.setEntity(new UrlEncodedFormEntity(pairs));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpResponse response;
        try {
            response = http.execute(post);
            InputStream content = response.getEntity().getContent();

            BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
            String json_string = "";
            String s;
            while ((s = buffer.readLine()) != null) {
                json_string += s;
            }
            response.getEntity().consumeContent();
            if (response.getStatusLine().getStatusCode() == 200) {
                // this is what we want
                return json_string;
            } else {
                Log.i(TAG, "Response (sent data) status code + reason : " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                Log.i(TAG, json_string);
                if (json_string == null || json_string.isEmpty()) return "";
                JSONObject jObject = null;
                try {
                    jObject = new JSONObject(json_string);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (jObject == null) return "";

                try {
                    Log.i(TAG, "access_token: " + jObject.getString("access_token"));
                    return jObject.getString("access_token");

                } catch (JSONException e) {
                    e.printStackTrace();
                    return "";
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     *
     * @return json_string with the user data, or "", to be handled in main thread.
     */
    public String getUserDataFromi5OIDC() {
        Log.i(TAG, "getUserDataFromi5OIDC");
        String json_string = "";
        String url = OIDCAPI + "userinfo";
        HttpResponse response;
        HttpGet get = new HttpGet(url);
        get.setHeader("Content-type", "application/x-www-form-urlencoded");
        get.setHeader("Authorization", "Bearer " + mAuthToken);
        try {
            response = http.execute(get);
            Log.i(TAG, "Response (sent data) status code + reason : " + response.getStatusLine()
                    .getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
            InputStream content = response.getEntity().getContent();

            BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
            String s;
            while ((s = buffer.readLine()) != null) {
                json_string += s;
            }
            Log.i(TAG, json_string);
            response.getEntity().consumeContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json_string;
    }


    private void setState(int state) {
        if (state == LOGGED_OUT) {
            mUser = null;
        } else if (state == LOGGED_IN && state != mIn) {
            disable_autologin_for_session = true;
        }
        Intent intent = new Intent();
        if (state == LOGGED_OUT) {
            intent.setAction(LOGIN_FAILED);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        } else if (state == LOGGED_IN) {
            intent.setAction(LOGIN_SUCCESS);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        }

        mIn = state;
        Log.i(TAG, "LoginState set to " + state + ". Should update the icon next. ");
    }

    @Override
    public String getPublicUrl() {
        return null;
    }

    @Override
    public boolean isIn() {
        return (mIn == LOGGED_IN);
    }

    @Override
    public boolean isOut() {
        return (mIn == LOGGED_OUT);
    }

    @Override
    public boolean isTrying() {
        return (mIn == TRYING_TO_LOG_IN);
    }

    @Override
    public String getUser() {
        return mUser;
    }

    @Override
    public int getState() {
        return mIn;
    }

    @Override
    public String getAuthToken() {
        return mAuthToken;
    }

    @Override
    public void logout() {
        setState(LOGGED_OUT);
        mAccount = null;
        mUser = null;
        mFullname = null;
        mEmail = null;
    }

    @Override
    public void launchLoginActivity(final Activity host_activity) {
        final AccountManager am = AccountManager.get(App.getContext());
        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                Log.i(TAG, "AccountManagerCallback running");
                boolean success = false;
                try {
                    Bundle result = bundleAccountManagerFuture.getResult();
                    if (result != null && result.keySet().contains("authtoken")) {
                        mAuthToken = result.getString("authtoken");
                        if (mAuthToken != null && !mAuthToken.isEmpty()) {
                            success = true;
                        }
                        mUser = result.getString("authAccount");
                        for (Account a: am.getAccountsByType(result.getString("accountType"))) {
                            if (mUser.equals(a.name)) {
                                mAccount = a;
                            }

                        }
                        Log.i(TAG, "Peeking to authToken: " + am.peekAuthToken(mAccount,
                                ACHSO_AUTH_TOKEN_TYPE));
                    }
                    // typical result bundle:
                    // accountType = fi.aalto.legroup.achso.i5account,
                    // authtoken = 03D96F19191AB286D7E1183A39016AE7,
                    // authAccount = jukka
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }
                if (success && mAccount != null) {
                    // We have an account with session_id, but it is probably out of date.
                    new VerifyLoginTask(host_activity).execute("");
                }
            }
        };
        Handler callback_thread = new Handler();
        Bundle addAccountOptions = new Bundle();
        Bundle getAuthTokenOptions = new Bundle();

        // this will start its own thread, but callback is run in main thread?
        am.getAuthTokenByFeatures(ACHSO_ACCOUNT_TYPE,
                ACHSO_AUTH_TOKEN_TYPE, null, host_activity,
                addAccountOptions, getAuthTokenOptions, callback, callback_thread );

    }

    @Override
    public void resumeAuthentication(String next_url) {
        String code = null;
        String state = null;
        Log.i(TAG, "Got url:" + next_url);
        Uri uri = Uri.parse(next_url);
        code = uri.getQueryParameter("code");
        state = uri.getQueryParameter("state");
        if (state == null || !state.equals(mTempState)) {
            Log.i(TAG, "Response state didn't match with requests state -parameter");
            logout();
            return;
        }
        Log.i(TAG, "Got code: " + code);
        Log.i(TAG, "Upload authorization success: " + (code != null));
        new AuthTokenTask().execute(code);
    }


    @Override
    public void autologinIfAllowed() {

    }

    @Override
    public void login(String user, String pass) {
        setState(LOGGED_OUT);
        appendLog(String.format("Doing login as %s", user));

        if (user != null && pass != null) {
            setState(TRYING_TO_LOG_IN);
            new LoginTask().execute(user, pass);
        }
    }

    /**
     * This is asynchronous task to receive a short key
     */
    private class VerifyLoginTask extends AsyncTask<String, Integer, Bundle> {
        Activity mActivity;
        public VerifyLoginTask(Activity myActivity) {
            mActivity = myActivity;
        }
        protected Bundle doInBackground(String... url) {
            Log.i(TAG, "Running login verification.");
            final AccountManager am = AccountManager.get(App.getContext());
            // Run Login process if necessary.
            if (mIn == LOGGED_OUT) {
                Bundle login_result = userSignIn(mUser, am.getPassword(mAccount));
                if (login_result.getBoolean("success")) {
                    String session_id = login_result.getString("session_id");
                    // We are now logged in.
                    am.setAuthToken(mAccount, ACHSO_AUTH_TOKEN_TYPE, session_id);
                } else {
                    // let's do something else.
                }
            }
            // But we don't have proper AccessToken yet.
            return getAuthorizationFromClViTra();
            //return getAuthorizationFromi5OIDC();
        }

        protected void onPostExecute(Bundle b) {
            Log.i(TAG, "onPostExecute after VerifyLoginTask");
            int short_code_status = b.getInt("status");
            Log.i(TAG, "status:" + short_code_status);
            mTempState = b.getString("state");
            switch (short_code_status) {
                case OK:
                    resumeAuthentication(b.getString("url"));
                    setState(LOGGED_IN);
                    break;
                case APPROVAL_NEEDED:
                    Log.i(TAG, "Launching approval check- intent with Uri " + b.getString("url"));
                    Intent intent = new Intent(mActivity, ApprovalActivity.class);
                    intent.putExtra("url", b.getString("url"));
                    intent.putExtra("cookie_value", b.getString("cookie_value"));
                    intent.putExtra("cookie_url", b.getString("cookie_url"));
                    mActivity.startActivityForResult(intent, ActionbarActivity.REQUEST_AUTHENTICATION_APPROVAL);
                    setState(LOGGED_IN);
                    break;
                case ERROR:
                    break;
                case AUTHENTICATION_FAILED:
                    break;
            }
        }
    }

    private class LoginTask extends AsyncTask<String, Void, Bundle> {

        @Override
        protected Bundle doInBackground(String... arg) {
            // this is run in its own thread: cannot access to UI,
            // deliver response and let onPostExecute handle it in main thread


            if (arg.length < 2) {
                return null;
            }
            Bundle login_result = userSignIn(arg[0], arg[1]);
            if (login_result.getBoolean("success")) {
                String session_id = login_result.getString("session_id");
                // We are now logged in.
                final AccountManager am = AccountManager.get(App.getContext());
                am.setAuthToken(mAccount, ACHSO_AUTH_TOKEN_TYPE, session_id);
            }
            return login_result;
        }
        protected void onPostExecute(Bundle login_result) {
            if (login_result.getBoolean("success")) {
                setState(LOGGED_IN);
            } else if (login_result.getString("fail") != null) {
                Toast.makeText(ctx, "Login failed: " + login_result.getString("fail"), Toast.LENGTH_LONG).show();
                setState(LOGGED_OUT);
            } else if (login_result.getString("error") != null) {
                Toast.makeText(ctx, "Login error: " + login_result.getString("error"),
                        Toast.LENGTH_LONG).show();
                setState(LOGGED_OUT);
            }
        }

    }

    private class AuthTokenTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... code) {
            // this is run in its own thread: cannot access to UI,
            // deliver response and let onPostExecute handle it in main thread
            //return getAccessTokenFromi5OIDC(code[0]);
            return getAccessTokenFromClViTra(code[0]);
        }
        protected void onPostExecute(String access_token) {
            mAuthToken = access_token;
            final AccountManager am = AccountManager.get(App.getContext());
            am.setAuthToken(mAccount, ACHSO_AUTH_TOKEN_TYPE, mAuthToken);
            new UserDataTask().execute();
        }

    }
    private class UserDataTask extends AsyncTask<Void, Void, String> {


        @Override
        protected String doInBackground(Void... voids) {
            // this is run in its own thread: cannot access to UI,
            // deliver response and let onPostExecute handle it in main thread
            return getUserDataFromi5OIDC();
        }

        protected void onPostExecute(String json_string) {
            if (json_string == null || json_string.isEmpty()) return;
            JSONObject jObject = null;
            try {
                jObject = new JSONObject(json_string);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jObject == null) return;

            try {
                Log.i(TAG, "name: " + jObject.getString("name"));
                Log.i(TAG, "preferred_username: " + jObject.getString("preferred_username"));
                Log.i(TAG, "email: " + jObject.getString("email"));
                Log.i(TAG, "email_verified: " + jObject.getBoolean("email_verified"));
                mEmail = jObject.getString("email");
                mUser = jObject.getString("preferred_username");
                mFullname = jObject.getString("name");
                final AccountManager am = AccountManager.get(App.getContext());
                String old_fullname = am.getUserData(mAccount, "fullname");
                // Here we change device's account settings based on settings we get from server.
                // It could often be otherwise around.
                if (old_fullname == null || !old_fullname.equals(mFullname)) {
                    am.setUserData(mAccount, "fullname", mFullname);
                }
                String old_email = am.getUserData(mAccount, "email");
                if (old_email == null || !old_email.equals(mEmail)) {
                    am.setUserData(mAccount, "email", mEmail);
                }
                if (mFullname!=null && !mFullname.isEmpty()) {
                    Toast.makeText(ctx, ctx.getString(R.string.Welcome)+ ", "+mFullname, Toast.LENGTH_LONG).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

    }



}
