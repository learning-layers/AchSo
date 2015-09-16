package fi.aalto.legroup.achso.sharing;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.common.base.Strings;
import com.google.common.base.Joiner;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.authentication.Authenticator;

public class SharingActivity extends Activity {

    public static final String ARG_TOKEN = "ARG_TOKEN";
    public static final String ARG_REFRESH_TOKEN = "ARG_REFRESH_TOKEN";

    public static boolean openShareActivity(Context context, List<UUID> videoIds)
    {
         Uri uri = Uri.parse(context.getString(R.string.achRailsUrl))
                .buildUpon()
                .appendPath("videos")
                .appendPath(Joiner.on(',').join(videoIds))
                .appendPath("shares")
                .build();

        return openSharingActivity(context, uri);
    }

    public static boolean openManageGroupsActivity(Context context)
    {
        Uri uri = Uri.parse(context.getString(R.string.achRailsUrl))
                .buildUpon()
                .appendPath("groups")
                .build();

        return openSharingActivity(context, uri);
    }

    public static boolean openSharingActivity(final Context context, final Uri uri) {
        if (App.loginManager.isLoggedOut()) {
            SnackbarManager.show(Snackbar.with(context).text(R.string.not_loggedin_share_nag));
            return false;
        }

        Account account = App.loginManager.getAccount();
        if (account == null) {
            SnackbarManager.show(Snackbar.with(context).text(R.string.not_loggedin_share_nag));
            return false;
        }

        AccountManager accountManager = AccountManager.get(context);

        AccountManagerTokenRetriever retriever = new AccountManagerTokenRetriever(accountManager, account);
        retriever.getTokens(new TokenCallback() {
            @Override
            public void run(String token, String refreshToken) {
                Intent intent = new Intent(context, SharingActivity.class);
                intent.setData(uri);
                intent.putExtra(SharingActivity.ARG_TOKEN, token);
                intent.putExtra(SharingActivity.ARG_REFRESH_TOKEN, refreshToken);
                context.startActivity(intent);
            }
        });

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_authentication);

        WebView webView = (WebView) findViewById(R.id.WebView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        final Uri uri = getIntent().getData();
        final String token = getIntent().getStringExtra(ARG_TOKEN);
        final String refreshToken = getIntent().getStringExtra(ARG_REFRESH_TOKEN);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
        });

        // Remove last session before starting a new one. (Make sure can't get to old login)
        // NOTE: This affects all the cookies in this app globally, if we want to store some other
        // WebView session data that should not be removed should be more careful here.
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    loadUrlWithToken(uri, token, refreshToken);
                }
            });
        } else {
            cookieManager.removeAllCookie();
            loadUrlWithToken(uri, token, refreshToken);
        }
    }

    void loadUrlWithToken(Uri uri, String token) {
        loadUrlWithToken(uri, token, null);
    }

    void loadUrlWithToken(Uri uri, String token, String refreshToken) {
        WebView webView = (WebView) findViewById(R.id.WebView);
        Map<String, String> headers = new HashMap<>();

        App.videoRepository.forceNextSyncImportant();

        // Make sure we don't send the token over clear text
        if (uri.getScheme().equals("https")) {
            if (!Strings.isNullOrEmpty(token)) {
                headers.put("Authorization", "Bearer " + token);
            }
            if (!Strings.isNullOrEmpty(refreshToken))
                headers.put("X-Refresh-Token", refreshToken);
        }
        webView.loadUrl(uri.toString(), headers);
    }

    private static class AccountManagerTokenRetriever {

        private final AccountManager accountManager;
        private final Account account;
        private String token = null;
        private String refreshToken = null;

        public AccountManagerTokenRetriever(AccountManager accountManager, Account account) {
            this.accountManager = accountManager;
            this.account = account;
        }

        public void getTokens(TokenCallback callback) {
            getAuthToken(Authenticator.TOKEN_TYPE_ACCESS, new IDTokenCallback(callback));
        }

        private void getAuthToken(String type, AccountManagerCallback<Bundle> callback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                accountManager.getAuthToken(account, type, null, true,
                        callback, null);
            } else {
                accountManager.getAuthToken(account, type, true,
                        callback, null);
            }
        }

        private class IDTokenCallback implements AccountManagerCallback<Bundle> {

            private TokenCallback callback;

            public IDTokenCallback(TokenCallback callback) {
                this.callback = callback;
            }

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bundle = future.getResult();
                    token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                    e.printStackTrace();
                }
                getAuthToken(Authenticator.TOKEN_TYPE_REFRESH, new RefreshTokenCallback(callback));
            }
        }
        private class RefreshTokenCallback implements AccountManagerCallback<Bundle> {

            private TokenCallback callback;

            public RefreshTokenCallback(TokenCallback callback) {
                this.callback = callback;
            }

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bundle = future.getResult();
                    refreshToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                    e.printStackTrace();
                }

                callback.run(token, refreshToken);
            }
        }
    }

    public interface TokenCallback {
        void run(String token, String refreshToken);
    }
}
