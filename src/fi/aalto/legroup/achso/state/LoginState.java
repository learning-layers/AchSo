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

    void autologinIfAllowed();

    void login(String user, String pass);
}
