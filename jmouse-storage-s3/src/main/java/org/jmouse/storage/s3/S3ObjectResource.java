package org.jmouse.storage.s3;

import org.jmouse.core.io.AbstractResource;
import org.jmouse.core.io.Resource;
import org.jmouse.storage.StorageKey;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * 📡 A {@link Resource} over an object arriving from the store.
 *
 * <p>Exists for two reasons, both of which are about not reading bytes twice.</p>
 *
 * <p><strong>Length.</strong> {@link Resource#getLength()} defaults to reading the whole stream to
 * count its bytes, which for a single-use network stream means the object is consumed before
 * anybody gets to read it — the length is measured and the bytes are gone. The store already
 * reported the length in the response headers, so it is taken from there.</p>
 *
 * <p><strong>Offset.</strong> A ranged read fetches only the requested bytes, so the stream begins
 * partway into the object. Consumers of a {@link org.jmouse.core.io.ResourceSegment} — the web
 * layer's partial-content writer among them — reach the segment's start by skipping that many bytes
 * from the resource, which is exactly right for a file on disk and would be a second skip here.
 * {@link #getInputStream()} therefore reports the leading bytes as already skipped, so a caller
 * cannot tell a ranged object-store read apart from a ranged local read while only the requested
 * bytes ever cross the network.</p>
 *
 * <p>Single-use, like the connection underneath it. The caller closes what it opens; closing the
 * stream releases the connection back to the SDK's pool, and <em>not</em> closing it eventually
 * exhausts that pool.</p>
 */
public class S3ObjectResource extends AbstractResource {

    /**
     * 🏷️ Name reported for every object served by this backend.
     */
    public static final String RESOURCE_NAME = "S3_OBJECT";

    private final StorageKey                             key;
    private final ResponseInputStream<GetObjectResponse> stream;
    private final long                                   objectLength;
    private final long                                   streamOffset;

    /**
     * 🏗️ Wrap a whole object.
     *
     * @param key          key the object was read from
     * @param stream       the open response stream
     * @param objectLength length of the object in the store
     */
    public S3ObjectResource(StorageKey key, ResponseInputStream<GetObjectResponse> stream, long objectLength) {
        this(key, stream, objectLength, 0);
    }

    /**
     * 🏗️ Wrap part of an object.
     *
     * @param key          key the object was read from
     * @param stream       the open response stream, holding the requested bytes only
     * @param objectLength length of the whole object in the store, not of the fetched part
     * @param streamOffset offset into the object at which {@code stream} begins
     */
    public S3ObjectResource(StorageKey key, ResponseInputStream<GetObjectResponse> stream, long objectLength,
                            long streamOffset) {
        this.key          = key;
        this.stream       = stream;
        this.objectLength = objectLength;
        this.streamOffset = streamOffset;
    }

    @Override
    public String getName() {
        return key.value();
    }

    @Override
    public String getResourceName() {
        return RESOURCE_NAME;
    }

    /**
     * 📏 The length of the whole object, as the store reported it — never a length counted here,
     * and never the length of a fetched range.
     *
     * @return object length in bytes
     */
    @Override
    public long getLength() {
        return objectLength;
    }

    /**
     * 📥 The object's bytes, positioned as if the whole object were being read.
     *
     * @return the open stream
     * @throws IOException never, declared to satisfy the contract
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if (streamOffset <= 0) {
            return stream;
        }

        return new PrePositionedInputStream(stream, streamOffset);
    }

    /**
     * ⏭️ A stream whose first bytes were never sent, and which answers for them anyway.
     *
     * <p>Only {@link #skip(long)} is affected, and only until the offset is used up: a caller that
     * skips to where the range already begins is told the skip succeeded, without a byte being
     * discarded. Everything past that behaves normally, so an over-long skip inside the range still
     * skips real bytes.</p>
     */
    private static final class PrePositionedInputStream extends InputStream {

        private final InputStream delegate;
        private       long        outstandingOffset;

        private PrePositionedInputStream(InputStream delegate, long outstandingOffset) {
            this.delegate          = delegate;
            this.outstandingOffset = outstandingOffset;
        }

        @Override
        public long skip(long count) throws IOException {
            if (outstandingOffset <= 0) {
                return delegate.skip(count);
            }

            long absorbed = Math.min(count, outstandingOffset);
            outstandingOffset -= absorbed;

            return absorbed + delegate.skip(count - absorbed);
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return delegate.read(buffer, offset, length);
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
