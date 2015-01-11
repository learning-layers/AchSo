package fi.aalto.legroup.achso.state;

/**
 * TODO: Make this more specific.
 *
 * @author Leo Nikkilä
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
