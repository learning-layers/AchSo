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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.authenticator.OIDCAuthenticator;
import fi.aalto.legroup.achso.util.APIUtility;
import fi.aalto.legroup.achso.util.App;

/**
 * Created by purma on 25.8.2014.
 */
public class OIDCLoginState implements LoginState {

    private final String TAG = this.getClass().getSimpleName();
    private final AccountManager accountManager;
    private final Context mContext;
    private String mAuthToken;
    private String mUser;
    private String mFullname;
    private String mEmail;
    private int mCurrentState;
    private Account mAccount;
    private boolean disable_autologin_for_session;
    private final int AUTH_ERROR = 0;
    private final int AUTH_INTENT_RECEIVED = 1;
    private final int AUTH_SUCCESS = 2;

    public OIDCLoginState(Context ctx) {
        accountManager = AccountManager.get(ctx);
        mCurrentState = LOGGED_OUT;
        this.mContext = ctx;
        disable_autologin_for_session = false;
    }

    @Override
    public String getPublicUrl() {
        return null;
    }

    @Override
    public boolean isIn() {
        return (mCurrentState == LOGGED_IN);
    }

    @Override
    public boolean isOut() {
        return (mCurrentState == LOGGED_OUT);
    }

    @Override
    public boolean isTrying() {
        return (mCurrentState == TRYING_TO_LOG_IN);
    }

    @Override
    public String getUser() {
        return null;
    }

    @Override
    public String getAuthToken() {
        return null;
    }

    @Override
    public int getState() {
        return mCurrentState;
    }

    @Override
    public void logout() {
        Log.i(TAG, "Logout called");

        SharedPreferences prefs = mContext.getSharedPreferences("AchSoPrefs", 0);
        SharedPreferences.Editor editable_prefs = prefs.edit();
        editable_prefs.putBoolean("autologin", false);
        editable_prefs.putString("account_name", "");
        editable_prefs.apply();
        setState(LOGGED_OUT);
    }

