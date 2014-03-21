/**
 * Copyright 2013 Aalto university, see AUTHORS
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.util.App;

public class LoginActivity extends ActionbarActivity {

    private final Context ctx = this;
    private EditText mUserName;
    private EditText mPassword;
    private Button mLoginButton;
    private CheckBox mAutoLoginCheckBox;
    private View.OnClickListener mLoginButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CharSequence username_cs = mUserName.getText();
            CharSequence password_cs = mPassword.getText();
            String username = "";
            String password = "";
            if (username_cs != null) {
                username = username_cs.toString();
            }
            if (password_cs != null) {
                password = password_cs.toString();
            }

            if (App.login_state.login(username, password)) {
                setResult(RESULT_OK);
                finish();
            } else {
                new AlertDialog.Builder(ctx)
                        .setTitle(ctx.getResources().getString(R.string.login))
                        .setMessage(ctx.getResources().getString(R.string.login_nag_text))
                        .setPositiveButton(R.string.ok, null)
                        .create().show();
            }
        }
    };
    private View.OnClickListener mAutoLoginCheckBoxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean checked = ((CheckBox) v).isChecked();
            SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
            Editor edit = prefs.edit();
            edit.putBoolean("autologin", checked);
            edit.apply();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i("LoginActivity", "Inflating options menu - LoginActivity");
        mMenu = menu;
        App.login_state.setHostActivity(this);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.main_menubar, menu);
        // Remove search as it does not make sense here
        menu.removeItem(R.id.action_search);
        updateLoginMenuItem();
        return true;
    }

    @Override
    public void updateLoginMenuItem() {
        // Remove login/logout options from ActionBar
        // to prevent cyclic starting of this activity
        MenuItem loginItem = mMenu.findItem(R.id.action_login);
        MenuItem logoutItem = mMenu.findItem(R.id.action_logout);
        MenuItem loadingItem = mMenu.findItem(R.id.menu_refresh);
        MenuItem offlineItem = mMenu.findItem(R.id.action_offline);

        if (mMenu == null || loginItem == null || logoutItem == null || loadingItem == null || offlineItem == null) {
            Log.i("LoginBarActivity", "Skipping icon update -- they are not present. Menu size:" + mMenu.size());
        } else {
            loginItem.setVisible(false);
            logoutItem.setVisible(false);
            loadingItem.setVisible(false);
            offlineItem.setVisible(false);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
        boolean autologin = prefs.getBoolean("autologin", false);

        mUserName = (EditText) findViewById(R.id.username_field);
        mPassword = (EditText) findViewById(R.id.password_field);

        if (autologin) {
            mUserName.setText(prefs.getString("login", ""));
            mPassword.setText(prefs.getString("pwd", ""));
        }
        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(mLoginButtonClickListener);

        mAutoLoginCheckBox = (CheckBox) findViewById(R.id.autologin_checkbox);
        mAutoLoginCheckBox.setOnClickListener(mAutoLoginCheckBoxListener);
        mAutoLoginCheckBox.setChecked(autologin);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
