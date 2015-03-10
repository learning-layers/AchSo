package fi.aalto.legroup.achso.entities;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.views.utilities.ColorGenerator;

public class User implements JsonSerializable, Parcelable {

    protected String name;
    protected Uri uri;

    @SuppressWarnings("UnusedDeclaration")
    private User() {
        // For serialization
    }

    public User(String name, Uri uri) {
        this.name = name;
        this.uri = uri;
    }

    protected User(Parcel parcel) {
        this.name = parcel.readString();
        this.uri = (Uri) parcel.readValue(Uri.class.getClassLoader());
    }

    public int getColor() {
        return ColorGenerator.getSeededColor(this.uri);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getUri() {
        return this.uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.name);
        parcel.writeValue(this.uri);
    }

    public static final Creator<User> CREATOR = new Creator<User>() {

        @Override
        public User createFromParcel(Parcel parcel) {
            return new User(parcel);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }

    };

}
