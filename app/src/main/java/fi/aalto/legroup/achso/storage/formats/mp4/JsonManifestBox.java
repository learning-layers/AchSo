package fi.aalto.legroup.achso.storage.formats.mp4;

import com.google.common.base.Charsets;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.util.UUIDConverter;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * A user-defined box used for storing JSON manifests inside the MP4 container. Located at
 * /moov/udta. For fast manipulation, this box *must* be followed by a `free` box with a sufficient
 * amount of unused space for general changes. As a rule of thumb, the two boxes should take up at
 * least 64 kB of space in total. If the free space is used up, the `free` box should be grown by
 * an additional 64 kB so that the following boxes are moved forward.
 *
 * @author Leo Nikkilä
 */
public final class JsonManifestBox extends AbstractBox {

    /**
     * User-defined box, type is always UUID.
     */
    public static final String TYPE = "uuid";

    /**
     * A unique identifier specific to this type of box. Do not reuse this anywhere.
     */
    public static final UUID IDENTIFIER = UUID.fromString("388ed96f-47b1-499a-9e7e-ee304eb19661");

    /**
     * JSON string as the rest of the payload.
     */
    private String jsonString;

    public JsonManifestBox() {
        super(TYPE, UUIDConverter.convert(IDENTIFIER));
    }

    public JsonManifestBox(String jsonString) {
        this();
        this.jsonString = jsonString;
    }

    /**
     * Sets the content of the box.
     */
    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    /**
     * Returns the content of the box.
     */
    public String getJsonString() {
        return jsonString;
    }

    /**
     * Returns the character set used for encoding and decoding the content.
     */
    private Charset getCharset() {
        return Charsets.UTF_8;
    }

    @Override
    protected long getContentSize() {
        return jsonString.getBytes(getCharset()).length;
    }

    @Override
    protected void getContent(ByteBuffer buffer) {
        byte[] bytes = jsonString.getBytes(getCharset());
        buffer.put(bytes);
    }

    @Override
    protected void _parseDetails(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        jsonString = new String(bytes, getCharset());
    }

}
