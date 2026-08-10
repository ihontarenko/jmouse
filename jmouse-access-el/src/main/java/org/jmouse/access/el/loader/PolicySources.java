package org.jmouse.access.el.loader;

import java.util.List;
import java.util.Optional;

/**
 * Where policy files come from — the one thing {@link PolicyLoader} does not know how to do.
 *
 * <p>The loader owns composition: parse, follow includes, detect a loop, merge. It owns none of the
 * input/output, because "a location" means a classpath pattern in a Spring application, a directory
 * on disk in a command-line tool, and a row in a table on the day the control room can save. All
 * three answer these two questions.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface PolicySources {

    /**
     * Every file a configured location names.
     *
     * <p>A location may match several — {@code classpath:policy/*.jmp} is the ordinary way to let a
     * product's modules each ship their own — so this returns a list. It returns an ordered one: merge
     * order decides nothing about who may do what, but it decides what a reader sees first, and a load
     * that shuffled between restarts would make a diff of the rendered policy useless.
     *
     * @param location as configured
     * @return the files it names, in a stable order; empty where it names none
     */
    List<PolicySource> at(String location);

    /**
     * The file an {@code include} names.
     *
     * <p>⚠️ <strong>Resolved relative to the file that wrote it</strong>, which is what makes a
     * directory of policy files movable as a unit and what every other language with an include does.
     * A path resolved against some root instead would mean a file could only be included from where it
     * happened to be written.
     *
     * @param path as written in the {@code include} line
     * @param from the file that wrote it
     * @return the file, or empty where nothing is there — the loader turns that into a failure naming
     *         the line, which it can and this cannot
     */
    Optional<PolicySource> included(String path, PolicySource from);
}
