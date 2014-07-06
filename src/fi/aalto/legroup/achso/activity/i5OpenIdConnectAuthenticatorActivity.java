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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.service.i5OpenIdConnectAccountService;
import fi.aalto.legroup.achso.state.i5OpenIdConnectLoginState;
import fi.aalto.legroup.achso.util.App;

/**
 * A login screen that offers login via email/password.

 */
public class i5OpenIdConnectAuthenticatorActivity extends AccountAuthenticatorActivity implements
        LoaderCallbacks<Cursor>{

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    // UI references.
    private AutoCompleteTextView mFullnameView;
    private AutoCompleteTextView mEmailView;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mPasswordConfirmView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView mRegisterModeLink;
    private TextView mLoginModeLink;
    private Button mLoginButton;
    private Button mRegisterButton;
    private AccountManager mAccountManager;

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    public final static String PARAM_USER_PASS = "USER_PASS";
    private static final String PARAM_USER_FULLNAME = "USER_FULLNAME";
    private static final String PARAM_USER_EMAIL = "USER_EMAIL";
    private boolean mNewAccount;
    private boolean mRegisterMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);

        // Set up the login form.
        mAccountManager = AccountManager.get(App.getContext());
        mFullnameView = (AutoCompleteTextView) findViewById(R.id.fullname);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();
        mUsernameView = (EditText) findViewById(R.id.login_name);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordConfirmView = (EditText) findViewById(R.id.password_confirm);
        Log.i("AuthenticatorActivity", "passwordview:" + mPasswordView);
        mNewAccount = getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false);
        mRegisterMode = mNewAccount;
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    //attemptLogin();
                    login();
                    return true;
                }
                return false;
            }
        });
        mRegisterModeLink = (TextView) findViewById(R.id.register_mode);
        mRegisterModeLink.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRegisterMode(true);
            }
        });
        mLoginModeLink = (TextView) findViewById(R.id.login_mode);
        mLoginModeLink.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRegisterMode(false);
            }
        });

        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        mRegisterButton = (Button) findViewById(R.id.create_account_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });
        toggleRegisterMode(mRegisterMode);

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Switch between register / login mode
     * @param register_mode true = switch to registering, false = switch to login.
     */
    private void toggleRegisterMode(boolean register_mode) {
        if (register_mode) {
            mRegisterMode = true;
            mRegisterModeLink.setVisibility(View.GONE);
            mLoginButton.setVisibility(View.GONE);
            mLoginModeLink.setVisibility(View.VISIBLE);
            mRegisterButton.setVisibility(View.VISIBLE);
            mFullnameView.setVisibility(View.VISIBLE);
            mPasswordConfirmView.setVisibility(View.VISIBLE);
            mEmailView.setVisibility(View.VISIBLE);

        } else {
            mRegisterMode = false;
            mRegisterModeLink.setVisibility(View.VISIBLE);
            mLoginButton.setVisibility(View.VISIBLE);
            mLoginModeLink.setVisibility(View.GONE);
            mRegisterButton.setVisibility(View.GONE);
            mFullnameView.setVisibility(View.GONE);
            mPasswordConfirmView.setVisibility(View.GONE);
            mEmailView.setVisibility(View.GONE);
        }
    }


    private void register() {
        final String fullname = mFullnameView.getText().toString();
        final String email = mEmailView.getText().toString();
        final String username = mUsernameView.getText().toString();
        final String pass = mPasswordView.getText().toString();
        final String pass_confirm = mPasswordConfirmView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(pass) && !isPasswordValid(pass)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        if (!pass.equals(pass_confirm)) {
            mPasswordConfirmView.setError(getString(R.string.error_passwords_dont_match));
            focusView = mPasswordConfirmView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            return;
        }
        // do register
        new AsyncTask<Void, Void, Intent>() {
            @Override
            protected Intent doInBackground(Void... params) {
                String authtoken = i5OpenIdConnectLoginState.userRegister(fullname, email, username, pass);
                final Intent res = new Intent();
                res.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
                res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, i5OpenIdConnectAccountService.ACHSO_ACCOUNT_TYPE);
                res.putExtra(AccountManager.KEY_AUTHTOKEN, authtoken);
                res.putExtra(PARAM_USER_PASS, pass);
                res.putExtra(PARAM_USER_FULLNAME, fullname);
                res.putExtra(PARAM_USER_EMAIL, email);
                return res;
            }
            @Override
            protected void onPostExecute(Intent intent) {
                finishRegister(intent);
            }
        }.execute();
    }

    private void populateAutoComplete() {
        if (VERSION.SDK_INT >= 14) {
            // Use ContactsContract.Profile (API 14+)
            getLoaderManager().initLoader(0, null, this);
        } else if (VERSION.SDK_INT >= 8) {
            // Use AccountManager (API 8+)
            new SetupEmailAutoCompleteTask().execute(null, null);
        }
    }

    private void finishRegister(Intent intent) {
        String username = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String password = intent.getStringExtra(PARAM_USER_PASS);
        String account_type = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        String auth_token = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String auth_token_type = i5OpenIdConnectAccountService.ACHSO_AUTH_TOKEN_TYPE;
        final Account account = new Account(username, account_type);

        // Creating the account on the device and setting the auth token we got
        // (Not setting the auth token will cause another call to the server to authenticate the user)
        mAccountManager.addAccountExplicitly(account, password, null);
        mAccountManager.setAuthToken(account, auth_token_type, auth_token);
        Log.i("AuthenticatorActivity", "Registering a new account "+ account + " setting " +
                "auth_token to " +
                auth_token);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        Log.i("AuthenticatorActivity", "Finishing AuthenticatorActivity");
        finish();
    }


    public void login() {
        final String user_name = mUsernameView.getText().toString();
        final String user_pass = mPasswordView.getText().toString();
        new AsyncTask<Void, Void, Intent>() {
            @Override
            protected Intent doInBackground(Void... params) {
                String authtoken = i5OpenIdConnectLoginState.userSignIn(user_name, user_pass);
                final Intent res = new Intent();
                res.putExtra(AccountManager.KEY_ACCOUNT_NAME, user_name);
                res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, i5OpenIdConnectAccountService.ACHSO_ACCOUNT_TYPE);
                res.putExtra(AccountManager.KEY_AUTHTOKEN, authtoken);
                res.putExtra(PARAM_USER_PASS, user_pass);
                return res;
            }
            @Override
            protected void onPostExecute(Intent intent) {
                finishLogin(intent);
            }
        }.execute();
    }

    private void finishLogin(Intent intent) {
        String username = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String password = intent.getStringExtra(PARAM_USER_PASS);
        String account_type = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        String auth_token = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String auth_token_type = i5OpenIdConnectAccountService.ACHSO_AUTH_TOKEN_TYPE;
        final Account account = new Account(username, account_type);

        if (mNewAccount) {
            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, password, null);
            mAccountManager.setAuthToken(account, auth_token_type, auth_token);
            Log.i("AuthenticatorActivity", "Adding an account "+ account + " setting authtoken to " +
                    auth_token);
        } else {
            mAccountManager.setPassword(account, password);
            mAccountManager.setAuthToken(account, auth_token_type, auth_token);
            Log.i("AuthenticatorActivity", "Modifying an account " + password);
        }
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        Log.i("AuthenticatorActivity", "Finishing AuthenticatorActivity");
        finish();
    }
    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    /**
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }
    */
    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                                                                     .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Use an AsyncTask to fetch the user's email addresses on a background thread, and update
     * the email text field with results on the main UI thread.
     */
    class SetupEmailAutoCompleteTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... voids) {
            ArrayList<String> emailAddressCollection = new ArrayList<String>();

            // Get all emails from the user's contacts and copy them to a list.
            ContentResolver cr = getContentResolver();
            Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    null, null, null);
            while (emailCur.moveToNext()) {
                String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract
                        .CommonDataKinds.Email.DATA));
                emailAddressCollection.add(email);
            }
            emailCur.close();

            return emailAddressCollection;
        }

	    @Override
	    protected void onPostExecute(List<String> emailAddressCollection) {
	       addEmailsToAutoComplete(emailAddressCollection);
	    }
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(i5OpenIdConnectAuthenticatorActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

}

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    /*
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
**/


