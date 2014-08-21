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
package fi.aalto.legroup.achso.service;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;



/**
 * Created by purma on 23.6.2014.
 *
 */
public class i5OpenIdConnectAccountService extends Service {

    public static final String ACCOUNT_NAME = "sync";
    public static final String ACHSO_ACCOUNT_TYPE = "fi.aalto.legroup.achso.i5account";
    public static final String ACHSO_AUTH_TOKEN_TYPE = "i5cloud";
    private static final String TAG = "i5CloudAccountService";
    private i5OpenIdConnectAuthenticator mAuthenticator;
    private Context mContext;

    /**
     * Obtain a handle to the {@link android.accounts.Account} used for sync in this application.
     *
     * @return Handle to application's account (not guaranteed to resolve unless CreateSyncAccount()
     * has been called)
     */
    public static Account GetAccount() {
        // Note: Normally the account name is set to the user's identity (username or email
        // address). However, since we aren't actually using any user accounts, it makes more sense
        // to use a generic string in this case.
        //
        // This string should *not* be localized. If the user switches locale, we would not be
        // able to locate the old account, and may erroneously register multiple accounts.
        final String accountName = ACCOUNT_NAME;
        return new Account(accountName, ACHSO_ACCOUNT_TYPE);
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "AccountService created");
        mAuthenticator = new i5OpenIdConnectAuthenticator(this);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }


    // Authenticator implementation is based on
    // https://udinic.wordpress.com/2013/04/24/write-your-own-android-authenticator/

}



