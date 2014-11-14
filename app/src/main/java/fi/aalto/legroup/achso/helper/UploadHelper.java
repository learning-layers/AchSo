package fi.aalto.legroup.achso.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ActionMode;
import android.widget.Toast;

import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.service.UploaderService;
import fi.aalto.legroup.achso.util.App;

/**
 * Created by lassi on 12.11.14.
 */
public class UploadHelper {

    public static void uploadVideos(Context context, List<SemanticVideo> videos, ActionMode mode) {

        // Fail if user is not logged in
        if (App.loginManager.isLoggedOut()) {
            String message = context.getString(R.string.not_loggedin_nag_text);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            return;
        }

        for (SemanticVideo sv : videos) {
            sv.setUploadStatus(SemanticVideo.UPLOAD_PENDING);
            Intent uploadIntent = new Intent(context, UploaderService.class);
            uploadIntent.putExtra(UploaderService.PARAM_IN, sv.getId());
            context.startService(uploadIntent);
        }

        mode.finish();
    }

}
