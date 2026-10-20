package org.jmouse.parallax.script;

import org.jmouse.el.StringSource;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.SyntaxErrorException;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.parallax.script.reader.DocumentReader;
import org.jmouse.script.el.ScriptExtension;
import org.jmouse.script.el.lexer.ScriptRecognizer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * jMX read by the engine's parsers says exactly what the hand-written reader said: for every document of the catalogue
 * the same tree, every expression cut out at the same line, column and offset; for every broken variant of one, the
 * same refusal at the same place.
 *
 * <p>The documents are this module's catalogue and test resources, and every directory named in the system property
 * {@code jmx.catalogues} (separated by {@code ;}) — a product's catalogue lives outside this repository.</p>
 */
class JmxReadingEquivalenceTest {

    private static final String EXTRA_CATALOGUES = "jmx.catalogues";

    /** Every this-many-th line of a document is deleted, one at a time, and its first word replaced. */
    private static final int MUTATION_STRIDE = 7;
    private static final int MUTATIONS_PER_DOCUMENT = 12;

    @Test
    void everyDocumentReadsTheSame() {
        Map<String, String> documents = documents();
        assertFalse(documents.isEmpty());
        List<String> differing = new ArrayList<>();
        documents.forEach((name, text) -> {
            if (!DocumentSnapshot.outcome(JmxReadingEquivalenceTest::byHand, text, name)
                    .equals(DocumentSnapshot.outcome(ExperienceReader::read, text, name))) {
                differing.add(name);
            }
        });
        assertEquals(List.of(), differing, () -> firstDifference(documents, differing));
    }

    @Test
    void everyBrokenDocumentIsRefusedTheSame() {
        List<String> differing = new ArrayList<>();
        documents().forEach((name, text) -> {
            for (String variant : variants(text)) {
                if (!DocumentSnapshot.outcome(JmxReadingEquivalenceTest::byHand, variant, name)
                        .equals(DocumentSnapshot.outcome(ExperienceReader::read, variant, name))) {
                    differing.add(name + "\n" + variant);
                    return;
                }
            }
        });
        assertEquals(List.of(), differing.stream().map(entry -> entry.substring(0, entry.indexOf('\n'))).toList(),
                () -> differing.isEmpty() ? "" : describe(differing.getFirst()));
    }

    /** The hand-written reader, as ExperienceReader read with it before the engine's parsers. */
    private static List<ExperienceDocument> byHand(String source, String documentName) {
        ExperienceNotation.Normalized normalized = ExperienceNotation.normalize(source);
        TokenCursor cursor;
        try {
            cursor = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ScriptRecognizer()))
                    .tokenize(new StringSource(documentName, normalized.source()));
        } catch (SyntaxErrorException failure) {
            throw new ExperienceParseException(documentName, 0, 0, failure.getMessage());
        }
        StandardExtensionContainer extensions = new StandardExtensionContainer();
        extensions.importExtension(new ScriptExtension());
        extensions.importExtension(new ElectricalTypes());
        return new DocumentReader(cursor, new DefaultParserContext(extensions), normalized, documentName).documents();
    }

    /** Broken variants of a text: its last brace gone, and one line at a time deleted or begun with a stray word. */
    private static List<String> variants(String text) {
        List<String> variants = new ArrayList<>();
        int lastBrace = text.lastIndexOf('}');
        if (lastBrace >= 0) {
            variants.add(text.substring(0, lastBrace) + text.substring(lastBrace + 1));
        }
        String[] lines = text.split("\n", -1);
        int made = 0;
        for (int index = MUTATION_STRIDE / 2; index < lines.length && made < MUTATIONS_PER_DOCUMENT; index += MUTATION_STRIDE) {
            if (lines[index].isBlank()) {
                continue;
            }
            List<String> deleted = new ArrayList<>(List.of(lines));
            deleted.remove(index);
            variants.add(String.join("\n", deleted));
            List<String> stray = new ArrayList<>(List.of(lines));
            stray.set(index, lines[index].replaceFirst("^(\\s*)\\S+", "$1zzz"));
            variants.add(String.join("\n", stray));
            made++;
        }
        return variants;
    }

    private static Map<String, String> documents() {
        List<Path> roots = new ArrayList<>(List.of(Path.of("src/main/resources/catalogue"), Path.of("src/test/resources")));
        String extra = System.getProperty(EXTRA_CATALOGUES, "");
        for (String root : extra.split(";")) {
            if (!root.isBlank()) {
                roots.add(Path.of(root.strip()));
            }
        }
        Map<String, String> documents = new LinkedHashMap<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".jmx")).sorted().toList()) {
                    documents.put(file.toString().replace('\\', '/'), Files.readString(file, StandardCharsets.UTF_8));
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException(unreadable);
            }
        }
        return documents;
    }

    private static String firstDifference(Map<String, String> documents, List<String> differing) {
        if (differing.isEmpty()) {
            return "";
        }
        String name = differing.getFirst();
        return describe(name + "\n" + documents.get(name));
    }

    /** Where the two readers first part, for a document given as its name and text. */
    private static String describe(String nameAndText) {
        int split = nameAndText.indexOf('\n');
        String name = nameAndText.substring(0, split);
        String text = nameAndText.substring(split + 1);
        String[] expected = DocumentSnapshot.outcome(JmxReadingEquivalenceTest::byHand, text, name).split("\n");
        String[] actual = DocumentSnapshot.outcome(ExperienceReader::read, text, name).split("\n");
        for (int index = 0; index < Math.max(expected.length, actual.length); index++) {
            String one = index < expected.length ? expected[index] : "<end>";
            String other = index < actual.length ? actual[index] : "<end>";
            if (!one.equals(other)) {
                return name + ", snapshot line " + (index + 1) + ":\n  by hand: " + one + "\n  engine:  " + other;
            }
        }
        return name;
    }
}
