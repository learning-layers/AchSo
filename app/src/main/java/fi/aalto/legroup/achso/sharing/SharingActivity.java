package fi.aalto.legroup.achso.sharing;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.authentication.Authenticator;

public class SharingActivity extends Activity {

    public static final String ARG_TOKEN = "ARG_TOKEN";
    public static final String ARG_REFRESH_TOKEN = "ARG_REFRESH_TOKEN";

    private static final int ACH_SO_ACCESS_CONTACTS = 1;

    private WebView webView;

    private AchRailsJavascriptInterface achRailsJavascriptInterface;

    private static String getLanguageCode()
    {
        String defaultLanguage = "en";
        List<String> supportedLanguages = Arrays.asList("en", "de", "fi", "et");

        String currentLanguage = Locale.getDefault().getLanguage();

        if (supportedLanguages.contains(currentLanguage)) {
            return currentLanguage;
        } else {
            return defaultLanguage;
        }
    }

    public static boolean openShareActivity(Context context, List<UUID> videoIds)
    {
         Uri uri = App.getAchRailsUrl(context)
                .buildUpon()
                .appendPath(getLanguageCode())
                .appendPath("videos")
                .appendPath(Joiner.on(',').join(videoIds))
                .appendPath("shares")
                .build();

        return openSharingActivity(context, uri);
    }

    public static boolean openManageGroupsActivity(Context context)
    {
        Uri uri = App.getAchRailsUrl(context)
                .buildUpon()
                .appendPath(getLanguageCode())
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

        webView = (WebView) findViewById(R.id.WebView);
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

        achRailsJavascriptInterface = new AchRailsJavascriptInterface(this);

        webView.addJavascriptInterface(achRailsJavascriptInterface, "Android");

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
        if (true || uri.getScheme().equals("https")) {
            if (!Strings.isNullOrEmpty(token)) {
                headers.put("Authorization", "Bearer " + token);
            }
            if (!Strings.isNullOrEmpty(refreshToken))
                headers.put("X-Refresh-Token", refreshToken);
        }
        webView.loadUrl(uri.toString(), headers);
    }

    public boolean hasUserGrantedContactAccess() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    public void askForContactPermission() {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.READ_CONTACTS,
            }, ACH_SO_ACCESS_CONTACTS);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == ACH_SO_ACCESS_CONTACTS && grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            achRailsJavascriptInterface.openContactPicker();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == AchRailsJavascriptInterface.CONTACT_PICK_CODE) {
            Uri contactUri = data.getData();

            String contactId = contactUri.getLastPathSegment();
            Cursor cursor = getContentResolver().query(
                    Email.CONTENT_URI, null, Email.CONTACT_ID + "=?",
                    new String[] { contactId }, null);

            int emailIdx = cursor.getColumnIndex(Email.DATA);
            int typeIdx = cursor.getColumnIndex(Email.TYPE);
            int labelIdx = cursor.getColumnIndex(Email.LABEL);

            final List<EmailOption> options = new ArrayList<>();

            if (cursor.moveToFirst()) {
                do {
                    String email = cursor.getString(emailIdx);
                    String type = Email.getTypeLabel(getResources(),
                            cursor.getInt(typeIdx), cursor.getString(labelIdx)).toString();

                    options.add(new EmailOption(email, type));
                } while (cursor.moveToNext());
            }
            cursor.close();

            if (options.size() == 1) {
                addInviteEmail(options.get(0).email);
            } else if (options.size() > 1) {
                String[] optionTexts = new String[options.size()];
                for (int i = 0; i < options.size(); i++) {
                    EmailOption option = options.get(i);
                    optionTexts[i] = option.email + " (" + option.type + ")";
                }
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.select_invite_email)
                        .setSingleChoiceItems(optionTexts, 0, null)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int selected = ((AlertDialog) dialog).getListView()
                                                .getCheckedItemPosition();
                                        if (selected < 0 || selected >= options.size()) {
                                            dialog.cancel();
                                            return;
                                        }
                                        EmailOption option = options.get(selected);
                                        if (option != null) {
                                            addInviteEmail(option.email);
                                        }
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                })
                        .create();

                dialog.show();
            } else {
                Toast.makeText(SharingActivity.this, R.string.contact_has_no_email, Toast.LENGTH_SHORT).show();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void addInviteEmail(String email) {
        runJavascript("addInviteEmail('" + email + "')");
    }

    private void runJavascript(String script) {
        if (webView == null)
            return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script + ";", null);
        } else {
            webView.loadUrl("javascript:" + script + ";");
        }
    }

    private static class EmailOption {
        public String email;
        public String type;

        public EmailOption(String email, String type) {
            this.email = email;
            this.type = type;
        }
    }

    public interface TokenCallback {
        void run(String token, String refreshToken);
    }
}
