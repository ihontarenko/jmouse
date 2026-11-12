package org.jmouse.web.http.converter;

import jakarta.servlet.http.HttpServletRequest;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Objects;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpStatus;
import org.jmouse.web.http.RequestHeaders;
import org.jmouse.web.mvc.HttpRequestException;

/** Servlet input adapter with a byte budget, including requests without a declared content length. */
public final class ServletHttpInputMessage implements HttpInputMessage {

    private final Headers headers;
    private final PushbackInputStream body;

    public ServletHttpInputMessage(HttpServletRequest request, long maximumBytes) throws IOException {
        Objects.requireNonNull(request, "request");
        if (maximumBytes <= 0) {
            throw new IllegalArgumentException("Request body byte limit must be positive");
        }
        if (request.getContentLengthLong() > maximumBytes) {
            throw tooLarge();
        }
        headers = RequestHeaders.ofRequest(request).headers();
        body = new PushbackInputStream(new BudgetedBody(request.getInputStream(), maximumBytes), 1) {
            @Override
            public void close() {
                // The servlet container owns the underlying request stream.
            }
        };
    }

    public boolean hasBody() throws IOException {
        int first = body.read();
        if (first < 0) {
            return false;
        }
        body.unread(first);
        return true;
    }

    /** Enforces the budget even when a converter stops after one value or buffers ahead. */
    public void discardRemaining() throws IOException {
        body.transferTo(java.io.OutputStream.nullOutputStream());
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

    @Override
    public InputStream getInputStream() {
        return body;
    }

    private static HttpRequestException tooLarge() {
        return new HttpRequestException(HttpStatus.PAYLOAD_TOO_LARGE, "Request body exceeds its byte limit");
    }

    private static final class BudgetedBody extends FilterInputStream {

        private final long maximumBytes;
        private long consumed;

        private BudgetedBody(InputStream input, long maximumBytes) {
            super(input);
            this.maximumBytes = maximumBytes;
        }

        @Override
        public int read() throws IOException {
            int value = in.read();
            account(value < 0 ? 0 : 1);
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int count = in.read(bytes, offset, length);
            account(Math.max(0, count));
            return count;
        }

        @Override
        public long skip(long count) throws IOException {
            long skipped = 0;
            byte[] buffer = new byte[(int) Math.min(8192, Math.max(1, count))];
            while (skipped < count) {
                int read = read(buffer, 0, (int) Math.min(buffer.length, count - skipped));
                if (read < 0) {
                    break;
                }
                skipped += read;
            }
            return skipped;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public void reset() throws IOException {
            throw new IOException("Request body budget does not support reset");
        }

        private void account(long count) {
            if (count > maximumBytes - consumed) {
                throw tooLarge();
            }
            consumed += count;
        }
    }
}
