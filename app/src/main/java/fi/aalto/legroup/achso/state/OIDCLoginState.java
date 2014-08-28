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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
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
    private final Context ctx;
    private String mAuthToken;
    private String mUser;
    private String mFullname;
    private String mEmail;
    private int mIn;
    private Account mAccount;
    private boolean disable_autologin_for_session;

    public OIDCLoginState(Context ctx) {
        accountManager = AccountManager.get(ctx);
        mIn = LOGGED_OUT;
        this.ctx = ctx;
        disable_autologin_for_session = false;
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
        return null;
    }

    @Override
    public String getAuthToken() {
        return null;
    }

    @Override
    public int getState() {
        return mIn;
    }

    @Override
    public void logout() {
        Log.i(TAG, "Logout called");

        setState(LOGGED_OUT);
    }

    @Override
    public void launchLoginActivity(Activity host_activity) {
        // Grab all our accounts
        Log.i(TAG, "launchLoginActivity called");
        final Activity host = host_activity;
        final Account availableAccounts[] = accountManager.getAccountsByType(App.ACHSO_ACCOUNT_TYPE);

        switch (availableAccounts.length) {
            // No account has been created, let's create one now
            case 0:
                accountManager.addAccount(App.ACHSO_ACCOUNT_TYPE, OIDCAuthenticator.TOKEN_TYPE_ID, null, null, host, new AccountManagerCallback<Bundle>() {
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
                                    launchLoginActivity(host);
                                } else {
                                    Log.e(TAG, "addAccount failed to create new account.");
                                }
                            }
                        }, null);
                break;

            // There's just one account, let's use that
            case 1:
                new ApiTask().execute(availableAccounts[0]);
                break;

            // Multiple accounts, let the user pick one
            default:
                String name[] = new String[availableAccounts.length];

                for (int i = 0; i < availableAccounts.length; i++) {
                    name[i] = availableAccounts[i].name;
                }

                new AlertDialog.Builder(host_activity).setTitle("Choose an account").setAdapter(new ArrayAdapter<String>(host, android.R.layout.simple_list_item_1, name), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedAccount) {
                                new ApiTask().execute(availableAccounts[selectedAccount]);
                            }
                        }).create().show();
        }
    }

    @Override
    public void autologinIfAllowed() {

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
        } else if (new_state == LOGGED_IN && new_state != mIn) {
            disable_autologin_for_session = true;
        }
        Intent intent = new Intent();
        if (new_state == LOGGED_OUT) {
            intent.setAction(LOGIN_FAILED);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        } else if (new_state == LOGGED_IN) {
            intent.setAction(LOGIN_SUCCESS);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        }

        mIn = new_state;
        Log.i(TAG, "LoginState set to " + new_state + ". Should update the icon next. ");
    }

    private class ApiTask extends AsyncTask<Account, Void, Map> {

        /**
         * Makes the API request. We could use the OIDCUtils.getUserInfo() method, but we'll do it
         * like this to illustrate making generic API requests.
         */
        @Override
        protected Map doInBackground(Account... args) {
            Account account = args[0];
            String idToken = null;
            Log.i(TAG, "Executing ApiTask.");

            try {
                AccountManagerFuture<Bundle> futureManager;
                if (App.API_VERSION >= 14) {
                     futureManager = accountManager.getAuthToken(account, OIDCAuthenticator.TOKEN_TYPE_ID, null, true, null,
                            null);
                } else {
                    futureManager = accountManager.getAuthToken(account, OIDCAuthenticator.TOKEN_TYPE_ID, true, null, null);
                }
                idToken = futureManager.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                Log.i(TAG, "idToken: " + idToken);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                return APIUtility.getJson(App.getContext(), App.oidc_config.userInfoUrl, idToken);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setState(TRYING_TO_LOG_IN);
        }

        /**
         * Processes the API's response.
         */
        @Override
        protected void onPostExecute(Map result) {
            if (result == null) {
                setState(LOGGED_OUT);
            } else {
                Toast.makeText(ctx, ctx.getString(R.string.Welcome) + ", " + result.toString(),
                        Toast.LENGTH_LONG).show();
                setState(LOGGED_IN);
            }
        }

    }

}
