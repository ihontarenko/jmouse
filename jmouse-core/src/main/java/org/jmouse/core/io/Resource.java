package org.jmouse.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * 📦 Represents a generic resource (file, classpath entry, JAR entry, etc.).
 *
 * <p>Extends {@link ReadableResource} with additional metadata
 * such as size, protocol, URI/URL access, and file conversion.</p>
 *
 * @see ReadableResource
 */
public interface Resource extends ReadableResource {

    /** ❓ Unknown resource protocol. */
    String UNKNOWN_PROTOCOL   = "unknown";
    /** 📚 Java module runtime protocol. */
    String JRT_PROTOCOL       = "jrt";
    /** 📂 Classpath protocol. */
    String CLASSPATH_PROTOCOL = "classpath";
    /** 🎁 JAR protocol. */
    String JAR_PROTOCOL       = "jar";
    /** 📄 File system protocol. */
    String FILE_PROTOCOL      = "file";
    /** 🌐 HTTPS protocol. */
    String HTTPS_PROTOCOL     = "https";
    /** 🌐 HTTP protocol. */
    String HTTP_PROTOCOL      = "http";
    /** 💻 Local protocol (custom use). */
    String LOCAL_PROTOCOL     = "local";

    /**
     * 🏷️ Get the name of this resource (filename or identifier).
     *
     * <p>Implementations may return a logical identifier (e.g. {@code "BYTE_ARRAY"},
     * {@code "INPUT_STREAM"}) or the actual filename depending on resource type.</p>
     *
     * @return display name of the resource (never {@code null})
     */
    String getName();

    /**
     * 📄 Convenience method to return the underlying file's name.
     *
     * <p>If this resource is file-based, returns {@link java.io.File#getName()}.
     * Otherwise returns {@code null}.</p>
     *
     * @return filename if file-based, otherwise {@code null}
     */
    default String getFilename() {
        String filename = null;

        try {
            filename = getFile().getName();
        } catch (IOException ignore) { }

        return filename;
    }


    /**
     * ✅ Check if the resource exists.
     *
     * <p>For file-based resources, checks {@link File#exists()}.
     * For others, attempts to open and close an {@link InputStream}.</p>
     *
     * @return {@code true} if accessible
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
     * Checks if the resource is readable.
     */
    @Override
    default boolean isReadable() {
        return exists();
    }

    /**
     * 📏 Get the size of the resource in bytes.
     */
    default long getSize() {
        long size = 0;

        try {
            try (InputStream stream = getInputStream()) {
                byte[] buffer = new byte[256];
                int    read;

                while ((read = stream.read(buffer)) != -1) {
                    size += read;
                }

                return size;
            }
        } catch (IOException ignore) { }

        return size;
    }

    /**
     * 🔗 Get the {@link URI} of this resource.
     *
     * @return resource URI
     * @throws ResourceException if the URL cannot be converted to a URI
     */
    default URI getURI() {
        try {
            return new URI(getURL().toString());
        } catch (URISyntaxException exception) {
            throw new ResourceException(this, exception);
        }
    }

    /**
     * 🌍 Get the {@link URL} of this resource.
     */
    URL getURL();

    /**
     * 📄 Whether this resource is file-based.
     */
    boolean isFile();

    /**
     * 📂 Get the {@link File} representation of this resource.
     *
     * @return file handle
     * @throws IOException if the resource cannot be resolved to a file
     */
    File getFile() throws IOException;

    /**
     * 🏷️ Get a human-readable display name for the resource.
     */
    String getResourceName();

    /**
     * 🔗 Resolve a new resource relative to this one.
     *
     * @param relativePath relative path from this resource
     * @return merged resource reference
     */
    Resource merge(String relativePath);

    /**
     * 📝 Read all bytes of this resource as a UTF-8 string.
     *
     * @return string contents, or {@code null} if an error occurs
     */
    default String asString() {
        String string = null;

        try (InputStream stream = getInputStream()) {
            string = new String(stream.readAllBytes());
        } catch (IOException ignore) { }

        return string;
    }

    /**
     * 🌍 Convert a location string into a {@link URL}.
     *
     * <p>Returns {@code null} if the string cannot be parsed
     * into a valid URI/URL.</p>
     *
     * @param location string location (e.g. {@code file:/path/to/file})
     * @return parsed URL or {@code null} if invalid
     * @throws MalformedURLException if conversion fails
     */
    static URL toURL(String location) throws MalformedURLException {
        try {
            return toURI(location.replaceAll("/{2,}", "/")).toURL();
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * 🌍 Convert a location string into a {@link URI}.
     *
     * <p>Spaces are replaced with {@code %20} for validity.</p>
     *
     * @param location string location
     * @return parsed URI
     * @throws URISyntaxException if the string is not a valid URI
     */
    static URI toURI(String location) throws URISyntaxException {
        return new URI(location.replace(" ", "%20"));
    }

    /**
     * 🔄 Convert a {@link URL} into a {@link URI}.
     *
     * @param url URL to convert
     * @return equivalent URI
     * @throws URISyntaxException if the URL cannot be converted
     */
    static URI toURI(URL url) throws URISyntaxException {
        return toURI(url.toString());
    }


}
