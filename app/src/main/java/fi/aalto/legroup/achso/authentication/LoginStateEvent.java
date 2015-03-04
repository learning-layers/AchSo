package fi.aalto.legroup.achso.authentication;

/**
 * TODO: Make this more specific.
 */
public class LoginStateEvent {

    private LoginManager.LoginState state;

    public LoginStateEvent(LoginManager.LoginState state) {
        this.state = state;
    }

    public LoginManager.LoginState getState() {
        return state;
    }

}
