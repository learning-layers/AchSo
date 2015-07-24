package fi.aalto.legroup.achso.sharing;

import android.accounts.Account;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.common.base.Joiner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;

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

        Uri uri = Uri.parse(getString(R.string.achRailsUrl))
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

        Account account = App.loginManager.getAccount();
        String token = App.authenticatedHttpClient.getBearerToken(account);

        Map<String, String> headers = new HashMap<>();
        
        // Make sure we don't send the token over clear text
        if (uri.getScheme().equals("https")) {
            headers.put("Authorization", "Bearer " + token);
        }
        webView.loadUrl(uri.toString(), headers);
    }
}
