/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import fi.aalto.legroup.achso.R;

public class ApprovalActivity extends ActionbarActivity {
    private static final String TAG = "ApprovalActivity";
    String mUrl;
    private String mHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approval);
    }

    @Override
    public void onResume() {
        super.onResume();
        mUrl = getIntent().getStringExtra("url");
        mHost = Uri.parse(mUrl).getHost();
        Log.i(TAG, "going to: " + mUrl);
        class GrantAccessWebView extends WebViewClient {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.i(TAG, url);
                Uri uri = Uri.parse(url);
                if (uri.getHost().equals(mHost)) {
                    Log.i(TAG, "In safe page.");
                    // This is my web site, so do not override; let my WebView load the page
                    return false;
                }
                Log.i(TAG, "In foreign page.");
                Intent result = new Intent();
                result.putExtra("url", url);
                setResult(RESULT_OK, result);
                finish();
                return false;
            }
        }
        WebView grantAccessWebView = (WebView) findViewById(R.id.webview);

        grantAccessWebView.setWebViewClient(new GrantAccessWebView());

        WebSettings webSettings = grantAccessWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // we are reloading the page we already received in Login process, but it shouldn't matter
        String cookie_value = getIntent().getStringExtra("cookie_value");
        String cookie_url = getIntent().getStringExtra("cookie_url");
        Log.i(TAG, "Setting cookie:" + cookie_url + " value:" + cookie_value);
        CookieManager cm = CookieManager.getInstance();
        cm.removeAllCookie();
        cm.setAcceptCookie(true);
        cm.setCookie(cookie_url, "JSESSIONID="+cookie_value);
        Log.i(TAG, "We have a cookie: "+ cm.getCookie(cookie_url));
        grantAccessWebView.loadUrl(mUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.approval, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
