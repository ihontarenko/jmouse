package org.jmouse.kicad.footprint;

import org.jmouse.kicad.parser.KiCadParseException;
import org.jmouse.kicad.parser.KiCadParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Where footprints are found by the name a schematic gives them — {@code Package_TO_SOT_THT:TO-220-3_Vertical}, a library
 * and a footprint —: in footprint libraries, each a {@code LIBRARY.pretty} folder holding {@code NAME.kicad_mod} files.
 * The places are searched in the order given, the first that holds the footprint answering, as a project's own
 * {@code fp-lib-table} stands before KiCad's.
 */
public final class KiCadFootprintLibraries {

    /** Libraries that hold nothing. */
    public static final KiCadFootprintLibraries NONE = new KiCadFootprintLibraries(List.of());

    private static final String LIBRARY_SUFFIX = ".pretty";
    private static final String FOOTPRINT_SUFFIX = ".kicad_mod";

    private final List<Function<String, Optional<String>>> places;
    private final Map<String, Optional<KiCadFootprint>> read = new ConcurrentHashMap<>();
    private final KiCadParser parser = new KiCadParser();

    private KiCadFootprintLibraries(List<Function<String, Optional<String>>> places) {
        this.places = List.copyOf(places);
    }

    /** The libraries in these folders: each holds {@code LIBRARY.pretty} folders. A folder that does not exist holds none. */
    public static KiCadFootprintLibraries in(List<Path> folders) {
        List<Function<String, Optional<String>>> places = new ArrayList<>();
        for (Path folder : folders) {
            places.add(relative -> {
                Path file = folder.resolve(relative);
                if (!Files.isRegularFile(file)) {
                    return Optional.empty();
                }
                try {
                    return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
                } catch (IOException unreadable) {
                    throw new UncheckedIOException("The footprint " + file + " cannot be read", unreadable);
                }
            });
        }
        return new KiCadFootprintLibraries(places);
    }

    /**
     * The libraries among files a reader sent — a project's own, by their path inside its folder: any
     * {@code …/LIBRARY.pretty/NAME.kicad_mod} is the footprint {@code LIBRARY:NAME}.
     */
    public static KiCadFootprintLibraries among(Map<String, String> files) {
        Map<String, String> byRelative = new ConcurrentHashMap<>();
        files.forEach((path, text) -> {
            String normalized = path.replace('\\', '/');
            int library = normalized.lastIndexOf(LIBRARY_SUFFIX + "/");
            if (library >= 0 && normalized.endsWith(FOOTPRINT_SUFFIX)) {
                int start = normalized.lastIndexOf('/', library) + 1;
                byRelative.put(normalized.substring(start), text);
            }
        });
        return new KiCadFootprintLibraries(List.of(relative -> Optional.ofNullable(byRelative.get(relative))));
    }

    /** These libraries searched first, then those. */
    public KiCadFootprintLibraries then(KiCadFootprintLibraries more) {
        List<Function<String, Optional<String>>> joined = new ArrayList<>(places);
        joined.addAll(more.places);
        return new KiCadFootprintLibraries(joined);
    }

    /**
     * The footprint a schematic names, {@code LIBRARY:NAME}; empty when no place holds it or the name names no library.
     *
     * @throws IllegalArgumentException when the file found is not a footprint KiCad wrote
     */
    public Optional<KiCadFootprint> find(String libraryIdentifier) {
        int separator = libraryIdentifier == null ? -1 : libraryIdentifier.indexOf(':');
        if (separator <= 0 || separator == libraryIdentifier.length() - 1) {
            return Optional.empty();
        }
        return read.computeIfAbsent(libraryIdentifier, identifier -> {
            String relative = identifier.substring(0, separator) + LIBRARY_SUFFIX + "/" + identifier.substring(separator + 1)
                    + FOOTPRINT_SUFFIX;
            for (Function<String, Optional<String>> place : places) {
                Optional<String> text = place.apply(relative);
                if (text.isPresent()) {
                    try {
                        return Optional.of(KiCadFootprint.of(parser.parse(relative, text.get())));
                    } catch (KiCadParseException unreadable) {
                        throw new IllegalArgumentException("The footprint " + identifier + " does not read: "
                                + unreadable.getMessage(), unreadable);
                    }
                }
            }
            return Optional.empty();
        });
    }
}
