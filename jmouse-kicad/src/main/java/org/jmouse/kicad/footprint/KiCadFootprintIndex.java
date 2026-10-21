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
import java.util.stream.Stream;

/**
 * Which footprints the libraries in some folders hold, by name alone: each {@code LIBRARY.pretty} folder's
 * {@code NAME.kicad_mod} files, listed without reading them. KiCad ships some fifteen thousand; a footprint is read —
 * {@link #read(Entry)} — only when it is asked about.
 */
public final class KiCadFootprintIndex {

    private static final String LIBRARY_SUFFIX = ".pretty";
    private static final String FOOTPRINT_SUFFIX = ".kicad_mod";

    private KiCadFootprintIndex() {
    }

    /**
     * One footprint as an index knows it.
     *
     * @param library the library's name, its folder's without {@code .pretty}
     * @param name    the footprint's name, its file's without {@code .kicad_mod}
     * @param file    where it is
     */
    public record Entry(String library, String name, Path file) {

        /** {@code Package_TO_SOT_THT:TO-220-3_Vertical}: how a schematic names the footprint. */
        public String identifier() {
            return library + ":" + name;
        }
    }

    /** Every footprint of the libraries in these folders, folder by folder; a folder that does not exist holds none. */
    public static List<Entry> list(List<Path> folders) {
        List<Entry> entries = new ArrayList<>();
        for (Path folder : folders) {
            if (!Files.isDirectory(folder)) {
                continue;
            }
            try (Stream<Path> libraries = Files.list(folder)) {
                for (Path library : libraries.filter(KiCadFootprintIndex::isLibrary).sorted().toList()) {
                    entries.addAll(footprintsOf(library));
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException("The footprint libraries in " + folder + " cannot be listed", unreadable);
            }
        }
        return entries;
    }

    /**
     * The footprint an entry names, read.
     *
     * @throws IllegalArgumentException when the file is not a footprint KiCad wrote
     */
    public static KiCadFootprint read(Entry entry) {
        try {
            String text = Files.readString(entry.file(), StandardCharsets.UTF_8);
            return KiCadFootprint.of(new KiCadParser().parse(entry.identifier(), text));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The footprint " + entry.file() + " cannot be read", unreadable);
        } catch (KiCadParseException unreadable) {
            throw new IllegalArgumentException("The footprint " + entry.identifier() + " does not read: "
                    + unreadable.getMessage(), unreadable);
        }
    }

    private static boolean isLibrary(Path path) {
        return Files.isDirectory(path) && path.getFileName().toString().endsWith(LIBRARY_SUFFIX);
    }

    private static List<Entry> footprintsOf(Path library) throws IOException {
        String folder = library.getFileName().toString();
        String libraryName = folder.substring(0, folder.length() - LIBRARY_SUFFIX.length());
        try (Stream<Path> files = Files.list(library)) {
            return files.filter(file -> file.getFileName().toString().endsWith(FOOTPRINT_SUFFIX)).sorted()
                    .map(file -> {
                        String fileName = file.getFileName().toString();
                        return new Entry(libraryName, fileName.substring(0, fileName.length() - FOOTPRINT_SUFFIX.length()),
                                file);
                    }).toList();
        }
    }
}
