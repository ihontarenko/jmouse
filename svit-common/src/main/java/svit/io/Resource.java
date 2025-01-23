package svit.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static svit.reflection.Reflections.getShortName;

/**
 * Represents a resource that can be accessed and manipulated.
 * <p>
 * This interface provides methods to interact with resources in various forms, such as files, URLs,
 * and input streams. It includes functionality to check for existence, retrieve size, and get
 * metadata like name, URI, and URL.
 * </p>
 *
 * @see ReadableResource
 */
public interface Resource extends ReadableResource {

    String PREFIX_CLASSPATH = "classpath";
    String JAR_FILE         = "jar";
    String FILE_URL         = "file";
    String HTTPS_URL        = "https";
    String HTTP_URL         = "http";
    String FILE_SYSTEM      = "local";

    /**
     * Returns the name of the resource.
     *
     * @return the name of the resource
     */
    String getName();

    /**
     * Checks if the resource exists.
     *
     * @return {@code true} if the resource exists, {@code false} otherwise
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
     *
     * @return the size of the resource
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
}
