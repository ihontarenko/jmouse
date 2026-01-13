package org.jmouse.common.pipeline.definition.loading;

import org.jmouse.core.Verify;

import java.io.InputStream;

public final class ClasspathSource implements DefinitionSource {

    private final String path;
    private final ClassLoader classLoader;

    public ClasspathSource(String path) {
        this(path, Thread.currentThread().getContextClassLoader());
    }

    public ClasspathSource(String path, ClassLoader classLoader) {
        this.path = Verify.nonNull(path, "path");
        this.classLoader = Verify.nonNull(classLoader, "classLoader");
    }

    @Override
    public String location() {
        return "classpath:" + path;
    }

    @Override
    public InputStream openStream() {
        String      normalized = path.startsWith("/") ? path.substring(1) : path;
        InputStream stream     = classLoader.getResourceAsStream(normalized);

        if (stream == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + path);
        }

        return stream;
    }
}
