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

package fi.aalto.legroup.achso.authenticator;

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

import com.google.api.client.auth.openidconnect.IdTokenResponse;

import java.io.IOException;

import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.OIDCUtils;

/**
 * Used by Android's AccountManager to manage our account information.
 *
 * The three OpenID tokens (not counting the single-use Authorization Token that is discarded) are
 * stored as what Android calls "auth tokens". They all have different token types:
 *
 * ID Token:      TOKEN_TYPE_ID
 * Access Token:  TOKEN_TYPE_ACCESS  (replaceable by the ID Token, so we're not really using this)
 * Refresh Token: TOKEN_TYPE_REFRESH
 *
 * @author Leo Nikkilä
 */
public class OIDCAuthenticator extends AbstractAccountAuthenticator {

    private final String TAG = this.getClass().getSimpleName();

    private Context context;
    private AccountManager accountManager;

    public static final String TOKEN_TYPE_ID = "fi.aalto.legroup.achso.TOKEN_TYPE_ID";
    public static final String TOKEN_TYPE_ACCESS = "fi.aalto.legroup.achso.TOKEN_TYPE_ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "fi.aalto.legroup.achso.TOKEN_TYPE_REFRESH";


    public OIDCAuthenticator(Context context) {
        super(context);
        this.context = context;

        accountManager = AccountManager.get(context);

        Log.d(TAG, "Authenticator created.");
    }

    /**
     * Called when the user adds a new account through Android's system settings or when an app
     * explicitly calls this.
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options) {

        Log.d(TAG, String.format("addAccount called with accountType %s, authTokenType %s.",
                accountType, authTokenType));

        Bundle result = new Bundle();

        Intent intent = createIntentForAuthorization(response);

        // We're creating a new account, not just renewing our authorisation
        intent.putExtra(OIDCAuthenticatorActivity.KEY_IS_NEW_ACCOUNT, true);

        result.putParcelable(AccountManager.KEY_INTENT, intent);

        return result;
    }

    /**
     * Tries to retrieve a previously stored token of any type. If the token doesn't exist yet or
     * has been invalidated, we need to request a set of replacement tokens.
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) {

        Log.d(TAG, String.format("getAuthToken called with account.type '%s', account.name '%s', " +
                "authTokenType '%s'.", account.type, account.name, authTokenType));

        // Try to retrieve a stored token
        String token = accountManager.peekAuthToken(account, authTokenType);

        if (TextUtils.isEmpty(token)) {
            // If we don't have one or the token has been invalidated, we need to check if we have
            // a refresh token
            Log.d(TAG, "Token empty, checking for refresh token.");
            String refreshToken = accountManager.peekAuthToken(account, TOKEN_TYPE_REFRESH);

            if (TextUtils.isEmpty(refreshToken)) {
                // If we don't even have a refresh token, we need to launch an intent for the user
                // to get us a new set of tokens by authorising us again.

                Log.d(TAG, "Refresh token empty, launching intent for renewing authorisation.");

                Bundle result = new Bundle();
                Intent intent = createIntentForAuthorization(response);

                // Provide the account that we need re-authorised
                intent.putExtra(OIDCAuthenticatorActivity.KEY_ACCOUNT_OBJECT, account);

                result.putParcelable(AccountManager.KEY_INTENT, intent);

                return result;
            } else {
                // Got a refresh token, let's use it to get a fresh set of tokens
                Log.d(TAG, "Got refresh token, getting new tokens.");

                IdTokenResponse tokenResponse;

                try {
                    tokenResponse = OIDCUtils.refreshTokens(App.oidc_config.authorizationServerUrl,
                            App.oidc_config.tokenServerUrl, App.oidc_config.clientId, App.oidc_config.clientSecret, refreshToken);

                    Log.d(TAG, "Got new tokens.");

                    accountManager.setAuthToken(account, TOKEN_TYPE_ID, tokenResponse.getIdToken());
                    accountManager.setAuthToken(account, TOKEN_TYPE_ACCESS, tokenResponse.getAccessToken());
                    accountManager.setAuthToken(account, TOKEN_TYPE_REFRESH, tokenResponse.getRefreshToken());
                } catch (IOException e) {
                    // There's not much we can do if we get here
                    Log.e(TAG, "Couldn't get new tokens.");
                    e.printStackTrace();
                }

                // Now, let's return the token that was requested
                token = accountManager.peekAuthToken(account, authTokenType);
            }
        }

        Log.e(TAG, String.format("Returning token '%s' of type '%s'.", token, authTokenType));

        Bundle result = new Bundle();

        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_AUTHTOKEN, token);

        return result;
    }

    /**
     * Returns an appropriate label for each token type.
     * TODO: Not sure what this is for. Remove?
     */
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (authTokenType.equals(TOKEN_TYPE_ACCESS)) {
            return "Access Token";
        } else if (authTokenType.equals(TOKEN_TYPE_ID)) {
            return "ID Token";
        } else if (authTokenType.equals(TOKEN_TYPE_REFRESH)) {
            return "Refresh Token";
        }

        return null;
    }

    /**
     * Create an intent for showing the authorisation web page.
     */
    private Intent createIntentForAuthorization(AccountAuthenticatorResponse response) {
        Intent intent = new Intent(context, OIDCAuthenticatorActivity.class);

        // Generate a new authorisation URL
        String authUrl = OIDCUtils.newAuthorizationUrl(App.oidc_config.authorizationServerUrl,
                App.oidc_config.tokenServerUrl,
                App.oidc_config.redirectUrl,
                App.oidc_config.clientId,
                App.oidc_config.clientSecret,
                App.oidc_config.scopes);

        Log.d(TAG, String.format("Created new intent with authorisation URL '%s'.", authUrl));

        intent.putExtra(OIDCAuthenticatorActivity.KEY_AUTH_URL, authUrl);

        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        return intent;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                              String[] features) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
                                    String authTokenType, Bundle options)
                                    throws NetworkErrorException {
        return null;
    }

}
