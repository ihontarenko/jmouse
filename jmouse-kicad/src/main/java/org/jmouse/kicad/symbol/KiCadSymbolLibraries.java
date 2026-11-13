package org.jmouse.kicad.symbol;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 📚 KiCad's installed symbol libraries — {@code share/kicad/symbols}, each a {@code LIBRARY.kicad_sym} file —, asked whether
 * they hold a symbol by the name a schematic gives it ({@code Transistor_FET:2N7002}). A schematic keeps its own copy of every
 * symbol it places; whether that copy is one of KiCad's is what these libraries answer — the library of that name holds a
 * symbol of that name —, never what the name alone looks like. The footprints' counterpart is
 * {@link org.jmouse.kicad.footprint.KiCadFootprintLibraries}.
 *
 * <p>A library is read once, on the first question about it; a folder that does not exist holds none.</p>
 */
public final class KiCadSymbolLibraries {

    /** Libraries that hold nothing. */
    public static final KiCadSymbolLibraries NONE = new KiCadSymbolLibraries(List.of());

    private static final String LIBRARY_SUFFIX = ".kicad_sym";

    private final List<Path>               folders;
    private final Map<String, Set<String>> namesByLibrary = new ConcurrentHashMap<>();

    private KiCadSymbolLibraries(List<Path> folders) {
        this.folders = List.copyOf(folders);
    }

    /** KiCad's installed libraries in these folders, searched in order. */
    public static KiCadSymbolLibraries in(List<Path> folders) {
        return new KiCadSymbolLibraries(folders);
    }

    /**
     * Whether an installed library holds the symbol a schematic names, {@code LIBRARY:NAME}; false for a name that names no
     * library, and for a library none of the folders holds.
     */
    public boolean holds(String libraryIdentifier) {
        int separator = libraryIdentifier == null ? -1 : libraryIdentifier.indexOf(':');
        if (separator <= 0 || separator == libraryIdentifier.length() - 1) {
            return false;
        }
        String library = libraryIdentifier.substring(0, separator);
        return namesByLibrary.computeIfAbsent(library, this::namesIn).contains(libraryIdentifier.substring(separator + 1));
    }

    /** The names of the symbols the first folder holding a library of that name has in it; none when no folder does. */
    private Set<String> namesIn(String library) {
        for (Path folder : folders) {
            Path file = folder.resolve(library + LIBRARY_SUFFIX);
            if (Files.isRegularFile(file)) {
                try {
                    return KiCadSymbolIndex.read(library, Files.readString(file, StandardCharsets.UTF_8)).stream()
                            .map(KiCadSymbolIndex.Entry::name).collect(Collectors.toUnmodifiableSet());
                } catch (IOException unreadable) {
                    throw new UncheckedIOException("The symbol library " + file + " cannot be read", unreadable);
                }
            }
        }
        return Set.of();
    }
}
