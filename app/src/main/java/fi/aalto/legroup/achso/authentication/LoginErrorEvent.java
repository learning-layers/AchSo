package fi.aalto.legroup.achso.authentication;

/**
 * @author Leo Nikkilä
 */
public class LoginErrorEvent {

    private String message;

    public LoginErrorEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
