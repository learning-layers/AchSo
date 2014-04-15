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
import android.content.Intent;
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

import fi.aalto.legroup.achso.activity.ActionbarActivity;
import fi.aalto.legroup.achso.util.App;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class i5LoginState {
    public static final int LOGGED_OUT = 0;
    private int mIn = LOGGED_OUT;
    public static final int TRYING_TO_LOG_IN = 1;
    public static final int LOGGED_IN = 2;
    public static String LOGIN_SUCCESS = "fi.aalto.legroup.achso.login_success";
    public static String LOGIN_FAILED = "fi.aalto.legroup.achso.login_failed";
    private final String loginUrl = "http://137.226.58.11:8081/i5Cloud/services/3/auth";
    private String mUser;
    private String mAuthToken;
    private String mPublicUrl;
    private String mExpires;
    private String mTenantId;
    private Context ctx;
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

    // it is easier to ask first if we are logged in at all

    public boolean isIn() {
        return (mIn == LOGGED_IN);
    }

    public boolean isOut() {
        return (mIn == LOGGED_OUT);
    }

    public boolean isTrying() {
        return (mIn == TRYING_TO_LOG_IN);
    }

    public String getUser() {
        return mUser;
    }

    public int getState() {
        return mIn;
    }

    public void logout() {
        mUser = null;
        setState(LOGGED_OUT);
    }

    // Internal stuff, mostly:

    public void autologinIfAllowed() {
        if (!disable_autologin_for_session) {
            if (mIn == LOGGED_OUT && App.hasConnection()) {
                SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
                boolean allowed = prefs.getBoolean("autologin", false);
                String user = prefs.getString("login", "");
                String pass = prefs.getString("pwd", "");
                appendLog(String.format("Doing autologin as %s", mUser));

                if (allowed && !user.isEmpty() && !pass.isEmpty()) {
                    new LoginTask() {
                        // this is run in main thread: it has access to UI
                        @Override
                        public void onPostExecute(String[] result_array) {
                            handle_login_result(result_array, true);
                        }
                    }.execute(user, pass);
                    Log.i("LoginState", "autologin launched async login.");
                }

            }
        }
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
            ctx.sendBroadcast(intent);
        } else if (state == LOGGED_IN) {
            intent.setAction(LOGIN_SUCCESS);
            ctx.sendBroadcast(intent);
        }

        mIn = state;
        Log.i("LoginState", "state set to " + state + ". Should update the icon next. ");
    }

    private boolean handle_login_result(String[] result_array, boolean silent) {
        // result_array: { result, status, user, password }
        if (result_array[1].equals("200")) {
            try {
                JSONObject json_data = new JSONObject(result_array[0]);
                mPublicUrl = json_data.getString("swift-url");
                mAuthToken = json_data.getString("X-Auth-Token");
                mExpires = json_data.getString("expires");
                mTenantId = json_data.getString("tenant-id");
                setState(LOGGED_IN);
                if (!silent) {
                    Toast.makeText(ctx, "Logged in as " + mUser, Toast.LENGTH_SHORT).show();
                    SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
                    if (prefs.getBoolean("autologin", false)) {
                        Editor edit = prefs.edit();
                        edit.putString("login", result_array[2]);
                        edit.putString("pwd", result_array[3]);
                        edit.apply();
                    }
                }
                return true;

            } catch (JSONException e) {
                if (!silent) {
                    Toast.makeText(ctx, "Received broken response from server.", Toast.LENGTH_LONG).show();
                }
                setState(LOGGED_OUT);
                e.printStackTrace();
                return false;
            }
        }
        if (!silent) {
            Toast.makeText(ctx, "Received status code " + result_array[1] + " in response: " + result_array[0], Toast.LENGTH_LONG).show();
            /**
             new AlertDialog.Builder(ctx)
             .setTitle(ctx.getResources().getString(R.string.login))
             .setMessage(ctx.getResources().getString(R.string.login_nag_text))
             .setPositiveButton(R.string.ok, null)
             .create().show();
             */
        }
        setState(LOGGED_OUT);
        return false;
    }

    public void login(String user, String pass) {
        setState(LOGGED_OUT);
        appendLog(String.format("Doing login as %s", user));

        if (user != null && pass != null) {
            setState(TRYING_TO_LOG_IN);
            new LoginTask() {
                // this is run in main thread: it has access to UI
                @Override
                public void onPostExecute(String[] result_array) {
                    handle_login_result(result_array, false);
                }
            }.execute(user, pass);
        }
    }


    private class LoginTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... arg) {
            // this is run in its own thread: cannot access to UI,
            // deliver response and let onPostExecute handle it in main thread

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
            String[] result_array = {"", "0", arg[0], arg[1]};
            post.setHeader("Content-type", "application/json");
            try {
                HttpResponse response = httpclient.execute(post);
                int status = response.getStatusLine().getStatusCode();

                if (status == 200) {
                    HttpEntity e = response.getEntity();
                    String data = EntityUtils.toString(e);
                    mUser = arg[0];
                    Log.i("LoginTask", "Received logging in response: " + data);
                    //setState(LOGGED_IN);
                    result_array[0] = data;
                    result_array[1] = "200";
                } else {
                    Log.i("LoginTask", "Received status code " + status + " in response: " + response.getStatusLine().getReasonPhrase());
                    result_array[0] = "Error";
                    result_array[1] = String.valueOf(status);
                }
            } catch (IOException e) {
                e.printStackTrace();
                result_array[0] = "IOException";
            }
            return result_array;
        }
    }
}
