package svit.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A {@link Resource} implementation that represents a resource loaded from a {@link URL}.
 *
 * @see AbstractResource
 * @see Resource
 */
public class URLResource extends AbstractResource {

    private final URL url;

    /**
     * Constructs a new {@link URLResource} for the given {@link URL}.
     *
     * @param url the URL of the resource
     */
    public URLResource(URL url) {
        this.url = url;
    }

    /**
     * Returns the name of the resource (the last segment of the URL path).
     *
     * @return the name of the resource
     */
    @Override
    public String getName() {
        return url.getProtocol() + ":" + url.getFile();
    }

    /**
     * Returns the size of the resource in bytes.
     *
     * @return the size of the resource
     * @throws RuntimeException if the size cannot be determined
     * <p>
     * This method uses {@link InputStream#available()} to estimate the size of the resource.
     * </p>
     */
    @Override
    public long getSize() {
        try (InputStream stream = getInputStream()) {
            return stream.available();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get size of resource: %s".formatted(getName()), e);
        }
    }

    /**
     * Returns the {@link URL} of the resource.
     *
     * @return the URL of the resource
     */
    @Override
    public URL getURL() {
        return url;
    }

    /**
     * Determines whether the resource is file-based.
     *
     * @return {@code true} if the resource is file-based, {@code false} otherwise
     * <p>
     * A resource is considered file-based if its protocol is {@code file}.
     * </p>
     */
    @Override
    public boolean isFile() {
        return FILE_PROTOCOL.equals(getURL().getProtocol());
    }

    /**
     * Returns a {@link File} representation of the resource.
     *
     * @return the file representation of the resource
     * @throws IOException        if an I/O error occurs
     * @throws ResourceException if the resource is not file-based
     */
    @Override
    public File getFile() throws IOException {
        if (isFile()) {
            return new File(getURL().getFile());
        }

        throw new ResourceException("URL is not a file: %s".formatted(getURL()));
    }

    /**
     * Opens an {@link InputStream} for reading the contents of the resource.
     *
     * @return an {@link InputStream} for the resource
     * @throws IOException if an I/O error occurs while opening the stream
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    /**
     * Returns a human-readable name for the resource.
     */
    @Override
    public String getResourceName() {
        return "URL";
    }

}
