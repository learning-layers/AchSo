package fi.aalto.legroup.achso.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

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

    private void doLogin() {
        AccountManager accountManager = AccountManager.get(this);
        String accountType = Authenticator.ACH_SO_ACCOUNT_TYPE;

        final Account availableAccounts[] = accountManager.getAccountsByType(accountType);

        switch (availableAccounts.length) {
            // No account has been created, let's create one now
            case 0:
                accountManager.addAccount(accountType, Authenticator.TOKEN_TYPE_ID, null, null,
                        this, new AccountManagerCallback<Bundle>() {
                            @Override
                            public void run(AccountManagerFuture<Bundle> futureManager) {
                                // Unless the account creation was cancelled, try logging in again
                                // after the account has been created.
                                if (futureManager.isCancelled()) {
                                    finish();
                                    return;
                                }

                                doLogin();
                            }
                        }, null);
                break;

            // There's just one account, let's use that
            case 1:
                App.loginManager.login(availableAccounts[0]);
                finish();
                break;

            // Multiple accounts, let the user pick one
            default:
                String name[] = new String[availableAccounts.length];

                for (int i = 0; i < availableAccounts.length; i++) {
                    name[i] = availableAccounts[i].name;
                }

                new AlertDialog.Builder(this)
                        .setTitle(R.string.choose_account)
                        .setAdapter(new ArrayAdapter<>(this,
                                        android.R.layout.simple_list_item_1, name),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int selectedAccount) {
                                        App.loginManager.login(availableAccounts[selectedAccount]);
                                        finish();
                                    }
                                })
                        .create()
                        .show();
        }
    }

}
