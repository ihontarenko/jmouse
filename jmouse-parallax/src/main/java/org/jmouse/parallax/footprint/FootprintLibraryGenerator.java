package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMap;
import org.jmouse.datamap.DataMapReader;
import org.jmouse.datamap.DataMapValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Our footprint library, generated: every package table under {@code footprint-standards/} worked out by the land
 * pattern rules, each footprint written as a jMX document into its folder. The documents are committed — the catalogue
 * ships them as it ships a drawn one — and regenerated, never edited.
 *
 * <pre>{@code
 * java … org.jmouse.parallax.footprint.FootprintLibraryGenerator jmouse-parallax/src/main/resources/catalogue/electronics/footprints
 * }</pre>
 */
public final class FootprintLibraryGenerator {

    /** The package tables, in the order their footprints are written. */
    static final List<String> TABLES = List.of("chip-packages.jmd", "small-outline-packages.jmd",
            "dual-inline-packages.jmd", "header-packages.jmd", "axial-packages.jmd", "radial-packages.jmd",
            "inline-packages.jmd", "hole-packages.jmd");

    private static final String FOLDER = "footprint-standards/";

    private FootprintLibraryGenerator() {
    }

    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Name the footprints folder to write into");
        }
        Path root = Path.of(arguments[0]);
        List<StandardFootprint> footprints = generate();
        for (StandardFootprint footprint : footprints) {
            write(root.resolve(footprint.folder()).resolve(footprint.slug() + ".jmx"), footprint.document());
        }
        System.out.println(footprints.size() + " footprints written under " + root.toAbsolutePath());
    }

    /** Every footprint the tables list. */
    public static List<StandardFootprint> generate() {
        LandPatternRules rules = LandPatternRules.of(read("land-pattern-rules.jmd"));
        List<StandardFootprint> footprints = new ArrayList<>();
        for (String name : TABLES) {
            DataMap table = read(name);
            String form = table.field("form").map(field -> field.value() instanceof DataMapValue.Word(String word) ? word : "")
                    .orElseThrow(() -> new IllegalArgumentException("The package table " + name + " names no form"));
            FootprintFamily family = FootprintFamily.of(form)
                    .orElseThrow(() -> new IllegalArgumentException("No footprint family draws the form '" + form + "'"));
            footprints.addAll(family.footprints(table, rules));
        }
        return footprints;
    }

    private static DataMap read(String name) {
        try (InputStream stream = FootprintLibraryGenerator.class.getClassLoader().getResourceAsStream(FOLDER + name)) {
            if (stream == null) {
                throw new IllegalArgumentException("The package table " + FOLDER + name + " is not on the classpath");
            }
            return DataMapReader.read(name, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static void write(Path file, String document) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, document, StandardCharsets.UTF_8);
        } catch (IOException unwritable) {
            throw new UncheckedIOException("The footprint " + file + " cannot be written", unwritable);
        }
    }
}
