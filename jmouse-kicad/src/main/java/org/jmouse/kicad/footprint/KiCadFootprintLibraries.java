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

    /**
     * Where a footprint was found, which says whose it is: KiCad's installed libraries — the KiCad Libraries Team's,
     * under their licence — or a project's own, whose holder and terms are the project's.
     */
    public enum Origin {
        INSTALLED, PROJECT
    }

    /** A place searched, and whose footprints it holds. */
    private record Place(Origin origin, Function<String, Optional<String>> files) {
    }

    /** A footprint as found, and where. */
    private record Found(KiCadFootprint footprint, Origin origin) {
    }

    private static final String LIBRARY_SUFFIX = ".pretty";
    private static final String FOOTPRINT_SUFFIX = ".kicad_mod";

    private final List<Place> places;
    private final Map<String, Optional<Found>> read = new ConcurrentHashMap<>();
    private final KiCadParser parser = new KiCadParser();

    private KiCadFootprintLibraries(List<Place> places) {
        this.places = List.copyOf(places);
    }

    /**
     * KiCad's installed libraries in these folders: each holds {@code LIBRARY.pretty} folders. A folder that does not
     * exist holds none.
     */
    public static KiCadFootprintLibraries in(List<Path> folders) {
        List<Place> places = new ArrayList<>();
        for (Path folder : folders) {
            places.add(new Place(Origin.INSTALLED, relative -> {
                Path file = folder.resolve(relative);
                if (!Files.isRegularFile(file)) {
                    return Optional.empty();
                }
                try {
                    return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
                } catch (IOException unreadable) {
                    throw new UncheckedIOException("The footprint " + file + " cannot be read", unreadable);
                }
            }));
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
        return new KiCadFootprintLibraries(List.of(new Place(Origin.PROJECT, relative -> Optional.ofNullable(byRelative.get(relative)))));
    }

    /** These libraries searched first, then those. */
    public KiCadFootprintLibraries then(KiCadFootprintLibraries more) {
        List<Place> joined = new ArrayList<>(places);
        joined.addAll(more.places);
        return new KiCadFootprintLibraries(joined);
    }

    /**
     * The footprint a schematic names, {@code LIBRARY:NAME}; empty when no place holds it or the name names no library.
     *
     * @throws IllegalArgumentException when the file found is not a footprint KiCad wrote
     */
    public Optional<KiCadFootprint> find(String libraryIdentifier) {
        return located(libraryIdentifier).map(Found::footprint);
    }

    /** Where the footprint a schematic names was found — whose it is —; empty when no place holds it. */
    public Optional<Origin> originOf(String libraryIdentifier) {
        return located(libraryIdentifier).map(Found::origin);
    }

    private Optional<Found> located(String libraryIdentifier) {
        int separator = libraryIdentifier == null ? -1 : libraryIdentifier.indexOf(':');
        if (separator <= 0 || separator == libraryIdentifier.length() - 1) {
            return Optional.empty();
        }
        return read.computeIfAbsent(libraryIdentifier, identifier -> {
            String relative = identifier.substring(0, separator) + LIBRARY_SUFFIX + "/" + identifier.substring(separator + 1)
                    + FOOTPRINT_SUFFIX;
            for (Place place : places) {
                Optional<String> text = place.files().apply(relative);
                if (text.isPresent()) {
                    try {
                        return Optional.of(new Found(KiCadFootprint.of(parser.parse(relative, text.get())), place.origin()));
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
