package fi.aalto.legroup.achso.util;

import java.util.List;

import fi.aalto.legroup.achso.database.SemanticVideo;

/**
 * Created by purma on 28.4.2014.
 */
public interface Connection {
    List<SemanticVideo> getVideos(int query_type, String query);
}
