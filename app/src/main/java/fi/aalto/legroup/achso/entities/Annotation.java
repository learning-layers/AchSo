package fi.aalto.legroup.achso.entities;

import android.graphics.Color;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;

import java.util.Date;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;

/**
 * An annotation entity that describes a video's annotation.
 */
public class Annotation implements JsonSerializable, Parcelable {

    private static final int FNV_32_INIT = 0x811c9dc5;
    private static final int FNV_32_PRIME = 0x01000193;

    // Taken from https://material.google.com/style/color.html#color-color-palette
    private static final int[] materialDesignPalette = {
            0xFFF44336,
            0xFFE91E63,
            0xFF9C27B0,
            0xFF673AB7,
            0xFF3F51B5,
            0xFF2196F3,
            0xFF03A9F4,
            0xFF00BCD4,
            0xFF009688,
            0xFF4CAF50,
            0xFF8BC34A,
            0xFFCDDC39,
            0xFFFFEB3B,
            0xFFFFC107,
            0xFFFF9800,
            0xFFFF5722,
            0xFF795548,
            0xFF9E9E9E,
            0xFF607D8B
    };

    protected long time;
    protected PointF position;
    protected String text;
    protected User author;
    protected Date createdTimestamp;

    Annotation() {
        // For serialization and pooling
        createdTimestamp = new Date();
    }

    public Annotation(long time, PointF position, String text, User author, Date createdTimestamp) {
        this.time = time;
        this.position = position;
        this.text = text;
        this.author = author;
        this.createdTimestamp = createdTimestamp;
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

    // Calculate random color from author name using FNV
    public int calculateColor() {
        String name  = this.author.getName();

        int rv = FNV_32_INIT;
        int len = name.length();

        for (int i = 0; i < len; i++) {
            rv ^= name.charAt(i);
            rv *= FNV_32_PRIME;
        }

        int index = rv % (materialDesignPalette.length - 1);

        if (index < 0) index *= -1;

        return materialDesignPalette[index];
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

    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Annotation)) {
            return false;
        }

        Annotation that = (Annotation) other;
        return this.time == that.time
                && Objects.equal(this.position, that.position)
                && Objects.equal(this.text, that.text)
                && Objects.equal(this.author, that.author);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.time, this.position, this.text, this.author);
    }
}
