package fi.aalto.legroup.achso.upload.video;

import android.accounts.Account;
import android.os.Handler;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.networking.CountingRequestBody;
import fi.aalto.legroup.achso.upload.Uploader;
import fi.aalto.legroup.achso.util.App;

/**
 * A new video uploader using the Tethys services.
 */
public class ClViTra2VideoUploader extends Uploader {

    private String endpointUrl;

    public ClViTra2VideoUploader(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    /**
     * Uploads the video. Must call uploadListener.onVideoUploadStart() when the upload starts,
     * onVideoUploadProgress() when the upload progresses, onVideoUploadFinish() when done and
     * onVideoUploadError() if an error occurs.
     *
     * @param video the video that will be uploaded
     */
    @Override
    public void upload(SemanticVideo video) {
        File videoFile = new File(video.getUri().getPath());

        // Get the file's extension and build a new file name with it and the video's key

        int extensionPlace = videoFile.getName().lastIndexOf('.');
        String extension = "";

        if (extensionPlace > 0) {
            extension = videoFile.getName().substring(extensionPlace);
        }

        String fileName = video.getKey() + extension;

        // Resolve the mime type of the video file to include it in the request
        String mimeType = URLConnection.guessContentTypeFromName(videoFile.getPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        MediaType mediaType = MediaType.parse(mimeType);

        // Build a multi-part request body with the file
        RequestBody body = new MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addPart(
                        Headers.of(
                                "Content-Disposition",
                                "form-data; name=\"file\"; filename=\"" + fileName + "\""
                        ),
                        RequestBody.create(mediaType, videoFile)
                )
                .build();

        // Wraps the request body so that we can keep track of the upload progress
        CountingRequestBody countingBody = new CountingRequestBody(body);

        Request request = new Request.Builder()
                .url(endpointUrl + "upload")
                .post(countingBody)
                .build();

        // Start polling for the progress
        ProgressPollingTask progressTask = new ProgressPollingTask(video, countingBody);
        progressTask.run();

        listener.onUploadStart(video);

        try {
            Account account = App.loginManager.getAccount();
            Response response = App.authenticatedHttpClient.execute(request, account);

            if (response.isSuccessful()) {
                listener.onUploadFinish(video);
            } else {
                listener.onUploadError(video, "Could not upload video: " +
                        response.code() + " " + response.message());
            }
        } catch (IOException e) {
            listener.onUploadError(video, "Could not upload video: " + e.getMessage());
            e.printStackTrace();
        } finally {
            progressTask.stop();
        }
    }

    /**
     * Task for polling the upload progress.
     */
    protected class ProgressPollingTask implements Runnable {

        protected final static int POLLING_INTERVAL = 500;
        protected Handler handler = new Handler(App.getContext().getMainLooper());

        protected SemanticVideo video;
        protected CountingRequestBody body;
        protected boolean shouldStop = false;

        public ProgressPollingTask(SemanticVideo video, CountingRequestBody body) {
            this.video = video;
            this.body = body;
        }

        @Override
        public void run() {
            if (shouldStop) return;

            float percentage = 100f * body.getCount() / body.contentLength();

            // Limit the percentage values to ones that are usually expected. We might get some
            // unusual values depending on how the RequestBody was implemented.
            if (percentage > 100) percentage = 100;
            if (percentage < 0) percentage = 0;

            listener.onUploadProgress(video, (int) percentage);

            handler.postDelayed(this, POLLING_INTERVAL);
        }

        public void stop() {
            shouldStop = true;
        }

    }

}
