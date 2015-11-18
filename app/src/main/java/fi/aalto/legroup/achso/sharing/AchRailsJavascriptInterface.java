package fi.aalto.legroup.achso.sharing;

import android.app.Activity;
import android.content.Intent;
import android.provider.ContactsContract;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class AchRailsJavascriptInterface {
    public static int CONTACT_PICK_CODE = 0xC0874C7;

    private final Activity activity;

    public AchRailsJavascriptInterface(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(activity, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void closeActivity() {
        activity.finish();
    }

    @JavascriptInterface
    public void openContactPicker() {
        if (activity instanceof SharingActivity) {
            SharingActivity sharingActivity = (SharingActivity) activity;

            if (!sharingActivity.hasUserGrantedContactAccess()) {

                sharingActivity.askForContactPermission();

            } else {
                startPickerIntent();
            }
        }
        //startPickerIntent();
    }

    private void startPickerIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        activity.startActivityForResult(intent, CONTACT_PICK_CODE);

    }
}
