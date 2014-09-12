package fi.aalto.legroup.achso.networking;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import com.github.kevinsawicki.http.HttpRequest;

import java.io.IOException;
import java.net.URL;

import fi.aalto.legroup.achso.authenticator.Authenticator;
import fi.aalto.legroup.achso.util.App;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Extends HttpRequest to provide automatic token renewal. Use it like you'd use HttpRequest, but
 * call execute() right before invoking any methods that would close the connection, like ok() or
 * body().
 *
 * TODO: Figure out a way to move token renewal stuff into closeOutput()?
 * https://github.com/kevinsawicki/http-request/blob/master/lib/src/main/java/com/github/kevinsawicki/http/HttpRequest.java#L2643
 *
 * @author Leo Nikkilä
 */
public class OIDCHttpRequest extends HttpRequest {

    protected Context context;
    protected Account account;
    protected boolean hasExecuted = false;

    /**
     * Create an HTTP connection wrapper.
     *
     * @param url     Remote resource URL.
     * @param method  HTTP request method (e.g., "GET", "POST").
     * @param account Account used for accessing a protected resource.
     * @throws com.github.kevinsawicki.http.HttpRequest.HttpRequestException
     */
    public OIDCHttpRequest(Context context, CharSequence url, String method, Account account)
            throws HttpRequestException {
        super(url, method);
        this.context = context;
        this.account = account;
    }

    /**
     * Create an HTTP connection wrapper.
     *
     * @param url     Remote resource URL.
     * @param method  HTTP request method (e.g., "GET", "POST").
     * @param account Account used for accessing a protected resource.
     * @throws com.github.kevinsawicki.http.HttpRequest.HttpRequestException
     */
    public OIDCHttpRequest(Context context, URL url, String method, Account account)
            throws HttpRequestException {
        super(url, method);
        this.context = context;
        this.account = account;
    }

    public OIDCHttpRequest execute() throws HttpRequestException {
        return execute(true);
    }

    @SuppressLint("NewApi")
    public OIDCHttpRequest execute(boolean doRetry) throws HttpRequestException {
        hasExecuted = true;

        AccountManager accountManager = AccountManager.get(context);
        String token;

        // Try retrieving an ID token from the account manager
        try {
            AccountManagerFuture<Bundle> futureManager;

            // Signature for getAuthToken has changed in API 14.
            if (App.API_VERSION >= 14) {
                futureManager = accountManager.getAuthToken(account, Authenticator.TOKEN_TYPE_ID, null, true, null, null);
            } else {
                futureManager = accountManager.getAuthToken(account, Authenticator.TOKEN_TYPE_ID, true, null, null);
            }

            token = futureManager.getResult().getString(AccountManager.KEY_AUTHTOKEN);
        } catch (Exception e) {
            IOException ioException = new IOException("Could not get ID token from account.", e);
            throw new HttpRequestException(ioException);
        }

        header("Authorization", "Bearer " + token);

        // If we're being denied access on the first try, let's renew the token and retry
        if (accessDenied() && doRetry) {
            AccountManager.get(context).invalidateAuthToken(App.ACHSO_ACCOUNT_TYPE, token);
            return execute(false);
        }

        return this;
    }

    /**
     * Close output stream
     *
     * @return this request
     * @throws com.github.kevinsawicki.http.HttpRequest.HttpRequestException
     * @throws java.io.IOException
     */
    @Override
    protected HttpRequest closeOutput() throws IOException {
        if (hasExecuted) {
            return super.closeOutput();
        } else {
            throw new IllegalStateException("Must call execute() before closing connection.");
        }
    }

    protected boolean accessDenied() {
        int code = code();
        return code == HTTP_FORBIDDEN || code == HTTP_UNAUTHORIZED;
    }

}
