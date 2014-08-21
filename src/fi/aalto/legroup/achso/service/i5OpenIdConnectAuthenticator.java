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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.i5OpenIdConnectAuthenticatorActivity;
import fi.aalto.legroup.achso.state.i5OpenIdConnectLoginState;

/**
* Created by purma on 4.7.2014.
*/
public class i5OpenIdConnectAuthenticator extends AbstractAccountAuthenticator {
    private static final String TAG = "i5OpenIdConnectAuthenticator";
    private Context mContext;


    public i5OpenIdConnectAuthenticator(Context context) {
        super(context);
        mContext = context;
        Log.i(TAG, "i5OpenIdConnectAuthenticator created");
    }

    /**
     * From AbstractAccountAuthenticator documentation:
     * Returns a Bundle that contains the Intent of the activity that can be used to edit
     * the properties. In order to indicate success the activity should call response
     * .setResult() with a non-null Bundle.
     * __Parameters__
     * @param response used to set the result for the request. If the Constants.INTENT_KEY is
     * set in the bundle then this response field is to be used for sending future results
     * if and when the Intent is started.
     * @param accountType	the AccountType whose properties are to be edited.
     * __Returns__
     * @return a Bundle containing the result or the Intent to start to continue the request. If
     * this is null then the request is considered to still be active and the result should
     * be sent later using response.
     */
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    /**
     * From AbstractAccountAuthenticator documentation:
     * Adds an account of the specified accountType. Note that this can be called from
     * Settings|add account, so this shouldn't make too much assumptions about Ach so! login
     * process.
     * __Parameters__
     * @param response to send the result back to the AccountManager, will never be null
     * @param accountType the type of account to add, will never be null
     * @param authTokenType the type of auth token to retrieve after adding the account, may be null
     * @param requiredFeatures a String array of authenticator-specific features that the added account must support, may be null
     * @param options a Bundle of authenticator-specific options, may be null
     * __Returns__
     * @return a Bundle result or null if the result is to be returned via the response. The result will contain either:
     * KEY_INTENT, or
     * KEY_ACCOUNT_NAME and KEY_ACCOUNT_TYPE of the account that was added, or
     * KEY_ERROR_CODE and KEY_ERROR_MESSAGE to indicate an error
     * __Throws__
     * @throws android.accounts.NetworkErrorException if the authenticator could not honor the request due to a network error
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures,
                             Bundle options) throws NetworkErrorException {
        Log.i(TAG, "addAccount called with response: "+ response.toString() + " accountType: " +
                        "" + accountType + " authTokenType: " + authTokenType + " " +
                        "requiredFeatures: " + Arrays.toString(requiredFeatures) + options
                .toString());

        final Intent intent = new Intent(mContext, i5OpenIdConnectAuthenticatorActivity.class);
        intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    /**
     * From AbstractAccountAuthenticator documentation:
     * Checks that the user knows the credentials of an account.
     * __Parameters__
     * @param response 	to send the result back to the AccountManager, will never be null
     * @param account	the account whose credentials are to be checked, will never be null
     * @param options	a Bundle of authenticator-specific options, may be null
     * __Returns__
     * @return a Bundle result or null if the result is to be returned via the response. The
     * result will contain either:
     * KEY_INTENT, or
     * KEY_BOOLEAN_RESULT, true if the check succeeded, false otherwise
     * KEY_ERROR_CODE and KEY_ERROR_MESSAGE to indicate an error
     * __Throws__
     * @throws android.accounts.NetworkErrorException	if the authenticator could not honor the request due
     * to a network error
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {

        return null;
    }

    /**
     * Gets the authtoken for an account. This is the usual way to get to the login screen.
     * __Parameters__
     * @param response to send the result back to the AccountManager, will never be null
     * @param account the account whose credentials are to be retrieved, will never be null
     * @param authTokenType the type of auth token to retrieve, will never be null
     * @param options a Bundle of authenticator-specific options, may be null
     * __Returns__
     * @return a Bundle result or null if the result is to be returned via the response. The result will contain either:
     * KEY_INTENT, or
     * KEY_ACCOUNT_NAME, KEY_ACCOUNT_TYPE, and KEY_AUTHTOKEN, or
     * KEY_ERROR_CODE and KEY_ERROR_MESSAGE to indicate an error
     * __Throws__
     * @throws android.accounts.NetworkErrorException if the authenticator could not honor the request due to a network error
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws
            NetworkErrorException {
        Log.i(TAG, "getAuthToken called with account: " + account.toString() + " " +
                        "authTokenType: " + authTokenType + " options: " + options.toString());

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);

        String authToken = am.peekAuthToken(account, authTokenType);
        String password = null;

        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            password = am.getPassword(account);
            if (password != null) {
                Bundle login_result = i5OpenIdConnectLoginState.userSignIn(account.name, password);
                authToken = login_result.getString("session_id");
            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(mContext, i5OpenIdConnectAuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, false);
        intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_PREFILLED_USERNAME, account.name);
        intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_PREFILLED_PASSWORD, password);
        if (password != null) {
            intent.putExtra(i5OpenIdConnectAuthenticatorActivity.ARG_MESSAGE,
                    mContext.getString(R.string.login_failed_help_text));

        }
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    /**
     * From AbstractAccountAuthenticator documentation:
     * Ask the authenticator for a localized label for the given authTokenType.
     *
     * __Parameters__
     * @param authTokenType	the authTokenType whose label is to be returned,
     * will never be null
     * __Returns__
     * @return the localized label of the auth token type, may be null if the type isn't known
     */
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return i5OpenIdConnectAccountService.ACHSO_AUTH_TOKEN_TYPE; // we don't need localized labels for this (yet)
    }

    /**
     * From AbstractAccountAuthenticator documentation:
     * Update the locally stored credentials for an account.
     * __Parameters__
     * @param response to send the result back to the AccountManager, will never be null
     * @param account the account whose credentials are to be updated, will never be null
     * @param authTokenType the type of auth token to retrieve after updating the credentials, may be null
     * @param options a Bundle of authenticator-specific options, may be null
     * __Returns__
     * @return a Bundle result or null if the result is to be returned via the response. The result will contain either:
     * KEY_INTENT, or
     * KEY_ACCOUNT_NAME and KEY_ACCOUNT_TYPE of the account that was added, or
     * KEY_ERROR_CODE and KEY_ERROR_MESSAGE to indicate an error
     * __Throws__
     * @throws android.accounts.NetworkErrorException if the authenticator could not honor the request due to a network error
     */
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
                                    String authTokenType,
                                    Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    /**
     * From AbstractAccountAuthenticator documentation:
     * Checks if the account supports all the specified authenticator specific features.
     *
     * __Parameters__
     * @param response	to send the result back to the AccountManager, will never be null
     * @param account	the account to check, will never be null
     * @param features	an array of features to check, will never be null
     * __Returns__
     * @return Bundle result or null if the result is to be returned via the response.
     * The result will contain either:
     * KEY_INTENT, or
     * KEY_BOOLEAN_RESULT, true if the account has all the features, false otherwise
     * KEY_ERROR_CODE and KEY_ERROR_MESSAGE to indicate an error
     *__Throws__
     * @throws android.accounts.NetworkErrorException	if the authenticator could not honor the request due
     * to a
     * network error
     */
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                              String[] features) throws NetworkErrorException {
        // we don't know about any features.
        boolean got_features = true;
        ArrayList<String> supported_features = new ArrayList<String>();
        // put features we support to that list here:

        // ok then.
        for (String s: features) {
            if (!supported_features.contains(s)) {
                got_features = false;
            }
        }
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, got_features);
        return result;
    }
}
