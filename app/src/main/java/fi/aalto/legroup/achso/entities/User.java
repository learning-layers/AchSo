package fi.aalto.legroup.achso.entities;

import android.net.Uri;

import fi.aalto.legroup.achso.entities.serialization.json.JsonSerializable;
import fi.aalto.legroup.achso.views.utilities.ColorGenerator;

/**
 * @author Leo Nikkilä
 */
public class User implements JsonSerializable {

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

    public int getColor() {
        return ColorGenerator.getSeededColor(uri);
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

}
