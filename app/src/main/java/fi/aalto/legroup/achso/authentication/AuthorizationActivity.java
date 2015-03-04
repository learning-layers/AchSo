package fi.aalto.legroup.achso.authentication;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.api.client.auth.openidconnect.IdTokenResponse;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Set;

import fi.aalto.legroup.achso.R;

/**
 * An Activity that is launched by the Authenticator for requesting authorisation from the user and
 * creating an Account.
 *
 * The user will interact with the OIDC server via a WebView that monitors the URL for parameters
 * that indicate either a successful authorisation or an error. These parameters are set by the
 * spec.
 *
 * After the Authorization Token has successfully been obtained, we use the single-use token to
 * fetch an ID Token, an Access Token and a Refresh Token. We create an Account and persist these
 * tokens.
 */
public class AuthorizationActivity extends AccountAuthenticatorActivity {

    private final String TAG = getClass().getSimpleName();

    public static final String KEY_AUTH_URL = "fi.aalto.legroup.achso.KEY_AUTH_URL";
    public static final String KEY_IS_NEW_ACCOUNT = "fi.aalto.legroup.achso.KEY_IS_NEW_ACCOUNT";
    public static final String KEY_ACCOUNT_OBJECT = "fi.aalto.legroup.achso.KEY_ACCOUNT_OBJECT";

    private AccountManager accountManager;
    private Account account;
    private boolean isNewAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        accountManager = AccountManager.get(this);

        Bundle extras = getIntent().getExtras();

        // Are we supposed to create a new account or renew the authorisation of an old one?
        isNewAccount = extras.getBoolean(KEY_IS_NEW_ACCOUNT, false);

        // In case we're renewing authorisation, we also got an Account object that we're supposed
        // to work with.
        account = extras.getParcelable(KEY_ACCOUNT_OBJECT);

        // Fetch the authentication URL that was given to us by the calling activity
        String authUrl = extras.getString(KEY_AUTH_URL);

        // Initialise the WebView
        WebView webView = (WebView) findViewById(R.id.WebView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String urlString, Bitmap favicon) {
                super.onPageStarted(view, urlString, favicon);

                Uri url = Uri.parse(urlString);
                Set<String> parameterNames = url.getQueryParameterNames();

                // The URL will contain a `code` parameter when the user has been authenticated
                if (parameterNames.contains("code")) {
                    // We won't need to keep loading anymore. This also prevents errors when using
                    // redirect URLs that don't have real protocols (like app://) that are just
                    // used for identification purposes in native apps.
                    view.stopLoading();

                    String authToken = url.getQueryParameter("code");

                    // Request the ID token
                    RequestIdTokenTask task = new RequestIdTokenTask();
                    task.execute(authToken);
                } else if (parameterNames.contains("error")) {
                    view.stopLoading();

                    // In case of an error, the `error` parameter contains an ASCII identifier, e.g.
                    // "temporarily_unavailable" and the `error_description` *may* contain a
                    // human-readable description of the error.
                    //
                    // For a list of the error identifiers, see
                    // http://tools.ietf.org/html/rfc6749#section-4.1.2.1

                    String error = url.getQueryParameter("error");
                    String errorDescription = url.getQueryParameter("error_description");

                    // If the user declines to authorise the app, there's no need to show an error
                    // message.
                    //
                    // TODO: Read error codes and provide a more helpful error message
                    if ( ! error.equals("access_denied")) {
                        showErrorDialog(String.format("Error code: %s\n\n%s", error,
                                errorDescription));
                    }
                }
            }
        });

        webView.loadUrl(authUrl);
    }

    /**
     * Requests the ID Token asynchronously.
     */
    private class RequestIdTokenTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... args) {
            String authToken = args[0];
            IdTokenResponse response;

            try {
                response = OIDCUtils.requestTokens(
                        OIDCConfig.getAuthorizationServerUrl(AuthorizationActivity.this),
                        OIDCConfig.getTokenServerUrl(AuthorizationActivity.this),
                        OIDCConfig.getRedirectUrl(AuthorizationActivity.this),
                        OIDCConfig.getClientId(AuthorizationActivity.this),
                        OIDCConfig.getClientSecret(AuthorizationActivity.this),
                        authToken);
            } catch (IOException e) {
                Log.e(TAG, "Could not get token response.");
                e.printStackTrace();
                return false;
            }

            if (isNewAccount) {
                createAccount(response);
            } else {
                setTokens(response);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean wasSuccess) {
            if (wasSuccess) {
                // The account manager still wants the following information back
                Intent intent = new Intent();

                intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
                intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);

                setAccountAuthenticatorResult(intent.getExtras());

                setResult(RESULT_OK, intent);
                finish();
            } else {
                showErrorDialog("Could not get ID Token.");
            }
        }
    }

    private void createAccount(IdTokenResponse response) {
        // AccountManager expects that each account has a unique username. If a new account has the
        // same username as a previously created one, it will overwrite the older account.
        //
        // Unfortunately the OIDC spec cannot guarantee[1] that any user information is unique,
        // save for the user ID (i.e. the ID Token subject) which is hardly human-readable. This
        // makes choosing between multiple accounts difficult.
        //
        // We'll resort to naming each account `preferred_username (ID)`. This is a neat solution
        // if the user ID is short enough.
        //
        // [1]: http://openid.net/specs/openid-connect-basic-1_0.html#ClaimStability

        // Use the app name as a fallback if the other information isn't available for some reason.
        String accountName = getString(R.string.app_name);
        String accountId = null;

        try {
            accountId = response.parseIdToken().getPayload().getSubject();
        } catch (IOException e) {
            Log.e(TAG, "Could not get ID Token subject.");
            e.printStackTrace();
        }

        // Get the user information so we can grab the `preferred_username`
        JsonObject userInfo = new JsonObject();

        try {
            userInfo = OIDCUtils.getUserInfo(OIDCConfig.getUserInfoUrl(this), response.getIdToken());
        } catch (IOException e) {
            Log.e(TAG, "Could not get UserInfo.");
            e.printStackTrace();
        }

        if (userInfo.has("preferred_username")) {
            accountName = userInfo.get("preferred_username").getAsString();
        }

        account = new Account(String.format("%s (%s)", accountName, accountId), Authenticator.ACH_SO_ACCOUNT_TYPE);
        accountManager.addAccountExplicitly(account, null, null);

        // Store the tokens in the account
        setTokens(response);
    }

    private void setTokens(IdTokenResponse response) {
        accountManager.setAuthToken(account, Authenticator.TOKEN_TYPE_ID, response.getIdToken());
        accountManager.setAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, response.getAccessToken());
        accountManager.setAuthToken(account, Authenticator.TOKEN_TYPE_REFRESH, response.getRefreshToken());
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Sorry, there was an error")
                .setMessage(message)
                .setNeutralButton("Close", null)
                .create()
                .show();
    }
}
