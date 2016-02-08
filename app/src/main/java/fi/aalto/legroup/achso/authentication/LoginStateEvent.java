package fi.aalto.legroup.achso.authentication;

/**
 * TODO: Make this more specific.
 */
public class LoginStateEvent {

    private LoginManager.LoginState state;
    private boolean notifyUser;

    public LoginStateEvent(LoginManager.LoginState state, boolean notifyUser) {
        this.state = state;
        this.notifyUser = notifyUser;
    }

    public LoginManager.LoginState getState() {
        return state;
    }

    public boolean shouldNotifyUser() {
        return notifyUser;
    }

}
