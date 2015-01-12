package fi.aalto.legroup.achso.entities;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;

/**
 * An annotation entity that describes a video's annotation.
 *
 * @author Leo Nikkilä
 */
public class Annotation implements JsonSerializable, Parcelable {

    protected long time;
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

    protected Annotation(Parcel parcel) {
        this.time = parcel.readLong();
        this.position = (PointF) parcel.readValue(PointF.class.getClassLoader());
        this.text = parcel.readString();
        this.author = (User) parcel.readValue(User.class.getClassLoader());
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(this.time);
        parcel.writeValue(this.position);
        parcel.writeString(this.text);
        parcel.writeValue(this.author);
    }

    public static final Creator<Annotation> CREATOR = new Creator<Annotation>() {

        @Override
        public Annotation createFromParcel(Parcel parcel) {
            return new Annotation(parcel);
        }

        @Override
        public Annotation[] newArray(int size) {
            return new Annotation[size];
        }

    };

}
