package fi.aalto.legroup.achso.annotation;

import com.google.gson.JsonObject;

import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.FloatPosition;

/**
 * TODO: Not really a factory. Move this when starting work with the persistence layer.
 *
 * @author Leo Nikkilä
 */
public class AnnotationFactory {

    public Annotation create(SemanticVideo video, long time, FloatPosition position) {
        long videoId = video.getId();

        JsonObject userInfo = App.loginManager.getUserInfo();
        String creator = null;

        if (userInfo != null && userInfo.has("preferred_username")) {
            creator = userInfo.get("preferred_username").getAsString();
        }

        // FIXME: This null has to be replaced with a proper key if we're annotating a remote video
        return new Annotation(videoId, time, "", position, 1.0f, creator, null);
    }

}
