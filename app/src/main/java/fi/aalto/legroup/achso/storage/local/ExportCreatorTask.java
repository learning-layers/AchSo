package fi.aalto.legroup.achso.storage.local;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.VideoInfo;

/**
 * TODO: Extract view stuff into the calling activity/fragment and use events.
 * TODO: Use ProgressDialogFragment.
 *
 * Created by lassi on 8.12.14.
 */
public class ExportCreatorTask extends AsyncTask<UUID, Integer, List<Uri>> {

    private ProgressDialog dialog;
    private Context context;

    public ExportCreatorTask(Context context) {
        super();
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        String message = this.context.getString(R.string.processing_video);

        this.dialog = new ProgressDialog(this.context);
        this.dialog.setMessage(message);
        this.dialog.setIndeterminate(true);
        this.dialog.setCanceledOnTouchOutside(false);
        this.dialog.show();
    }

    @Override
    protected List<Uri> doInBackground(UUID... uuids) {

        // TODO: Make me async

        byte[] buffer = new byte[1024];
        List<Uri> uris = new ArrayList<>();

        for (int i = 0; i < uuids.length; i++) {
            //this.dialog.setMessage(appContext.getString(R.string.compressing_body, i, uuids.length));
            try {
                UUID id = uuids[i];
                VideoInfo video = App.videoInfoRepository.get(id);
                File zip = new File(Environment.getExternalStorageDirectory() + "/" + video.getTitle() + ".achso");
                FileOutputStream fos = new FileOutputStream(zip);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ZipOutputStream zos = new ZipOutputStream(bos);

                try {
                    InputStream fis;
                    zos.putNextEntry(new ZipEntry(video.getId().toString() + ".mp4"));
                    Uri uri = video.getVideoUri();
                    if(video.isRemote())  {
                        fis = new URL(uri.toString()).openStream();
                    } else {
                        fis = context.getContentResolver().openInputStream(uri);
                    }

                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    fis.close();

                    zos.putNextEntry(new ZipEntry(video.getId().toString() + ".jpg"));
                    fis = new FileInputStream(video.getThumbUri().getPath());
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    fis.close();

                    zos.putNextEntry(new ZipEntry(video.getId().toString() + ".json"));
                    fis = new FileInputStream(App.videoInfoRepository.getManifestFromId(video.getId()));
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    fis.close();
                } finally {
                    zos.close();
                }

                fos.close();
                uris.add(Uri.fromFile(zip));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uris;
    }

    @Override
    protected void onPostExecute(List<Uri> uris) {
        this.dialog.dismiss();
        App.bus.post(new ExportCreatorTaskResultEvent(uris));
        super.onPostExecute(uris);
    }
}
