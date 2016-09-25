package fi.aalto.legroup.achso.entities;

import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;

public class Group implements JsonSerializable {
    protected int id;
    protected String name;
    protected List<UUID> videos;

    public String getName() {
        return name;
    }
    public List<UUID> getVideos() {
        return videos;
    }

    public int getId() { return id;}

    public void setId(int id) { this.id = id; }
    public void setName(String name) {
        this.name = name;
    }
    public void setVideos(List<UUID> videos) {
        this.videos = videos;
    }
    public  boolean hasVideo(UUID id) {
        return this.videos.contains(id);
    }

    public void removeVideoFromGroup(UUID videoID) {
        this.videos.remove(videoID);
    }

    public void addVideoToGroup(UUID videoID) {
        if (!this.videos.contains(videoID)) {
            this.videos.add(videoID);
        }
    }
}
