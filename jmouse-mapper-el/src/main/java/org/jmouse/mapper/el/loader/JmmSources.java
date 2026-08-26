package org.jmouse.mapper.el.loader;

import java.util.List;

/**
 * Where {@code .jmm} documents come from — the one thing {@link JmmLoader} does not know how to do. 📁
 *
 * <h2>⚠️ An interface, for the reason the policy loader has one</h2>
 *
 * <p>The loader owns composition: read every document, bind it, refuse a target two files claim. It
 * owns none of the input/output, because "a location" means a classpath pattern in a Spring
 * application, a directory on disk in a command-line tool, and a row in a table on the day a control
 * room can save one. All three answer the same question.</p>
 *
 * <p>This is deliberately the same shape as {@code PolicySources} in {@code jmouse-access-el} — minus
 * its {@code included(…)}, because a {@code .jmm} fragment is <strong>file-scoped</strong> (§7 of the
 * reference document) and no document reaches another. Copying the shape means a product wiring both
 * languages writes the same kind of thing twice rather than two different kinds.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface JmmSources {

    /**
     * Every document a configured location names.
     *
     * <p>A location may match several, so this returns a list — and an <strong>ordered</strong> one.
     * Load order decides nothing about what a mapping does, because a target is described in exactly
     * one file; but it decides which of two files claiming one target is named as the duplicate, and
     * a message that changed between restarts would be a poor thing to debug.</p>
     *
     * @param location as configured
     * @return the documents it names, in a stable order; empty where it names none
     */
    List<JmmSource> at(String location);

    /**
     * Documents found on the classpath.
     *
     * @return a source reading the current thread's context classloader
     */
    static JmmSources ofClasspath() {
        return new ClasspathJmmSources();
    }
}
