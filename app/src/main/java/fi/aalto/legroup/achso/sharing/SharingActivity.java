package fi.aalto.legroup.achso.sharing;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.UUID;

import fi.aalto.legroup.achso.R;

public class SharingActivity extends Activity {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_authentication);

        WebView webView = (WebView) findViewById(R.id.WebView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        UUID videoId = (UUID)getIntent().getSerializableExtra(ARG_VIDEO_ID);

        Uri uri = Uri.parse(getString(R.string.achRailsUrl))
            .buildUpon()
            .appendPath("videos")
            .appendPath(videoId.toString())
            .appendPath("shares")
            .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
        });
        webView.loadUrl(uri.toString());
    }
}
