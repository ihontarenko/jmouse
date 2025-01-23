package svit.util;

import svit.io.JarResourceException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class for handling Java IO operations, specifically related to URLs and resources.
 */
final public class JavaIO {

    private JavaIO() {}

    /**
     * Converts a location string to a {@link URL}, resolving it using the provided {@link ClassLoader}.
     *
     * @param location the resource location, which can include a protocol (e.g., "jar:", "file:")
     * @param loader   the {@link ClassLoader} used for resolving resources
     * @return a {@link URL} representing the resolved location
     * @throws JarResourceException if the location cannot be converted to a URL
     */
    public static URL toURL(String location, ClassLoader loader) {
        try {
            String clean = Files.removeProtocol(location);
            return Jars.isJarURL(clean) || clean.startsWith("/") ? new URI(location).toURL()
                    : loader.getResource(clean);
        } catch (MalformedURLException | URISyntaxException e) {
            throw new JarResourceException("Failed to create URL from '%s' location".formatted(location), e);
        }
    }

}
