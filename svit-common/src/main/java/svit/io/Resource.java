package svit.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Represents a resource that can be accessed and manipulated.
 *
 * @see ReadableResource
 */
public interface Resource extends ReadableResource {

    String JRT_PROTOCOL       = "jrt";
    String CLASSPATH_PROTOCOL = "classpath";
    String JAR_PROTOCOL       = "jar";
    String FILE_PROTOCOL      = "file";
    String HTTPS_PROTOCOL     = "https";
    String HTTP_PROTOCOL      = "http";
    String LOCAL_PROTOCOL     = "local";

    /**
     * Returns the name of the resource.
     */
    String getName();

    /**
     * Checks if the resource exists.
     */
    default boolean exists() {
        boolean exists;

        if (isFile()) {
            try {
                exists = getFile().exists();
            } catch (IOException ioException) {
                throw new ResourceException(
                        "Failed to check if resource exists for: " + getResourceName(), ioException);
            }
        } else {
            try {
                getInputStream().close();
                exists = true;
            } catch (IOException ioException) {
                exists = false;
            }
        }

        return exists;
    }

    /**
     * Returns the size of the resource in bytes.
     */
    long getSize();

    /**
     * Returns the {@link URI} of the resource.
     */
    default URI getURI() {
        try {
            return new URI(getURL().toString());
        } catch (URISyntaxException exception) {
            throw new ResourceException(this, exception);
        }
    }

    /**
     * Returns the {@link URL} of the resource.
     */
    URL getURL();

    /**
     * Determines whether the resource is file-based.
     */
    boolean isFile();

    /**
     * Returns a {@link File} representation of the resource.
     */
    File getFile() throws IOException;

    /**
     * Returns a human-readable name for the resource.
     */
    String getResourceName();

    /**
     * Return input stream bytes as String
     * */
    default String asString() {
        String string = null;

        try(InputStream stream = getInputStream()) {
            string = new String(stream.readAllBytes());
        } catch (IOException ignore) { }

        return string;
    }
}
