package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.resolve.SqlTextLoader;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Classpath-based {@link SqlTextLoader}.
 *
 * <p>This loader reads SQL text from a classpath resource using the configured
 * character set.</p>
 *
 * @author Ivan Hontarenko
 */
public class ClasspathSqlTextLoader implements SqlTextLoader {

    private final ClassLoader classLoader;
    private final Charset     charset;

    /**
     * Creates a new loader using the current thread context class loader and UTF-8.
     */
    public ClasspathSqlTextLoader() {
        this(Thread.currentThread().getContextClassLoader(), StandardCharsets.UTF_8);
    }

    /**
     * Creates a new loader.
     *
     * @param classLoader class loader used to resolve resources
     * @param charset     charset used to decode resource bytes
     */
    public ClasspathSqlTextLoader(ClassLoader classLoader, Charset charset) {
        this.classLoader = Verify.nonNull(classLoader, "classLoader");
        this.charset = Verify.nonNull(charset, "charset");
    }

    @Override
    public String load(String location) {
        Verify.notBlank(location, "location");

        String normalized = normalize(location);

        try (InputStream stream = classLoader.getResourceAsStream(normalized)) {
            if (stream == null) {
                throw new IllegalStateException("SQL resource not found: " + location);
            }

            byte[] bytes = stream.readAllBytes();
            return new String(bytes, charset);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load SQL resource: " + location, exception);
        }
    }

    /**
     * Normalizes classpath resource location.
     *
     * @param location original resource location
     *
     * @return normalized resource location
     */
    protected String normalize(String location) {
        return location.startsWith("/") ? location.substring(1) : location;
    }

}