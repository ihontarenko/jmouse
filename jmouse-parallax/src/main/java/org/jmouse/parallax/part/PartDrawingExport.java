package org.jmouse.parallax.part;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Our library's drawings for every product that shows a part: each symbol and footprint of the catalogue as the JSON
 * the server already hands an interface — read from its jMX, the one source —, and an index of them all. The package
 * {@code @jmouse/part-drawings} draws these as SVG, in a page or at its own build.
 *
 * <pre>{@code
 * java … org.jmouse.parallax.part.PartDrawingExport <catalogue/electronics> <jmouse-ui/packages/part-drawings/data>
 * }</pre>
 *
 * Writes {@code index.json} — slug, kind, title, summary, family, licence, and a symbol's pins — and
 * {@code drawings/<slug>.json}, the drawing itself.
 */
public final class PartDrawingExport {

    /** The folders the drawings are taken from, and the kind each holds. */
    private static final List<String> KINDS = List.of("symbols", "footprints");

    private static final ObjectMapper JSON = new ObjectMapper();

    private PartDrawingExport() {
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Name the catalogue's electronics folder and the folder to write into");
        }
        Path catalogue = Path.of(arguments[0]);
        Path output = Path.of(arguments[1]);
        Files.createDirectories(output.resolve("drawings"));
        ArrayNode index = JSON.createArrayNode();
        for (String kind : KINDS) {
            Path folder = catalogue.resolve(kind);
            try (Stream<Path> files = Files.walk(folder)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".jmx")).sorted().toList()) {
                    index.add(export(folder, file, kind, output));
                }
            }
        }
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("index.json").toFile(), index);
        System.out.println(index.size() + " drawings written under " + output.toAbsolutePath());
    }

    private static ObjectNode export(Path folder, Path file, String kind, Path output) {
        try {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            ExperienceDocument document = ExperienceReader.readOne(source, file.getFileName().toString());
            ObjectDefinition definition = ExperienceDefinitions.read(source, file.getFileName().toString()).definition();
            String drawing = switch (definition) {
                case ObjectDefinition.Symbol symbol -> symbol.drawing();
                case ObjectDefinition.Footprint footprint -> footprint.drawing();
                default -> throw new IllegalArgumentException(file + " is neither a symbol nor a footprint");
            };
            JsonNode tree = JSON.readTree(drawing);
            Files.writeString(output.resolve("drawings").resolve(document.slug() + ".json"), JSON.writeValueAsString(tree),
                    StandardCharsets.UTF_8);

            ObjectNode entry = JSON.createObjectNode();
            entry.put("slug", document.slug());
            entry.put("kind", kind.substring(0, kind.length() - 1));
            entry.put("title", document.title());
            entry.put("summary", document.meta().summary());
            Path family = folder.relativize(file.getParent());
            entry.put("family", family.toString().replace('\\', '/'));
            entry.put("license", document.meta().license());
            if (tree.has("pins")) {
                ArrayNode pins = entry.putArray("pins");
                tree.get("pins").forEach(pin -> pins.add(pin.path("name").asText()));
            }
            if (tree.has("pads")) {
                entry.put("pads", tree.get("pads").size());
            }
            return entry;
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The drawing " + file + " cannot be exported", unreadable);
        }
    }
}
