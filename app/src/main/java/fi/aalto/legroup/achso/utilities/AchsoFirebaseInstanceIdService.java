package fi.aalto.legroup.achso.utilities;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import fi.aalto.legroup.achso.app.App;

public class AchsoFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        App.tokenUpdated(refreshedToken);

        super.onTokenRefresh();
    }
}
