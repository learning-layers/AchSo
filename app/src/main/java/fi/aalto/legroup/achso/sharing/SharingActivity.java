package fi.aalto.legroup.achso.sharing;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.common.base.Joiner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.authentication.Authenticator;

public class SharingActivity extends Activity {

    public static final String ARG_VIDEO_IDS = "ARG_VIDEO_IDS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_authentication);

        WebView webView = (WebView) findViewById(R.id.WebView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        List<UUID> videoIds = (List<UUID>)getIntent().getSerializableExtra(ARG_VIDEO_IDS);

        final Uri uri = Uri.parse(getString(R.string.achRailsUrl))
            .buildUpon()
            .appendPath("videos")
            .appendPath(Joiner.on(',').join(videoIds))
            .appendPath("shares")
            .build();

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
                    loadUrlAuthenticated(uri);
                }
            });
        } else {
            cookieManager.removeAllCookie();
            loadUrlAuthenticated(uri);
        }

    }


    void loadUrlAuthenticated(final Uri uri) {

        if (App.loginManager.isLoggedOut()) {
            // TODO: Show error
            return;
        }

        Account account = App.loginManager.getAccount();

        if (account == null) {
            // TODO: Show error
            return;
        }

        AccountManager accountManager = AccountManager.get(this);
        String token = null;

        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                Bundle bundle = null;
                try {
                    bundle = future.getResult();
                } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                    e.printStackTrace();
                }
                if (bundle != null) {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    loadUrlWithToken(uri, token);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            accountManager.getAuthToken(account, Authenticator.TOKEN_TYPE_ID, null, true,
                    callback, null);
        } else {
            accountManager.getAuthToken(account, Authenticator.TOKEN_TYPE_ID, true,
                    callback, null);
        }
    }


    void loadUrlWithToken(Uri uri, String token) {
        WebView webView = (WebView) findViewById(R.id.WebView);
        Map<String, String> headers = new HashMap<>();

        // Make sure we don't send the token over clear text
        if (uri.getScheme().equals("https")) {
            headers.put("Authorization", "Bearer " + token);
        }
        webView.loadUrl(uri.toString(), headers);
    }
}
