package fi.aalto.legroup.achso.authentication;

public class OIDCNotReadyException extends Exception {
    public OIDCNotReadyException() {
        super("OpenID Connect is not ready");
    }
}
