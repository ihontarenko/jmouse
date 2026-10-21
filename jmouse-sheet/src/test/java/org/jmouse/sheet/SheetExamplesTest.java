package org.jmouse.sheet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The examples shipped in {@code jmd/examples/} read, write back the same, and link only to what exists. */
class SheetExamplesTest {

    private static final String EXAMPLES = "sheet/examples";

    @Test
    void everyExampleWritesBackAsItsFile() throws IOException, URISyntaxException {
        Map<String, String> examples = examples();
        assertFalse(examples.isEmpty());
        examples.forEach((path, text) -> {
            Sheet sheet = SheetReader.read(path, text);
            // The examples are written as the writer writes, so reading one and writing it back gives the file itself,
            // its comments included.
            assertEquals(text.replace("\r\n", "\n"), SheetWriter.write(sheet), path);
        });
    }

    @Test
    void examplesLinkOnlyToWhatExists() throws IOException, URISyntaxException {
        SheetLibrary library = SheetLibrary.read(examples());
        assertEquals(List.of(), library.brokenLinks());

        SheetEntry levels = library.document("parts/74hc595.sht").orElseThrow().entry("levels").orElseThrow();
        SheetValue.Link highInput = (SheetValue.Link) levels.value("high-input-minimum").orElseThrow();
        SheetField resolved = library.field("parts/74hc595.sht", highInput).orElseThrow();
        assertEquals(3.15, ((SheetValue.Measure) resolved.value()).quantity().value(), 1e-12);
    }

    @Test
    void theLibraryMapNamesSymbolsAndPins() throws IOException, URISyntaxException {
        Sheet map = SheetLibrary.read(examples()).document("kicad/library-map.sht").orElseThrow();
        SheetEntry diode = map.allEntries().stream()
                .filter(entry -> entry.value("symbol").equals(Optional.of(new SheetValue.Text("Device:D"))))
                .findFirst().orElseThrow();
        assertEquals(new SheetValue.Word("1n4148"), diode.value("component").orElseThrow());
        SheetValue.Sequence pins = assertInstanceOf(SheetValue.Sequence.class, diode.value("pins").orElseThrow());
        assertTrue(pins.items().contains(new SheetValue.Assignment("1", new SheetValue.Word("k"))));
    }

    /** Every {@code .sht} under the examples, by its path relative to them. */
    private static Map<String, String> examples() throws IOException, URISyntaxException {
        Path root = Path.of(SheetExamplesTest.class.getClassLoader().getResource(EXAMPLES).toURI());
        Map<String, String> examples = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".sht")).sorted().toList()) {
                examples.put(root.relativize(file).toString().replace('\\', '/'), Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        return examples;
    }
}
