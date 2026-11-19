package org.jmouse.java.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Parses every main source of the sibling {@code jmouse-core} module and requires zero problems.
 * Skipped when the module is not checked out next to this one.
 */
class RealWorldCorpusTest {

    private static final Path CORPUS =
            Path.of(
                    System.getProperty(
                            "jmouse.java.parser.corpus", "../jmouse-core/src/main/java"));

    @Test
    void everyJmouseCoreSourceParsesWithoutAProblem() throws IOException {
        assumeTrue(
                Files.isDirectory(CORPUS),
                "jmouse-core sources are not checked out next to this module.");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(CORPUS)) {
            files = walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }

        JavaParser parser = new JavaParser();
        List<String> failures = new ArrayList<>();
        Map<DeclarationKind, Integer> counts = new EnumMap<>(DeclarationKind.class);
        int javaDocs = 0;
        int lines = 0;
        long started = System.nanoTime();

        for (Path path : files) {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            JavaSourceFile file = parser.parse(path.toString(), text);
            if (!file.isComplete()) {
                failures.add(path + ": " + file.problems());
            }
            for (Declaration declaration : file.declarations()) {
                counts.merge(declaration.kind(), 1, Integer::sum);
                assertTrue(declaration.span().end().offset() <= text.length());
            }
            javaDocs += file.javaDocs().size();
            lines += (int) text.lines().count();
        }

        long elapsedMilliseconds = (System.nanoTime() - started) / 1_000_000;
        System.out.printf(
                "jmouse-core corpus: %d files, %d lines, %d JavaDoc comments, %d ms%n",
                files.size(), lines, javaDocs, elapsedMilliseconds);
        System.out.println("jmouse-core declarations: " + counts);
        assertTrue(files.size() > 100, "Expected the whole jmouse-core source tree.");
        assertEquals(List.of(), failures);
    }
}
