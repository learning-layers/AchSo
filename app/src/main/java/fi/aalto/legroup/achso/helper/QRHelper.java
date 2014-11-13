package fi.aalto.legroup.achso.helper;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.view.ActionMode;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;

/**
 * Created by lassi on 7.11.14.
 */
public class QRHelper {
    private static boolean launchedForAdding;
    private static ActionMode mode;
    private static List<SemanticVideo> videos;
    private static MenuItem menuItem;

    public static boolean launchedForAdding() {
        return launchedForAdding;
    }

    public static void readQRCodeForVideos(Activity activity, List<SemanticVideo> selectedVideos, ActionMode actionMode) {
        videos = selectedVideos;
        mode = actionMode;
        launchedForAdding = true;
        readQRCodeValue(activity);
    }

    public static void readQRCodeForSearching(Activity activity, MenuItem searchItem) {
        launchedForAdding = false;
        menuItem = searchItem;
        readQRCodeValue(activity);
    }

    private static void readQRCodeValue(Activity activity) {
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(activity.getString(R.string.read_qr_code));
        integrator.setScanningRectangle(400, 400);
        integrator.initiateScan();
    }

    public static void readQRCodeResult(Activity activity, int requestCode, int resultCode, Intent data) {

        String code = getQRCodeForResult(requestCode, resultCode, data);
        if (launchedForAdding) {
            VideoDBHelper db = new VideoDBHelper(activity.getApplicationContext());

            for (SemanticVideo video : videos) {
                video.setQrCode(code);
                db.update(video);
            }

            Toast toast = Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.code_added_to_videos), Toast.LENGTH_SHORT);
            toast.show();
            mode.finish();
        } else {
            SearchView search = (SearchView)menuItem.getActionView();
            search.setQuery(code, true);
            MenuItemCompat.expandActionView(menuItem);
        }
    }

    private static String getQRCodeForResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            return result.getContents();
        }

        return null;
    }
}
