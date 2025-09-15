package org.jmouse.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 📦 {@link Resource} backed by a single-use {@link InputStream}.
 *
 * <p>Useful when the content is provided as a stream (e.g. network response,
 * socket, or dynamically generated data) and should only be consumed once.</p>
 *
 * ⚠️ Unlike {@link ByteArrayResource}, this resource is <b>not reusable</b>.
 * Once the stream has been read, subsequent calls to {@link #getInputStream()}
 * will throw an exception.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * InputStream input = Files.newInputStream(Path.of("data.txt"));
 * Resource resource = new InputStreamResource(input);
 *
 * try (InputStream stream = resource.getInputStream()) {
 *     System.out.println(new String(stream.readAllBytes()));
 * }
 *
 * // Second attempt will fail:
 * resource.getInputStream(); // throws IllegalStateException
 * }</pre>
 */
public class InputStreamResource extends AbstractResource {

    private final InputStream stream;
    private       boolean     read = false;

    /**
     * 🏗️ Create a new resource backed by the given stream.
     *
     * @param stream underlying input stream (not closed automatically)
     */
    public InputStreamResource(InputStream stream) {
        this.stream = stream;
    }

    /**
     * @return display name including type and size (if known)
     */
    @Override
    public String getName() {
        return "%s[%d]".formatted(getResourceName(), getLength());
    }

    /**
     * @return constant string {@code "INPUT_STREAM"}
     */
    @Override
    public String getResourceName() {
        return "INPUT_STREAM";
    }

    /**
     * 📥 Return the underlying input stream.
     *
     * <p>Can only be called once. Subsequent calls will throw
     * {@link IllegalStateException}.</p>
     *
     * @return the wrapped {@link InputStream}
     * @throws IOException if stream cannot be obtained
     * @throws IllegalStateException if the stream has already been consumed
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if (this.read) {
            throw new IllegalStateException("Resource '%s' has already been read".formatted(getResourceName()));
        }

        this.read = true;

        return stream;
    }
}
