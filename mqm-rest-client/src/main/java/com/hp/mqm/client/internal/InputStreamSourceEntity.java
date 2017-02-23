/*
 * Copyright 2017 Hewlett-Packard Development Company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.mqm.client.internal;

import com.hp.mqm.client.InputStreamSource;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamSourceEntity extends AbstractHttpEntity {

    private final static int INNER_OUTPUT_BUFFER_SIZE = 2048;

    private InputStreamSource inputStreamSource;
    private final long length;

    public InputStreamSourceEntity(InputStreamSource inputStreamSource) {
        this(inputStreamSource, null);
    }

    public InputStreamSourceEntity(InputStreamSource inputStreamSource, long length) {
        this(inputStreamSource, length, null);
    }

    public InputStreamSourceEntity(InputStreamSource inputStreamSource, ContentType contentType) {
        this(inputStreamSource, -1, contentType);
    }

    public InputStreamSourceEntity(InputStreamSource inputStreamSource, long length, ContentType contentType) {
        if (inputStreamSource == null) {
            throw new IllegalArgumentException("InputStreamSource cannot be null.");
        }
        this.length = length;
        if (contentType != null) {
            setContentType(contentType.toString());
        }
        this.inputStreamSource = inputStreamSource;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return length;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public InputStream getContent() throws IOException {
        InputStream inputStream = inputStreamSource.getInputStream();
        if (inputStream == null) {
            throw new IllegalStateException("InputStreamSource#getInputSteam() returns null.");
        }
        return inputStream;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream cannot be null.");
        }
        final InputStream inputStream = getContent();
        try {
            final byte[] buffer = new byte[INNER_OUTPUT_BUFFER_SIZE];
            int l;
            if (this.length < 0) {
                while ((l = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, l);
                }
            } else {
                long remaining = this.length;
                while (remaining > 0) {
                    l = inputStream.read(buffer, 0, (int)Math.min(INNER_OUTPUT_BUFFER_SIZE, remaining));
                    if (l == -1) {
                        break;
                    }
                    outputStream.write(buffer, 0, l);
                    remaining -= l;
                }
            }
        } finally {
            inputStream.close();
        }
    }
}
