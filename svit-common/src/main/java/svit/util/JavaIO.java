package svit.util;

import svit.io.JarResourceException;
import svit.io.ResourceException;

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

    /**
     * Converts a {@link URI} to a {@link URL}.
     *
     * @param uri the {@link URI} to convert
     * @return the corresponding {@link URL}
     * @throws ResourceException if the conversion fails due to a malformed URL
     * @see URI#toURL()
     * @see ResourceException
     */
    public static URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new ResourceException("Failed to convert URI to URL: %s".formatted(uri), e);
        }
    }

    /**
     * Converts a {@link URL} to a {@link URI}.
     *
     * @param url the {@link URL} to convert
     * @return the corresponding {@link URI}
     * @throws ResourceException if the conversion fails due to a syntax error in the URL
     * @see URL#toURI()
     * @see ResourceException
     */
    public static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new ResourceException("Failed to convert URL to URI: %s".formatted(url), e);
        }
    }

}
