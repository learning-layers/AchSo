package fi.aalto.legroup.achso.entities;

import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;

public class Group implements JsonSerializable {
    protected String name;
    protected List<UUID> videos;

    public String getName() {
        return name;
    }
    public List<UUID> getVideos() {
        return videos;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setVideos(List<UUID> videos) {
        this.videos = videos;
    }
}
