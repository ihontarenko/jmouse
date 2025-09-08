package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;
import org.jmouse.web.http.request.Headers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

public class VersionalResource implements HttpResource {

    private final Resource delegate;
    private final Headers  headers = new Headers();

    public VersionalResource(Resource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    public URL getURL() {
        return delegate.getURL();
    }

    @Override
    public boolean isFile() {
        return delegate.isFile();
    }

    @Override
    public File getFile() throws IOException {
        return delegate.getFile();
    }

    @Override
    public String getResourceName() {
        return delegate.getResourceName();
    }

    @Override
    public Reader getReader() {
        return delegate.getReader();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }
}
