package fi.aalto.legroup.achso.authentication;

public class LoginErrorEvent {

    private String message;

    public LoginErrorEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
