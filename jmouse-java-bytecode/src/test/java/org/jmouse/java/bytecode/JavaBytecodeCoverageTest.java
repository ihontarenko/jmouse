package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

class JavaBytecodeCoverageTest {
    @Test
    void declarationsOnlyMarkEveryOmittedCodeRegionAndCannotBecomeComplete() throws Exception {
        byte[] bytes;
        try (InputStream input =
                JavaBytecodeParserTest.Fixture.class.getResourceAsStream(
                        "JavaBytecodeParserTest$Fixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaBytecodeParser parser = new JavaBytecodeParser();
        JavaBytecodeResult declarations =
                parser.parse("Fixture.class", bytes, JavaBytecodeMode.DECLARATIONS_ONLY);
        assertFalse(declarations.isComplete());
        assertThrows(IllegalStateException.class, declarations::requireComplete);
        declarations.root().methods().stream()
                .flatMap(method -> method.attributes().stream())
                .filter(JavaCodeAttributeNode.class::isInstance)
                .map(JavaCodeAttributeNode.class::cast)
                .forEach(
                        code -> {
                            assertEquals(JavaCodeCoverage.OMITTED, code.coverage());
                            assertTrue(code.instructions().isEmpty());
                            assertTrue(code.code().length() > 0);
                        });
        parser.parse("Fixture.class", bytes).requireComplete().methods().stream()
                .flatMap(method -> method.attributes().stream())
                .filter(JavaCodeAttributeNode.class::isInstance)
                .map(JavaCodeAttributeNode.class::cast)
                .forEach(code -> assertEquals(JavaCodeCoverage.COMPLETE, code.coverage()));
    }
}
