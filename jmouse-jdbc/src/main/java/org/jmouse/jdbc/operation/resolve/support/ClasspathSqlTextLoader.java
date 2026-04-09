package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.resolve.SqlTextLoader;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Classpath-based {@link SqlTextLoader}.
 *
 * @author Ivan Hontarenko
 */
public class ClasspathSqlTextLoader implements SqlTextLoader {

    private final ClassLoader classLoader;
    private final Charset     charset;

    public ClasspathSqlTextLoader() {
        this(Thread.currentThread().getContextClassLoader(), StandardCharsets.UTF_8);
    }

    public ClasspathSqlTextLoader(ClassLoader classLoader, Charset charset) {
        this.classLoader = Verify.nonNull(classLoader, "classLoader");
        this.charset = Verify.nonNull(charset, "charset");
    }

    @Override
    public String load(String location) {
        Verify.notBlank(location, "location");

        try (InputStream stream = classLoader.getResourceAsStream(normalize(location))) {
            if (stream == null) {
                throw new IllegalStateException("SQL resource not found: " + location);
            }

            byte[] bytes = stream.readAllBytes();
            return new String(bytes, charset);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load SQL resource: " + location, exception);
        }
    }

    protected String normalize(String location) {
        return location.startsWith("/") ? location.substring(1) : location;
    }

}