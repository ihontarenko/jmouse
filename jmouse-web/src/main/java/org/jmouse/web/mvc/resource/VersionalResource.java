package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;
import org.jmouse.web.http.request.Headers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * 🏷️ Resource wrapper that adds version metadata.
 *
 * <p>Delegates all calls to the underlying {@link Resource}
 * while attaching version info (e.g. via {@code ETag} header).</p>
 */
public class VersionalResource implements HttpResource {

    /**
     * 🎯 Underlying resource being wrapped.
     */
    private final Resource delegate;

    /**
     * 🏷️ Fixed version identifier.
     */
    private final String version;

    /**
     * 🏗️ Create a versioned resource wrapper.
     *
     * @param delegate resource to wrap
     * @param version  version identifier
     */
    public VersionalResource(Resource delegate, String version) {
        this.delegate = delegate;
        this.version = version;
    }

    /**
     * 📑 Build headers for this resource.
     *
     * <p>Sets a weak {@code ETag} based on the version string.</p>
     *
     * @return headers containing {@code ETag}
     */
    @Override
    public Headers getHeaders() {
        Headers headers = new Headers();
        headers.setETag("W/\"" + this.version + "\"");
        return headers;
    }

    /**
     * @return delegate resource name
     */
    @Override
    public String getName() {
        return delegate.getName();
    }

    /**
     * @return delegate size in bytes
     */
    @Override
    public long getLength() {
        return delegate.getLength();
    }

    /**
     * ⏱️ Get the last-modified timestamp of this resource, if available.
     */
    @Override
    public long getLastModified() {
        return delegate.getLastModified();
    }

    /**
     * @return delegate URL
     */
    @Override
    public URL getURL() {
        return delegate.getURL();
    }

    /**
     * @return {@code true} if delegate is a file resource
     */
    @Override
    public boolean isFile() {
        return delegate.isFile();
    }

    /**
     * @return delegate file handle
     */
    @Override
    public File getFile() throws IOException {
        return delegate.getFile();
    }

    /**
     * @return delegate logical resource name
     */
    @Override
    public String getResourceName() {
        return delegate.getResourceName();
    }

    /**
     * 🔗 Resolve a new resource relative to this one.
     *
     * @param relativePath relative path from this resource
     * @return merged resource reference
     */
    @Override
    public Resource merge(String relativePath) {
        return delegate.merge(relativePath);
    }

    /**
     * @return delegate reader
     */
    @Override
    public Reader getReader() {
        return delegate.getReader();
    }

    /**
     * @return delegate input stream
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public String toString() {
        return "Versional['%s']: %s".formatted(version, delegate);
    }
}
