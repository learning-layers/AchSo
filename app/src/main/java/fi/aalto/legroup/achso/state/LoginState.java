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

package fi.aalto.legroup.achso.state;

import android.app.Activity;

/**
 * Created by purma on 28.4.2014.
 */
public interface LoginState {
    int LOGGED_OUT = 0;
    int TRYING_TO_LOG_IN = 1;
    int LOGGED_IN = 2;
    String LOGIN_SUCCESS = "fi.aalto.legroup.achso.login_success";
    String LOGIN_FAILED = "fi.aalto.legroup.achso.login_failed";

    String getPublicUrl();

    boolean isIn();

    boolean isOut();

    boolean isTrying();

    String getUser();

    String getAuthToken();

    int getState();

    void logout();

    /**
     * Starts the login procedure, which may include getting login information from
     * AccountManager or launching some kind of login screen.
     * @param host_activity - activity that is starting the login procedure
     */
    void launchLoginActivity(Activity host_activity);

    /**
     * Check if the conditions for automatic login are fulfilled and try to login if they are.
     * @param host_activity
     */
    void autologinIfAllowed(Activity host_activity);

    /**
     * Do login with given credentials. Doesn't try to receive credentials from any other sources
     * or deal with AccountManager.
     * @param user - username
     * @param pass - password
     */
    void login(String user, String pass);

    /**
     * This gets called when there is authentication step that requires ApprovalActivity and its
     * webview (e.g. for approving that openid identity can be used for another service). When
     * ApprovalActivity is finished, it calls this with an url for the next step.
     * It is up to the LoginState implementation what is done with this url.
     * @param next_url url returned from ApprovalActivity. Can be useful.
     */
    void resumeAuthentication(String next_url);
}
