package org.jmouse.validator.el.loader;

import java.util.List;

/**
 * Where {@code .jmv} documents come from — the one thing {@link JmvLoader} does not know how to do. 📁
 *
 * <h2>⚠️ An interface, for the reason the mapping and policy loaders have one</h2>
 *
 * <p>The loader owns composition: read every document, compile it, refuse a name two files claim. It
 * owns none of the input/output, because "a location" means a classpath pattern in a Spring
 * application, a directory on disk in a command-line tool, and a row in a table on the day a form
 * builder can save one — and that last case is not hypothetical here, it is the point of the
 * builder.</p>
 *
 * <p>Deliberately the same shape as {@code JmmSources} and {@code PolicySources}, so a product wiring
 * several of these languages writes the same kind of thing several times rather than several different
 * kinds.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface JmvSources {

    /**
     * Every document a configured location names.
     *
     * <p>⚠️ Ordered. Load order decides nothing about what a validation does, because a document is
     * described in exactly one file; but it decides which of two files claiming one name is reported as
     * the duplicate, and a message that changed between restarts would be a poor thing to debug.</p>
     *
     * @param location as configured
     * @return the documents it names, in a stable order; empty where it names none
     */
    List<JmvSource> at(String location);

    /**
     * Documents found on the classpath.
     *
     * @return a source reading the current thread's context classloader
     */
    static JmvSources ofClasspath() {
        return new ClasspathJmvSources();
    }
}
