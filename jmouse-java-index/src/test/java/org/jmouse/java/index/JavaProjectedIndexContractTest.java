package org.jmouse.java.index;

import org.jmouse.java.parser.JavaSyntaxParser;
import org.jmouse.java.parser.projection.JavaExecutableProjection;

/** Existing index behavior must also hold when executable metadata comes from the full AST. */
class JavaProjectedIndexContractTest extends JavaIndexContractTest {
    private final JavaSyntaxParser syntax = new JavaSyntaxParser();

    @Override
    protected FileIndex file(String source, String text) {
        return FileIndex.of(JavaExecutableProjection.project(syntax.recoverCompilationUnit(source, text)));
    }
}
