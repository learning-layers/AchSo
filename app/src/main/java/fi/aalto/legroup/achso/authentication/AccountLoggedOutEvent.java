package fi.aalto.legroup.achso.authentication;

import android.accounts.Account;

/**
 * Created by mat on 19/04/2017.
 */

public class AccountLoggedOutEvent {
    Account account;

    public AccountLoggedOutEvent(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }
}
