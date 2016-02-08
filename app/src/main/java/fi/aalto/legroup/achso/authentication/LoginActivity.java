package fi.aalto.legroup.achso.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;

/**
 * TODO: This should be a fragment or something.
 */
public final class LoginActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (App.isConnected()) {
            doLogin();
        } else {
            Toast.makeText(this, R.string.connection_problem, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void createAccount() {
        final AccountManager accountManager = AccountManager.get(this);
        String accountType = Authenticator.ACH_SO_ACCOUNT_TYPE;

        accountManager.addAccount(accountType, Authenticator.TOKEN_TYPE_ID, null, null,
                this, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> futureManager) {
                        // Give up if the account creation was cancelled.
                        if (futureManager.isCancelled()) {
                            finish();
                            return;
                        }

                        // Try to log in with the account that was returned in the bundle.
                        try {
                            Bundle result = futureManager.getResult();
                            String accountType = Authenticator.ACH_SO_ACCOUNT_TYPE;
                            String accountName = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                            if (accountName != null) {
                                final Account availableAccounts[] = accountManager.getAccountsByType(accountType);
                                for (Account account : availableAccounts) {
                                    if (accountName.equals(account.name)) {
                                        finish();
                                        App.loginManager.login(account);
                                        return;
                                    }
                                }
                            }
                        } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                            e.printStackTrace();
                        }

                        // Present the login screen again if it failed.
                        doLogin();
                    }
                }, null);
    }

    private void doLogin() {
        AccountManager accountManager = AccountManager.get(this);
        String accountType = Authenticator.ACH_SO_ACCOUNT_TYPE;

        final Account availableAccounts[] = accountManager.getAccountsByType(accountType);

        if (availableAccounts.length > 0) {

            String name[] = new String[availableAccounts.length + 1];
            for (int i = 0; i < availableAccounts.length; i++) {
                name[i] = availableAccounts[i].name;
            }
            name[availableAccounts.length] = getString(R.string.login_with_another);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.choose_account)
                    .setAdapter(new ArrayAdapter<>(this,
                                    android.R.layout.simple_list_item_1, name),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int selectedAccount) {
                                    if (selectedAccount < availableAccounts.length) {
                                        App.loginManager.login(availableAccounts[selectedAccount]);
                                        finish();
                                    } else {
                                        createAccount();
                                    }

                                }
                            })
                    .create()
                    .show();
        } else {
            // Always present the login screen when there is no account
            createAccount();
        }
    }

}
