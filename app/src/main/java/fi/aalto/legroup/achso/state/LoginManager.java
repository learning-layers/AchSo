package fi.aalto.legroup.achso.state;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.io.IOException;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.authenticator.Authenticator;
import fi.aalto.legroup.achso.entities.User;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.OIDCConfig;

/**
 * Performs login routines and manages the login state.
 */
public class LoginManager {

    private final User defaultUser;

    public enum LoginState {
        LOGGED_OUT,
        LOGGING_IN,
        LOGGED_IN,
        LOGGING_OUT
    }

    // Keys for storing the auto-login preferences
    protected static final String PREFS_NAME = "AchSoLoginManagerPrefs";
    protected static final String PREFS_AUTO_LOGIN_ACCOUNT = "autoLoginAccount";

    protected Context context;
    protected Bus bus;
    protected Account account;
    protected JsonObject userInfo;
    protected User user;

    private LoginState state = LoginState.LOGGED_OUT;

    public LoginManager(Context context, Bus bus) {
        this.context = context;
        this.bus = bus;
        this.defaultUser = new User(context.getString(R.string.author_is_unknown), Uri.EMPTY);

        bus.register(this);
    }

    /**
     * Tries to log in automatically using stored account information. If no information is stored,
     * this will do nothing.
     */
    public void login() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Get auto-login preferences, abort if none in store
        String accountName = prefs.getString(PREFS_AUTO_LOGIN_ACCOUNT, null);

        if (accountName == null) return;

        AccountManager accountManager = AccountManager.get(context);
        String accountType = Authenticator.ACH_SO_ACCOUNT_TYPE;
        Account[] availableAccounts = accountManager.getAccountsByType(accountType);

        // Find the stored account and use it to log in
        for (Account availableAccount : availableAccounts) {
            if (availableAccount.name.equals(accountName)) {
                login(availableAccount);
            }
        }
    }

    /**
     * Logs in using a specified account.
     *
     * @param account account used to log in with
     */
    public void login(Account account) {
        this.account = account;
        new LoginTask().execute(account);
    }

    /**
     * Logs out from the account and disables auto-login. Use this if the user manually logs out.
     */
    public void logoutExplicitly() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();

        prefsEditor.remove(PREFS_AUTO_LOGIN_ACCOUNT);
        prefsEditor.apply();

        logout();
    }

    /**
     * Logs out from the account but does not disable auto-login. Use this if the action is
     * automatic (e.g. connectivity lost) and not initiated by the user.
     */
    public void logout() {
        setState(LoginState.LOGGED_OUT);
    }

    public LoginState getState() {
        return state;
    }

    public Account getAccount() {
        return account;
    }

    public JsonObject getUserInfo() { return userInfo; }

    public User getUser() {
        if (user == null) {
            return defaultUser;
        }

        return user;
    }

    public boolean isLoggedIn() {
        return state == LoginState.LOGGED_IN || isLoggingOut();
    }

    public boolean isLoggingIn() {
        return state == LoginState.LOGGING_IN;
    }

    public boolean isLoggedOut() {
        return state == LoginState.LOGGED_OUT || isLoggingIn();
    }

    public boolean isLoggingOut() {
        return state == LoginState.LOGGING_OUT;
    }

    /**
     * Sets and broadcasts the login state.
     *
     * @param state the new login state
     */
    protected void setState(LoginState state) {
        this.state = state;

        bus.post(produceLoginState());
    }

    @Produce
    public LoginStateEvent produceLoginState() {
        return new LoginStateEvent(state);
    }

    @Subscribe
    public void onLoginRequest(LoginRequestEvent event) {
        switch (event.getType()) {
            case LOGIN:
                login();
                break;

            case LOGOUT:
                logout();
                break;

            case EXPLICIT_LOGOUT:
                logoutExplicitly();
                break;
        }
    }

    /**
     * This task checks if authentication works by fetching user information for the account.
     * We'll save the information, since it will come handy after login.
     */
    private class LoginTask extends AsyncTask<Account, Void, String> {

        /**
         * Fetches user information.
         *
         * @return null if successful, error message if one occurs
         */
        @Override
        protected String doInBackground(Account... accounts) {
            String userInfoUrl = OIDCConfig.getUserInfoUrl(context);
            Account account = accounts[0];

            Request request = new Request.Builder()
                    .url(userInfoUrl)
                    .header("Accept", "application/json")
                    .get()
                    .build();

            try {
                Response response = App.authenticatedHttpClient.execute(request, account);

                if (response.isSuccessful()) {
                    String body = response.body().string();

                    userInfo = new JsonParser().parse(body).getAsJsonObject();

                    // TODO: Provide a real URI
                    user = new User(userInfo.get("name").getAsString(), Uri.EMPTY);
                } else {
                    return "Couldn't fetch user info: " +
                            response.code() + " " + response.message();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Couldn't fetch user info: " + e.getMessage();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            setState(LoginState.LOGGING_IN);
        }

        @Override
        protected void onPostExecute(String error) {
            if (error != null) {
                bus.post(new LoginErrorEvent(error));
                setState(LoginState.LOGGED_OUT);
                return;
            }

            // Remember this account for auto-login
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor prefsEditor = prefs.edit();

            prefsEditor.putString(PREFS_AUTO_LOGIN_ACCOUNT, account.name);
            prefsEditor.apply();

            setState(LoginState.LOGGED_IN);
        }

    }

}
