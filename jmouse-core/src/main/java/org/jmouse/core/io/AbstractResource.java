package org.jmouse.core.io;

import java.io.*;
import java.net.URL;

/**
 * An abstract base class for {@link Resource} implementations.
 * @see Resource
 */
public abstract class AbstractResource implements Resource {

    /**
     * Returns a {@link Reader} for reading the contents of the resource.
     */
    @Override
    public Reader getReader() {
        try {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        } catch (IOException e) {
            throw new ResourceException("Failed to create reader for: " + getResourceName(), e);
        }
    }

    /**
     * 🌍 Get the {@link URL} of this resource.
     */
    @Override
    public URL getURL() {
        throw new ResourceException("Resource '%s' cannot be resolved as URL".formatted(getResourceName()));
    }

    /**
     * 📄 Whether this resource is file-based.
     */
    @Override
    public boolean isFile() {
        return false;
    }

    /**
     * 📂 Get the {@link File} representation of this resource.
     *
     * @return file handle
     * @throws IOException if the resource cannot be resolved to a file
     */
    @Override
    public File getFile() throws IOException {
        throw new ResourceException("Resource '%s' cannot be resolved as file".formatted(getResourceName()));
    }

    /**
     * 🔗 Resolve a new resource relative to this one.
     *
     * @param relativePath relative path from this resource
     * @return merged resource reference
     */
    @Override
    public Resource merge(String relativePath) {
        throw new ResourceException("Resource '%s' cannot be merged".formatted(getResourceName()));
    }

    /**
     * Returns a string representation of the resource.
     */
    @Override
    public String toString() {
        return "%s : %s".formatted(getResourceName(), getName());
    }
}
