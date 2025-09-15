package org.jmouse.core.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 📦 {@link Resource} backed by a byte array.
 *
 * <p>Provides random-access resource data from memory. Useful for
 * in-memory caching, testing, or when content is generated on the fly.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
 * Resource resource = new ByteArrayResource(data);
 *
 * System.out.println(resource.getName()); // BYTE_ARRAY[5]
 * try (InputStream stream = resource.getInputStream()) {
 *     System.out.println(new String(stream.readAllBytes())); // Hello
 * }
 * }</pre>
 */
public class ByteArrayResource extends AbstractResource {

    private final byte[] array;

    /**
     * 🏗️ Create a new in-memory resource backed by the given byte array.
     *
     * @param array resource contents (not copied)
     */
    public ByteArrayResource(byte[] array) {
        this.array = array;
    }

    /**
     * @return display name including type and size
     */
    @Override
    public String getName() {
        return "%s[%d]".formatted(getResourceName(), getLength());
    }

    /**
     * @return size of the byte array in bytes
     */
    @Override
    public long getLength() {
        return array.length;
    }

    /**
     * @return constant string {@code "BYTE_ARRAY"}
     */
    @Override
    public String getResourceName() {
        return "BYTE_ARRAY";
    }

    /**
     * @return new {@link ByteArrayInputStream} for the underlying data
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(array);
    }
}
