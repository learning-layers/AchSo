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

    public static boolean openCreateGroupActivity(Context context)
    {
        Uri uri = Uri.parse(context.getString(R.string.achRailsUrl))
                .buildUpon()
                .appendPath("groups")
                .appendPath("new")
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
                    Intent intent = new Intent(context, SharingActivity.class);
                    intent.setData(uri);
                    intent.putExtra(SharingActivity.ARG_TOKEN, token);
                    context.startActivity(intent);
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
                    loadUrlWithToken(uri, token);
                }
            });
        } else {
            cookieManager.removeAllCookie();
            loadUrlWithToken(uri, token);
        }
    }

    void loadUrlWithToken(Uri uri, String token) {
        WebView webView = (WebView) findViewById(R.id.WebView);
        Map<String, String> headers = new HashMap<>();

        App.videoRepository.forceNextSyncImportant();

        // Make sure we don't send the token over clear text
        if (uri.getScheme().equals("https")) {
            headers.put("Authorization", "Bearer " + token);
        }
        webView.loadUrl(uri.toString(), headers);
    }
}
