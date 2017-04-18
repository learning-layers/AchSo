package fi.aalto.legroup.achso.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Utilises HttpClient to provide automatic token renewal.
 */
public class AuthenticatedHttpClient {

    private final String TAG = getClass().getSimpleName();

    protected Context context;
    protected OkHttpClient httpClient;

    public AuthenticatedHttpClient(Context context, OkHttpClient httpClient) {
        this.context = context.getApplicationContext();
        this.httpClient = httpClient;
    }

    public String getBearerToken(Account account) {
        AccountManager accountManager = AccountManager.get(context);
        String token = null;

        // Try retrieving an access token from the account manager
        try {
            token = accountManager.blockingGetAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, true);
        } catch (Exception e) {
            Log.e(TAG, "Could not get access token from account: " + e.getMessage());
            e.printStackTrace();
        }
        return token;
    }

    public Response execute(Request request, Account account) throws IOException {
        return execute(request, account, true);
    }

    public Response execute(Request request, Account account, boolean doRetry) throws IOException {
        AccountManager accountManager = AccountManager.get(context);

        String token = getBearerToken(account);

        request = request.newBuilder().header("Authorization", "Bearer " + token).build();

        Response response = httpClient.newCall(request).execute();

        // If we're being denied access on the first try, let's renew the token and retry
        if (accessDenied(response) && doRetry) {
            accountManager.invalidateAuthToken(Authenticator.ACH_SO_ACCOUNT_TYPE, token);
            return execute(request, account, false);
        }

        return response;
    }

    public void enqueue(Request request, Account account, Callback cb) {
        String token = getBearerToken(account);

        request = request.newBuilder().header("Authorization", "Bearer " + token).build();

        httpClient.newCall(request).enqueue(cb);
    }

    public boolean accessDenied(Response response) {
        int code = response.code();

        // FIXME: The internal error is for SSS when tokens are expired
        return code == HTTP_UNAUTHORIZED
                || code == HTTP_FORBIDDEN
                || code == HTTP_NOT_FOUND
                || code == HTTP_INTERNAL_ERROR;
    }

}
