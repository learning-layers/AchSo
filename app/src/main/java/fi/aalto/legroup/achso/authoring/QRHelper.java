package fi.aalto.legroup.achso.authoring;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.OptimizedVideo;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.storage.VideoRepository;

public class QRHelper {
    private static boolean launchedForAdding;
    private static ActionMode mode;
    private static List<UUID> ids;
    private static MenuItem menuItem;

    public static void readQRCodeForVideos(Activity activity, List<UUID> selection,
                                           ActionMode actionMode) {
        ids = selection;
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

    public static String getQRCodeForResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            return result.getContents();
        }

        return null;
    }
}
