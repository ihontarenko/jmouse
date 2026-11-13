package org.jmouse.core.io;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A path in the one spelling every system reads: its names joined by {@value #SEPARATOR}, whatever separator the disk it was
 * found on writes — the way a classpath resource, an index of files and a URL write a path, so one written on Windows reads
 * the same on Linux.
 */
public final class PortablePaths {

    /** What joins the names of a portable path. */
    public static final String SEPARATOR = "/";

    private PortablePaths() {
    }

    /**
     * Where a file stands below a folder, written portably: {@code components/passive/resistor.component.jm}.
     *
     * @param folder the folder the path is taken from
     * @param file   a file at or below it
     * @throws IllegalArgumentException when the file is not below the folder
     */
    public static String below(Path folder, Path file) {
        Path normalizedFolder = folder.toAbsolutePath().normalize();
        Path normalizedFile = file.toAbsolutePath().normalize();
        if (!normalizedFile.startsWith(normalizedFolder)) {
            throw new IllegalArgumentException(file + " is not below " + folder);
        }
        return written(normalizedFolder.relativize(normalizedFile));
    }

    /** A relative path written portably: its names joined by {@value #SEPARATOR}. */
    public static String written(Path relative) {
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException(relative + " is absolute; a portable path is relative");
        }
        return StreamSupport.stream(relative.spliterator(), false).map(Path::toString).collect(Collectors.joining(SEPARATOR));
    }
}