    @Override
    public void launchLoginActivity(final Activity host_activity) {
        // Grab all our accounts
        Log.i(TAG, "launchLoginActivity called");
        final Account availableAccounts[] = accountManager.getAccountsByType(App.ACHSO_ACCOUNT_TYPE);

        switch (availableAccounts.length) {
            // No account has been created, let's create one now
            case 0:
                accountManager.addAccount(App.ACHSO_ACCOUNT_TYPE, OIDCAuthenticator.TOKEN_TYPE_ID, null, null, host_activity, new AccountManagerCallback<Bundle>() {
                            @Override
                            public void run(AccountManagerFuture<Bundle> futureManager) {
                                // Unless the account creation was cancelled, try logging in again
                                // after the account has been created.
                                try {
                                    Log.i(TAG, "New account with name: " + futureManager.getResult().get(AccountManager.KEY_ACCOUNT_NAME) + " and type: " +
                                            futureManager.getResult().get(AccountManager.KEY_ACCOUNT_TYPE));
                                } catch (OperationCanceledException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (AuthenticatorException e) {
                                    e.printStackTrace();
                                }
                                ;
                                if (futureManager.isCancelled()) {
                                    return;
                                }
                                final Account accounts[] = accountManager.getAccountsByType(App.ACHSO_ACCOUNT_TYPE);
                                if (accounts.length > 0) {
                                    launchLoginActivity(host_activity);
                                } else {
                                    Log.e(TAG, "addAccount failed to create new account.");
                                }
                            }
                        }, null);
                break;

            // There's just one account, let's use that
            case 1:
                new LoginTask().execute(availableAccounts[0]);
                break;

            // Multiple accounts, let the user pick one
            default:
                String name[] = new String[availableAccounts.length];

                for (int i = 0; i < availableAccounts.length; i++) {
                    name[i] = availableAccounts[i].name;
                }

                new AlertDialog.Builder(host_activity).setTitle("Choose an account").setAdapter(new ArrayAdapter<String>(host_activity, android.R.layout.simple_list_item_1, name), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedAccount) {
                        new LoginTask().execute(availableAccounts[selectedAccount]);
                    }
                }).create().show();
        }
    }

    @Override
    public void autologinIfAllowed() {
        // we probably have logged in already
        Log.d(TAG, "Checking if autologin is necessary.");
        if (!disable_autologin_for_session) {
            SharedPreferences prefs = mContext.getSharedPreferences("AchSoPrefs", 0);
            boolean autologin = prefs.getBoolean("autologin", false);
            String account_name = prefs.getString("account_name", "");
            Log.d(TAG, "autologin: " + autologin + " account_name:" + account_name + " " +
                    "connection: " + App.hasConnection());
            if (autologin && !account_name.isEmpty() && App.hasConnection()) {
                Log.d(TAG, "Starting autologin.");
                final Account availableAccounts[] = accountManager.getAccountsByType(App.ACHSO_ACCOUNT_TYPE);
                for (int i=0; i < availableAccounts.length; i++) {
                    Log.d(TAG, "Found account: " + availableAccounts[i].name + ", " +
                            "matching it with " + account_name );
                    if (availableAccounts[i].name.equals(account_name)) {
                        Log.d(TAG, "Launching loginTask from autologin.");
                        new LoginTask().execute(availableAccounts[i]);
                    }
                }
            }
        }
    }

    @Override
    public void login(String user, String pass) {

    }

    @Override
    public void resumeAuthentication(String next_url) {

    }

    private void setState(int new_state) {
        if (new_state == LOGGED_OUT) {
            mUser = null;
        } else if (new_state == LOGGED_IN && new_state != mCurrentState) {
            disable_autologin_for_session = true; // don't try to login anymore
        }
        Intent intent = new Intent();
        if (new_state == LOGGED_OUT) {
            Log.i(TAG, "LOGGED OUT.");
            intent.setAction(LOGIN_FAILED);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        } else if (new_state == LOGGED_IN) {
            Log.i(TAG, "LOGGED IN.");
            intent.setAction(LOGIN_SUCCESS);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
        mCurrentState = new_state;
    }

    private void launchLoginScreenIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.getContext().startActivity(intent);
    }

    private void finishLogin(Account account, Map user_info) {
        // remember this account as a working account for this login session.
        mAccount = account;

        // put userInfo to easily accessible places in LoginState
        mFullname = user_info.get("name").toString();
        mEmail = user_info.get("email").toString();
        mUser = user_info.get("preferred_username").toString();
        Toast.makeText(mContext, mContext.getString(R.string.Welcome) + ", " +
                mFullname, Toast.LENGTH_LONG).show();

        // Remember how to login for future
        SharedPreferences prefs = mContext.getSharedPreferences("AchSoPrefs", 0);
        SharedPreferences.Editor editable_prefs = prefs.edit();
        editable_prefs.putBoolean("autologin", true);
        editable_prefs.putString("account_name", mAccount.name);
        editable_prefs.apply();

        // launch UI update calls etc.
        setState(LOGGED_IN);

    }


    private class LoginTask extends AsyncTask<Account, Void, Bundle> {
        /**
         * Makes the API request. We could use the OIDCUtils.getUserInfo() method, but we'll do it
         * like this to illustrate making generic API requests.
         */
        @SuppressLint("NewApi")
        @Override
        protected Bundle doInBackground(Account... accounts) {
            Bundle bundle = new Bundle();
            Account account = accounts[0];
            bundle.putParcelable("account", account);
            String idToken = null;
            Intent intent = null;
            Log.d(TAG, "Executing ApiTask.");

            try {
                // Signature for getAuthToken has changed in API 14.
                AccountManagerFuture<Bundle> futureManager;
                if (App.API_VERSION >= 14) {
                    futureManager = accountManager.getAuthToken(account, OIDCAuthenticator.TOKEN_TYPE_ID, null, true, null, null);
                } else {
                    futureManager = accountManager.getAuthToken(account, OIDCAuthenticator.TOKEN_TYPE_ID, true, null, null);
                }
                Bundle auth_result = futureManager.getResult();
                idToken = auth_result.getString(AccountManager.KEY_AUTHTOKEN);
                intent = (Intent) auth_result.get(AccountManager.KEY_INTENT);
                Log.d(TAG, "idToken: " + idToken);
                bundle.putBundle("auth_result", auth_result);
            } catch (Exception e) {
                e.printStackTrace();
                bundle.putInt("success", AUTH_ERROR);
                bundle.putString("error", "problem with account manager");
            }
            if (idToken != null) {
                try {
                    bundle.putSerializable("user_info", (java.io.Serializable) APIUtility.getJson(App.getContext(), App.oidc_config.userInfoUrl, idToken));
                    bundle.putInt("success", AUTH_SUCCESS);
                } catch (IOException e) {
                    Log.e(TAG, "Received IO Error: " + e.getMessage());
                    //e.printStackTrace();
                    bundle.putInt("success", AUTH_ERROR);
                    bundle.putString("error", "server failed to give userInfo: "+ e.getMessage());
                }
            } else if (intent != null) {
                Log.i(TAG, "Received intent to display login screen");
                bundle.putInt("success", AUTH_INTENT_RECEIVED);
            } else {
                bundle.putString("error", "missing idToken");
                bundle.putInt("success", AUTH_ERROR);
                Log.i(TAG, "Authentication had problems");
            }
            return bundle;
        }

        @Override
        protected void onPreExecute() {
            setState(TRYING_TO_LOG_IN);
        }

        /**
         * Processes the API's response.
         */
        @Override
        protected void onPostExecute(Bundle bundle) {
            switch(bundle.getInt("success", AUTH_ERROR)) {
                case AUTH_SUCCESS:
                    Map user_info = (Map) bundle.getSerializable("user_info");
                    Account account = bundle.getParcelable("account");
                    finishLogin(account, user_info);
                    break;
                case AUTH_INTENT_RECEIVED:
                    Bundle auth_result = bundle.getBundle("auth_result");
                    Intent intent = (Intent) auth_result.get(AccountManager.KEY_INTENT);
                    Log.i(TAG, "Couldn't finish without user interaction: launching login screen.");
                    launchLoginScreenIntent(intent);
                    break;
                case AUTH_ERROR:
                    String error = bundle.getString("error");
                    Toast.makeText(mContext, "Login error: "+ error, Toast.LENGTH_LONG).show();
                    setState(LOGGED_OUT);
            }
        }

    }

}
