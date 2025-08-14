package org.jmouse.util;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Paths;

final public class Files {

    public static final char   COLON        = ':';
    public static final String SLASH        = "/";
    public static final String SYSTEM_SLASH = FileSystems.getDefault().getSeparator();
    public static final String DOT          = ".";

    private Files() {
    }

    /**
     * Converts the package name of a given class to a file system path.
     */
    public static String packageToPath(Class<?> clazz, String separator) {
        return clazz.getPackage().getName().replace(DOT, separator);
    }

    /**
     * Extracts the protocol from a given path.
     *
     * @param path           the input path
     * @param defaultProtocol the default protocol to return if no protocol is found
     * @return the extracted protocol, or the default protocol if none exists
     */
    public static String extractProtocol(String path, String defaultProtocol) {
        int colonIndex = path.indexOf(COLON);

        if (colonIndex == -1) {
            return defaultProtocol;
        }

        return path.substring(0, colonIndex);
    }

    /**
     * Removes the protocol (e.g., "file://", "http://") from the given path.
     * If no protocol is present, the original path is returned.
     *
     * @param path the file path or URL string
     * @return the path without the protocol prefix
     */
    public static String removeProtocol(String path) {
        String protocol = extractProtocol(path, null);

        if (protocol != null) {
            path = path.substring(protocol.length() + 1);
        }

        return path;
    }

    /**
     * Converts a URL to a file system path.
     *
     * This method resolves the URL to a URI and converts it to a file path string.
     *
     * @param url the URL to convert
     * @return the file path corresponding to the URL
     * @throws RuntimeException if the URL cannot be converted to a URI
     */
    public static String getPath(URL url) {
        try {
            return Paths.get(url.toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Normalizes the given file path to use the system's default file separator.
     *
     * If the path is {@code null}, it returns the system's default separator.
     * Converts all forward slashes ('/') in the path to the system-specific separator.
     *
     * @param path the file path to normalize
     * @return the normalized file path with system-specific separators
     */
    public static String normalizePath(String path) {
        return path == null ? SYSTEM_SLASH : path.replace(SLASH, SYSTEM_SLASH);
    }

    /**
     * Normalizes the given file path to use the system's default file separator,
     * and then replaces the system separator with the specified separator.
     *
     * This method is useful for enforcing a specific separator ('/' or '\\')
     * regardless of the operating system.
     *
     * @param path      the file path to normalize
     * @param separator the desired file separator ('/' or '\\')
     * @return the normalized file path with the specified separator
     */
    public static String normalizePath(String path, String separator) {
        return normalizePath(path).replace(SYSTEM_SLASH, separator);
    }

    /**
     * Retrieves the relative path of a URL based on a specified base path.
     */
    public static String getRelativePath(URL url, String basePath) {
        return Strings.suffix(url.toExternalForm(), basePath);
    }

    /**
     * Removes the file extension from a given path.
     */
    public static String removeExtension(String path) {
        return Strings.prefix(path, DOT);
    }

}
