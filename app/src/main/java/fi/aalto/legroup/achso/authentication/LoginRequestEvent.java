package fi.aalto.legroup.achso.authentication;

/**
 * @author Leo Nikkilä
 */
public class LoginRequestEvent {

    private Type type;

    public LoginRequestEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static enum Type {
        LOGIN,
        LOGOUT,
        EXPLICIT_LOGOUT
    }

}
