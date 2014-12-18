package fi.aalto.legroup.achso.entities;

import android.graphics.PointF;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;

/**
 * An annotation entity that describes a video's annotation.
 *
 * @author Leo Nikkilä
 */
public class Annotation implements JsonSerializable {

    protected Long time;
    protected PointF position;
    protected String text;
    protected User author;

    @SuppressWarnings("UnusedDeclaration")
    private Annotation() {
        // For serialization
    }

    public Annotation(long time, PointF position, String text, User author) {
        this.time = time;
        this.position = position;
        this.text = text;
        this.author = author;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public PointF getPosition() {
        return this.position;
    }

    public void setPosition(PointF position) {
        if (position.x > 1) position.x = 1;
        if (position.x < 0) position.x = 0;

        if (position.y > 1) position.y = 1;
        if (position.y < 0) position.y = 0;

        this.position = position;
    }

    public String getText() {
        if (this.text == null) {
            this.text = "";
        }

        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

}
