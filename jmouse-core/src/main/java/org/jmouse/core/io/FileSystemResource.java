package org.jmouse.core.io;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
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

    /**
     * Constructs a new {@link FileSystemResource} for the given file system path.
     */
    public FileSystemResource(Path path) {
        this.path = path;
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
    public long getSize() {
        return path.toFile().length();
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
        return path.toFile();
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
     * Returns a human-readable name for the resource.
     */
    @Override
    public String getResourceName() {
        return "FileSystem(%s)".formatted(System.getProperty("os.name"));
    }
}
