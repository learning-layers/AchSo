package fi.aalto.legroup.achso.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * The service that lets Android know about the custom Authenticator.
 *
 * @author Leo Nikkilä
 */
public class AuthenticatorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        Authenticator authenticator = new Authenticator(this);
        return authenticator.getIBinder();
    }

}
