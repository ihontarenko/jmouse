package org.jmouse.java.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JavaSyntaxCorpusTest {
    @ParameterizedTest
    @ValueSource(strings = {"src/main/java", "../jmouse-core/src/main/java", "../jmouse-el/src/main/java",
            "../jmouse-el-grammar/src/main/java", "../jmouse-formula/src/main/java", "../jmouse-java-index/src/main/java"})
    void sourceParserReadsRealJavaIncludingEveryExecutableRegion(String directory) throws IOException {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        Path root = Path.of(directory);
        assumeTrue(Files.isDirectory(root), "The sibling source module is not checked out");
        List<Path> paths;
        try (Stream<Path> sources = Files.walk(root)) {
            paths = sources.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        assertFalse(paths.isEmpty());
        List<String> failures = new ArrayList<>();
        for (Path path : paths) {
            try {
                parser.parseCompilationUnit(path.toString(), Files.readString(path));
            } catch (JavaSyntaxException exception) {
                failures.add(path + ":" + exception.span().start() + ": " + exception.getMessage());
            }
        }
        assertEquals(List.of(), failures);
    }
}
