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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.ActionbarActivity;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.LasConnection;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class LasLoginState implements LoginState {
    public static final int LOGGED_OUT = 0;
    private int mIn = LOGGED_OUT;
    public static final int TRYING_TO_LOG_IN = 1;
    public static final int LOGGED_IN = 2;
    private String mUser = null;
    private Context ctx;
    private String mAuthToken;
    private boolean disable_autologin_for_session = false;

    public LasLoginState(Context ctx) {
        this.ctx = ctx;
    }

    public int getLoginStatus() {
        return mIn;
    }

    public void asynchronousLogin(String user, String pass) {
        setState(LOGGED_OUT);
        mUser = user;
        if (user.isEmpty() || pass.isEmpty()) {
            return;
        } else {
            AsyncTask<String, Void, String> task = new LoginTask();
            task.execute(user, pass);
            setState(TRYING_TO_LOG_IN);
        }

    }

    @Override
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
                    Log.i("LasLoginState", "autologin launched async login.");
                }

            }
        }
    }

    @Override
    public void login(String user, String pass) {
        setState(LOGGED_OUT);
        appendLog(String.format("Doing manual login as %s", user));

        if (user == null || pass == null) {
            return;
        } else {
            // TODO (Petru) Replace the old LAS login with the integrated OpenID login

            //HTTP call - after Android > 3.x the AsyncTask should be used to avoid the android.os.NetworkOnMainThreadException
            //The login uses the util.LasConnection class (taken from AnViAnno) and the http-connector-client.jar library
            AsyncTask<String, Void, String> taskresult = new LoginTask().execute(user, pass);
            setState(TRYING_TO_LOG_IN);

            // The result of the login operation will be a session id
            String result;
            try {
                result = taskresult.get();
            } catch (InterruptedException e) {
                Log.e("LoginState", "LasLogin failed, catched InterruptedException");
                e.printStackTrace();
                setState(LOGGED_OUT);
                return;
            } catch (ExecutionException e) {
                Log.e("LoginState", "LasLogin failed, catched ExecutionException");
                e.printStackTrace();
                setState(LOGGED_OUT);
                return;
            }
            // LoginTask onPostExecute sets mIn to LOGGED_IN or LOGGED_OUT
            if (result.equals(LasConnection.CONNECTION_PROBLEM)) {
                Log.e("LoginState", "LasLogin failed, connection problem");
                setState(LOGGED_OUT);
            } else if (result.equals(LasConnection.AUTHENTICATION_PROBLEM)) {
                Log.e("LoginState", "LasLogin failed, authentication problem");
                setState(LOGGED_OUT);
            } else if (result.equals(LasConnection.UNDEFINED_PROBLEM)) {
                Log.e("LoginState", "LasLogin failed, unknown problem");
                setState(LOGGED_OUT);
            } else {
                Log.i("LoginState", "LasLogin result: " + result);
                mUser = user;
                appendLog(String.format("Logged in as %s", mUser));
                setState(LOGGED_IN);
            }
        }

        if (mIn == LOGGED_IN) {
            SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
            if (prefs.getBoolean("autologin", false)) {
                Editor edit = prefs.edit();
                edit.putString("login", user);
                edit.putString("pwd", pass);
                edit.apply();
            }
        } else if (mIn == TRYING_TO_LOG_IN) {
            Log.e("LasLoginState", "Still trying to log in, even as task has ended. Strange.");
        }
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
        Log.i("LasLoginState", "state set to " + state + ". Should update the icon next. ");
        Intent intent = new Intent();
        if (state == LOGGED_OUT) {
            intent.setAction(LOGIN_FAILED);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        } else if (state == LOGGED_IN) {
            intent.setAction(LOGIN_SUCCESS);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        }


    }

    @Override
    public String getPublicUrl() {
        return null;
    }

    @Override
    public String getAuthToken() {
        return mAuthToken;
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

    public void logout() {
        mUser = null;
        setState(LOGGED_OUT);
        LasConnection lc = (LasConnection) App.connection;
        lc.disconnect();
        SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
        if (prefs.getBoolean("autologin", false)) {
            Editor edit = prefs.edit();
            edit.putBoolean("autologin", false);
            edit.apply();
        }


    }

    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... arg) {
            String response, user, pass;
            if (arg.length < 2) {
                return null;
            }
            user = arg[0];
            pass = arg[1];
            Log.i("LasLoginTask", "Starting background logging in");
            // create the connection; the result will be a session id or an error message
            LasConnection lc = (LasConnection) App.connection;
            response = lc.connect(user, pass);
            //lc.disconnect();
            mUser = user;
            Log.i("LasLoginTask", "Received logging in response: " + response);
            return response;
        }

        protected void onPostExecute(String result) {
            Log.i("LasLoginTask", "doing onPostExecute with result " + result);
            if (result.equals(LasConnection.CONNECTION_PROBLEM)) {
                Toast.makeText(ctx, ctx.getString(R.string.connection_problem), Toast.LENGTH_LONG).show();
                Log.e("LasLoginState", "Login failed, connection problem");
                setState(LOGGED_OUT);
            } else if (result.equals(LasConnection.AUTHENTICATION_PROBLEM)) {
                Toast.makeText(ctx, ctx.getString(R.string.authentication_problem), Toast.LENGTH_LONG).show();
                Log.e("LasLoginState", "Login failed, authentication problem");
                setState(LOGGED_OUT);
            } else if (result.equals(LasConnection.UNDEFINED_PROBLEM)) {
                Toast.makeText(ctx, "Unknown problem connecting", Toast.LENGTH_LONG).show();
                Log.e("LasLoginState", "Login failed, unknown problem");
                setState(LOGGED_OUT);
            } else {
                Log.i("LasLoginState", "Login result: " + result);
                mAuthToken = result;
                setState(LOGGED_IN);
            }
        }
    }
}
