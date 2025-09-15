package org.jmouse.core.io;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * A resource implementation for accessing and manipulating files in the file system.
 * <p>
 * This class provides methods to read from and write to files, as well as retrieve file descriptor
 * such as name, size, and URL representation.
 * </p>
 *
 * @see AbstractResource
 * @see WritableResource
 */
public class FileSystemResource extends AbstractResource implements WritableResource {

    private final Path path;
    private final File file;

    /**
     * Constructs a new {@link FileSystemResource} for the given file system path.
     */
    public FileSystemResource(Path path) {
        this.path = path;
        this.file = path.toFile();
    }

    /**
     * Returns the name of the file represented by this resource.
     */
    @Override
    public String getName() {
        return path.toString();
    }

    /**
     * Returns the size of the file in bytes.
     */
    @Override
    public long getLength() {
        return file.length();
    }

    /**
     * ⏱️ Get the last-modified timestamp of this resource, if available.
     *
     * <p>Typically expressed as the number of milliseconds since the
     * epoch (January 1, 1970 UTC). Implementations may return {@code 0}
     * if the last modification time cannot be determined.</p>
     *
     * @return last modified time in epoch milliseconds, or {@code 0} if unavailable
     */
    @Override
    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(this.path).toMillis();
        } catch (IOException exception) {
            return -1;
        }
    }

    /**
     * Checks if the resource represents a regular file.
     */
    @Override
    public boolean isFile() {
        return Files.isRegularFile(path);
    }

    /**
     * Returns a {@link File} representation of the resource.
     */
    @Override
    public File getFile() {
        return file;
    }

    /**
     * Returns the {@link URL} representation of the file.
     */
    @Override
    public URL getURL() {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new ResourceException("Malformed URL", e);
        }
    }

    /**
     * Returns an {@link InputStream} for reading the file's contents.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    /**
     * Returns a {@link Writer} for writing to the file.
     */
    @Override
    public Writer getWriter() {
        try {
            return new BufferedWriter(new OutputStreamWriter(getOutputStream()));
        } catch (IOException e) {
            throw new ResourceException("Failed to create writer for: " + getResourceName(), e);
        }
    }

    /**
     * Returns an {@link OutputStream} for writing to the file.
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        return Files.newOutputStream(path);
    }

    /**
     * 🔗 Resolve a new resource relative to this one.
     */
    @Override
    public Resource merge(String relativePath) {
        return new FileSystemResource(path.resolve(relativePath));
    }

    /**
     * Returns a human-readable name for the resource.
     */
    @Override
    public String getResourceName() {
        return "FileSystem(%s)".formatted(System.getProperty("os.name"));
    }
}
