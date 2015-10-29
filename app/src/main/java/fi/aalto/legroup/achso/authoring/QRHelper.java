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
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.OptimizedVideo;

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

    public static void readQRCodeResult(Activity activity, int requestCode, int resultCode, Intent data) {

        String code = getQRCodeForResult(requestCode, resultCode, data);
        if (launchedForAdding) {
            for (UUID id : ids) {
                try {
                    OptimizedVideo video = App.videoRepository.getVideo(id);
                    video.setTag(code);
                    video.inflate().save();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            SnackbarManager.show(Snackbar.with(activity).text(R.string.code_added_to_videos));

            if (mode != null) {
                mode.finish();
            }
        } else {
            SearchView search = (SearchView) menuItem.getActionView();
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
