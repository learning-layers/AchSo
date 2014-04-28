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

    int getState();

    void logout();

    void autologinIfAllowed();

    void login(String user, String pass);
}
