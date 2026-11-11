package org.jmouse.cad.spice;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Where the files a netlist's {@code .include} and {@code .lib} name are read from. The converter itself reads no
 * file and no address: what an include gets is what its caller hands over — the files a reader chose from their own
 * cabinet, keyed by the path the netlist writes.
 */
@FunctionalInterface
public interface SpiceIncludes {

    /** The text of the file the netlist names {@code path}; empty when none was given. */
    Optional<String> text(String path);

    /** No file at all: every include is left out with a note. */
    static SpiceIncludes none() {
        return path -> Optional.empty();
    }

    /**
     * The files given, by the path the netlist writes. A path is matched as written, then by its file name alone and
     * without case — a deck says {@code .include "models/TL072.lib"} and the reader hands over {@code tl072.lib}.
     */
    static SpiceIncludes of(Map<String, String> files) {
        if (files == null || files.isEmpty()) {
            return none();
        }
        return path -> {
            if (files.containsKey(path)) {
                return Optional.of(files.get(path));
            }
            String name = fileName(path);
            return files.entrySet().stream().filter(entry -> fileName(entry.getKey()).equals(name))
                    .map(Map.Entry::getValue).findFirst();
        };
    }

    private static String fileName(String path) {
        String unquoted = path.replace("\"", "").replace("'", "").trim();
        int slash = Math.max(unquoted.lastIndexOf('/'), unquoted.lastIndexOf('\\'));
        return unquoted.substring(slash + 1).toLowerCase(Locale.ROOT);
    }
}
