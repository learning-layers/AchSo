package fi.aalto.legroup.achso.authentication;

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

/**
 * Used by Android's AccountManager to manage our account information.
 *
 * The three OpenID tokens (not counting the single-use Authorization Token that is discarded) are
 * stored as what Android calls "auth tokens". They all have different token types:
 *
 * ID Token:      TOKEN_TYPE_ID
 * Access Token:  TOKEN_TYPE_ACCESS  (replaceable by the ID Token, so we're not really using this)
 * Refresh Token: TOKEN_TYPE_REFRESH
 */
public class Authenticator extends AbstractAccountAuthenticator {

    public static final String ACH_SO_ACCOUNT_TYPE = "fi.aalto.legroup.achso.ll_oidc";

    public static final String TOKEN_TYPE_ID = "fi.aalto.legroup.achso.TOKEN_TYPE_ID";
    public static final String TOKEN_TYPE_ACCESS = "fi.aalto.legroup.achso.TOKEN_TYPE_ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "fi.aalto.legroup.achso.TOKEN_TYPE_REFRESH";

    private final String TAG = getClass().getSimpleName();

    private Context context;
    private AccountManager accountManager;

    public Authenticator(Context context) {
        super(context);
        this.context = context.getApplicationContext();

        accountManager = AccountManager.get(context);
    }

    /**
     * Called when the user adds a new account through Android's system settings or when an app
     * explicitly calls this.
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options) {

        Bundle result = new Bundle();

        Intent intent = createIntentForAuthorization(response);

        // We're creating a new account, not just renewing our authorisation
        intent.putExtra(AuthorizationActivity.KEY_IS_NEW_ACCOUNT, true);

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

        // Try to retrieve a stored token
        String token = accountManager.peekAuthToken(account, authTokenType);

        if (TextUtils.isEmpty(token)) {
            // If we don't have one or the token has been invalidated, we need to check if we have
            // a refresh token
            String refreshToken = accountManager.peekAuthToken(account, TOKEN_TYPE_REFRESH);

            if (TextUtils.isEmpty(refreshToken)) {
                // If we don't even have a refresh token, we need to launch an intent for the user
                // to get us a new set of tokens by authorising us again.

                Bundle result = new Bundle();
                Intent intent = createIntentForAuthorization(response);

                // Provide the account that we need re-authorised
                intent.putExtra(AuthorizationActivity.KEY_ACCOUNT_OBJECT, account);

                result.putParcelable(AccountManager.KEY_INTENT, intent);
                return result;
            } else {
                // Got a refresh token, let's use it to get a fresh set of tokens

                IdTokenResponse tokenResponse;

                try {
                    tokenResponse = OIDCUtils.refreshTokens(
                            OIDCConfig.getTokenServerUrl(context),
                            OIDCConfig.getClientId(context),
                            OIDCConfig.getClientSecret(context),
                            OIDCConfig.getScopes(context),
                            refreshToken);

                    accountManager.setAuthToken(account, TOKEN_TYPE_ID, tokenResponse.getIdToken());
                    accountManager.setAuthToken(account, TOKEN_TYPE_ACCESS, tokenResponse.getAccessToken());
                    accountManager.setAuthToken(account, TOKEN_TYPE_REFRESH, tokenResponse.getRefreshToken());
                } catch (IOException e) {
                    // There's not much we can do if we get here
                    Log.e(TAG, "Couldn't refresh tokens.");
                    e.printStackTrace();
                }

                // Now, let's return the token that was requested
                token = accountManager.peekAuthToken(account, authTokenType);
            }
        }

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
        switch (authTokenType) {
            case TOKEN_TYPE_ACCESS:
                return "Access Token";

            case TOKEN_TYPE_ID:
                return "ID Token";

            case TOKEN_TYPE_REFRESH:
                return "Refresh Token";

            default:
                return null;
        }
    }

    /**
     * Create an intent for showing the authorisation web page.
     */
    private Intent createIntentForAuthorization(AccountAuthenticatorResponse response) {
        Intent intent = new Intent(context, AuthorizationActivity.class);

        // Generate a new authorisation URL
        String authUrl = OIDCUtils.newAuthorizationUrl(
                OIDCConfig.getAuthorizationServerUrl(context),
                OIDCConfig.getTokenServerUrl(context),
                OIDCConfig.getRedirectUrl(context),
                OIDCConfig.getClientId(context),
                OIDCConfig.getClientSecret(context),
                OIDCConfig.getScopes(context));

        intent.putExtra(AuthorizationActivity.KEY_AUTH_URL, authUrl);
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
