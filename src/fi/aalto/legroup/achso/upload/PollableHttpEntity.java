/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
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

    public PollableHttpEntity(HttpEntity wrapped, final ProgressListener listener) {
        super(wrapped);
        mProgressListener = listener;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        super.writeTo(new CountingOutputStream(outstream, mProgressListener));
    }

    public static interface ProgressListener {
        void transferred(long bytes);
    }

    public static class CountingOutputStream extends FilterOutputStream {

        private final ProgressListener mProgressListener;
        private long mTransferred;

        public CountingOutputStream(final OutputStream out, final ProgressListener listener) {
            super(out);
            mProgressListener = listener;
            mTransferred = 0;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            mTransferred += len;
            mProgressListener.transferred(mTransferred);
        }

        public void write(int b) throws IOException {
            out.write(b);
            mTransferred++;
            mProgressListener.transferred(mTransferred);
        }
    }

}
