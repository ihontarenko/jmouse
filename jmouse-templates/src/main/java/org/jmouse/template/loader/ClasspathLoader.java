package org.jmouse.template.loader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ClasspathLoader extends AbstractLoader<String> {

    private final ClassLoader classLoader;

    public ClasspathLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClasspathLoader() {
        this(ClasspathLoader.class.getClassLoader());
    }

    @Override
    public Reader load(String name) {
        Reader reader   = null;
        String location = resolvePath(name);

        InputStream stream = classLoader.getResourceAsStream(location);

        if (stream != null) {
            reader = new BufferedReader(new InputStreamReader(stream));
        }

        if (reader == null) {
            throw new TemplateLoaderException("Failed to load resource " + name);
        }

        return reader;
    }

    private String resolvePath(String path) {
        StringBuilder builder = new StringBuilder();

        if (getPrefix() != null) {
            builder.append(getPrefix());
        }

        builder.append(path);

        if (getSuffix() != null) {
            builder.append(getSuffix());
        }

        return builder.toString();
    }

}
