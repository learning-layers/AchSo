package fi.aalto.legroup.achso.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ActionMode;

import java.util.List;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.service.UploaderService;
import fi.aalto.legroup.achso.view.VideoGridItemView;

/**
 * Created by lassi on 12.11.14.
 */
public class UploadHelper {
    public static void uploadVideos(Activity activity, List<SemanticVideo> videos, ActionMode actionMode) {
        for (SemanticVideo sv : videos) {
            sv.setUploadStatus(SemanticVideo.UPLOAD_PENDING);
            Intent uploadIntent = new Intent(activity, UploaderService.class);
            uploadIntent.putExtra(UploaderService.PARAM_IN, sv.getId());
            activity.startService(uploadIntent);
        }

        actionMode.finish();
    }
}
