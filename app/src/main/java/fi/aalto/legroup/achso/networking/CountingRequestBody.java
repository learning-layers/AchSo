package fi.aalto.legroup.achso.networking;

import com.google.common.io.CountingOutputStream;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;

import okio.BufferedSink;
import okio.Okio;

/**
 * Decorates an OkHttp request body to count the number of bytes written when writing it.
 * Useful for tracking the upload progress of large multi-part requests.
 *
 * @author Leo Nikkilä
 */
public class CountingRequestBody extends RequestBody {

    protected RequestBody body;
    protected CountingOutputStream stream;

    public CountingRequestBody(RequestBody body) {
        this.body = body;
    }

    @Override
    public MediaType contentType() {
        return body.contentType();
    }

    @Override
    public long contentLength() {
        return body.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        stream = new CountingOutputStream(sink.outputStream());
        BufferedSink newSink = Okio.buffer(Okio.sink(stream));

        body.writeTo(newSink);
    }

    /**
     * @return number of bytes written
     */
    public long getCount() {
        if (stream == null) return 0;
        return stream.getCount();
    }

}
