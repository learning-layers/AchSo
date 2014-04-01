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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import fi.aalto.legroup.achso.activity.ActionbarActivity;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.LasConnection;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class i5LoginState {
    public static final int LOGGED_OUT = 0;
    private int mIn = LOGGED_OUT;
    public static final int TRYING_TO_LOG_IN = 1;
    public static final int LOGGED_IN = 2;
    private final String loginUrl = "http://137.226.58.11:8081/i5Cloud/services/3/auth";
    private String mUser;
    private String mAuthToken;
    private String mPublicUrl;
    private String mExpires;
    private String mTenantId;
    private Context ctx;
    private Activity mHost;
    private boolean disable_autologin_for_session = false;

    public i5LoginState(Context ctx) {
        this.ctx = ctx;
    }



    // auth token and swift-url are the keys that we need for api calls, these will be asked often
    public String getAuthToken() {
        return mAuthToken;
    }

    public String getPublicUrl() {
        return mPublicUrl;
    }

    // it is safer to ask first if we are logged in at all

    public boolean isIn() {
        return (mIn == LOGGED_IN);
    }
    public boolean isOut() {
        return (mIn == LOGGED_OUT);
    }
    public boolean isTrying() {
        return (mIn == TRYING_TO_LOG_IN);
    }

    // Internal stuff, mostly:

    public int getLoginStatus() {
        return mIn;
    }


    private void asynchronousLogin(String user, String pass) {
        setState(LOGGED_OUT);
        mUser = user;
        if (user.isEmpty() || pass.isEmpty()) {
            return;
        } else {
            AsyncTask<String, Void, String> task = new LoginTask();
            setState(TRYING_TO_LOG_IN);
            task.execute(user, pass);
            // LoginTask will set login state variables if it succeeds.
        }
    }

    public void setHostActivity(Activity host) {
        mHost = host;
    }

    public void autologinIfAllowed() {
        if (!disable_autologin_for_session) {
            if (mIn == LOGGED_OUT && App.hasConnection()) {
                SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
                boolean allowed = prefs.getBoolean("autologin", false);
                String login = prefs.getString("login", "");
                String pwd = prefs.getString("pwd", "");
                appendLog(String.format("Doing autologin as %s", mUser));

                if (allowed && !login.isEmpty() && !pwd.isEmpty()) {
                    asynchronousLogin(login, pwd);
                    Log.i("LoginState", "autologin launched async login.");
                }

            }
        }
    }

    public boolean login(String user, String pass) {
        setState(LOGGED_OUT);
        appendLog(String.format("Doing manual login as %s", user));

        if (user != null && pass != null) {
            setState(TRYING_TO_LOG_IN);
            AsyncTask<String, Void, String> task_result = new LoginTask().execute(user, pass);
            // LoginTask will set login state variables if it succeeds.
            try {
                if (task_result.get() != null) {
                    SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
                    if (prefs.getBoolean("autologin", false)) {
                        Editor edit = prefs.edit();
                        edit.putString("login", user);
                        edit.putString("pwd", pass);
                        edit.apply();
                    }
                    return true;
                }
            } catch (InterruptedException e) {
                Log.e("LoginState", "Login failed, catched InterruptedException");
                e.printStackTrace();
                setState(LOGGED_OUT);
            } catch (ExecutionException e) {
                Log.e("LoginState", "Login failed, catched ExecutionException");
                e.printStackTrace();
                setState(LOGGED_OUT);
            }
        }
        return false;
    }

    public String getUser() {
        return mUser;
    }

    public int getState() {
        return mIn;
    }

    private void setState(int state) {
        if (state == LOGGED_OUT) {
            mUser = null;
        } else if (state == LOGGED_IN && state != mIn) {
            Toast.makeText(ctx, "Logged in as " + mUser, Toast.LENGTH_SHORT).show();
            disable_autologin_for_session = true;
        }
        mIn = state;
        Log.i("LoginState", "state set to " + state + ". Should update the icon next. ");
        ((ActionbarActivity) mHost).updateLoginMenuItem();
    }


    public void logout() {
        mUser = null;
        setState(LOGGED_OUT);
    }

    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... arg) {
            if (arg.length < 2) {
                return null;
            }
            HttpClient httpclient = new DefaultHttpClient();
            Log.i("LoginTask", "Starting background logging in");
            //creating HTTP REST Call & authentication information
            HttpPost post = new HttpPost(loginUrl);
            HashMap<String, String> login_info = new HashMap<String, String>();
            login_info.put("username", arg[0]);
            login_info.put("password", arg[1]);
            StringEntity se;
            try {
                se = new StringEntity(new JSONObject(login_info).toString(), "UTF-8");
                post.setEntity(se);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            post.setHeader("Content-type", "application/json");
            try {
                HttpResponse response = httpclient.execute(post);
                int status = response.getStatusLine().getStatusCode();

                if (status == 200) {
                    HttpEntity e = response.getEntity();
                    String data = EntityUtils.toString(e);
                    Log.i("LoginTask", "Received logging in response: " + data);
                    JSONObject json_data = new JSONObject(data);
                    mPublicUrl = json_data.getString("swift-url");
                    mAuthToken = json_data.getString("X-Auth-Token");
                    mExpires = json_data.getString("expires");
                    mTenantId = json_data.getString("tenant-id");
                    mUser = arg[0];
                    setState(LOGGED_IN);
                    return data;
                } else {
                    Log.i("LoginTask", "Received status code " + status + " in response: " + response.getStatusLine().getReasonPhrase());
                    //Toast.makeText(ctx, "Received status code " + status + " in response: " + response.getStatusLine().getReasonPhrase(), Toast.LENGTH_LONG).show();
                    setState(LOGGED_OUT);
                    return null;
                }
            } catch (IOException e) {
                setState(LOGGED_OUT);
                e.printStackTrace();
            } catch (JSONException e) {
                setState(LOGGED_OUT);
                e.printStackTrace();
            }
            return null;
        }
    }
}
