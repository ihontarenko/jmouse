package org.jmouse.datamap;

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
class DataMapExamplesTest {

    private static final String EXAMPLES = "jmd/examples";

    @Test
    void everyExampleWritesBackAsItsFile() throws IOException, URISyntaxException {
        Map<String, String> examples = examples();
        assertFalse(examples.isEmpty());
        examples.forEach((path, text) -> {
            DataMap dataMap = DataMapReader.read(path, text);
            // The examples are written as the writer writes, so reading one and writing it back gives the file itself,
            // its comments included.
            assertEquals(text.replace("\r\n", "\n"), DataMapWriter.write(dataMap), path);
        });
    }

    @Test
    void examplesLinkOnlyToWhatExists() throws IOException, URISyntaxException {
        DataMapLibrary library = DataMapLibrary.read(examples());
        assertEquals(List.of(), library.brokenLinks());

        DataMapEntry levels = library.document("parts/74hc595.jmd").orElseThrow().entry("levels").orElseThrow();
        DataMapValue.Link highInput = (DataMapValue.Link) levels.value("high-input-minimum").orElseThrow();
        DataMapField resolved = library.field("parts/74hc595.jmd", highInput).orElseThrow();
        assertEquals(3.15, ((DataMapValue.Measure) resolved.value()).quantity().value(), 1e-12);
    }

    @Test
    void theLibraryMapNamesSymbolsAndPins() throws IOException, URISyntaxException {
        DataMap map = DataMapLibrary.read(examples()).document("kicad/library-map.jmd").orElseThrow();
        DataMapEntry diode = map.allEntries().stream()
                .filter(entry -> entry.value("symbol").equals(Optional.of(new DataMapValue.Text("Device:D"))))
                .findFirst().orElseThrow();
        assertEquals(new DataMapValue.Word("1n4148"), diode.value("component").orElseThrow());
        DataMapValue.Sequence pins = assertInstanceOf(DataMapValue.Sequence.class, diode.value("pins").orElseThrow());
        assertTrue(pins.items().contains(new DataMapValue.Assignment("1", new DataMapValue.Word("k"))));
    }

    /** Every {@code .jmd} under the examples, by its path relative to them. */
    private static Map<String, String> examples() throws IOException, URISyntaxException {
        Path root = Path.of(DataMapExamplesTest.class.getClassLoader().getResource(EXAMPLES).toURI());
        Map<String, String> examples = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".jmd")).sorted().toList()) {
                examples.put(root.relativize(file).toString().replace('\\', '/'), Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        return examples;
    }
}
