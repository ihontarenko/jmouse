package org.jmouse.template.loader;

import java.io.*;

public class ClasspathLoader extends AbstractLoader<String> {

    public static final String      SEPARATOR = "/";
    private final       ClassLoader classLoader;

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
            throw new TemplateLoaderException("Failed to load template " + name);
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
