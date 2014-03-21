/**
 * Copyright 2013 Aalto university, see AUTHORS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.upload;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import fi.aalto.legroup.achso.database.SemanticVideo;

public class PollableHttpEntity extends HttpEntityWrapper implements HttpEntity {
    private final ProgressListener mProgressListener;
    private final SemanticVideo mVideo;

    public PollableHttpEntity(HttpEntity wrapped, final ProgressListener listener, final SemanticVideo video) {
        super(wrapped);
        mProgressListener = listener;
        mVideo = video;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        super.writeTo(new CountingOutputStream(outstream, mProgressListener, mVideo));
    }

    public static interface ProgressListener {
        void transferred(long bytes, SemanticVideo sv);
    }

    public static class CountingOutputStream extends FilterOutputStream {

        private final ProgressListener mProgressListener;
        private final SemanticVideo mVideo;
        private long mTransferred;

        public CountingOutputStream(final OutputStream out, final ProgressListener listener, final SemanticVideo video) {
            super(out);
            mProgressListener = listener;
            mTransferred = 0;
            mVideo = video;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            mTransferred += len;
            mProgressListener.transferred(mTransferred, mVideo);
        }

        public void write(int b) throws IOException {
            out.write(b);
            mTransferred++;
            mProgressListener.transferred(mTransferred, mVideo);
        }
    }

}
