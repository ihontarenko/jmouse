package org.jmouse.java.parser;

import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.projection.JavaDeclarationProjection;
import org.jmouse.java.parser.model.JavaSourceFile;

/** The same public record contract is exercised through the new full-syntax projection. */
class JavaProjectedDeclarationsTest extends JavaParserDeclarationsTest {
    @Override
    protected JavaSourceFile read(String sourceName, CharSequence text) {
        return JavaDeclarationProjection.project(new JavaSyntaxParser()
                .parseSource(JavaUnitRoles.COMPILATION_UNIT, sourceName, text));
    }
}
