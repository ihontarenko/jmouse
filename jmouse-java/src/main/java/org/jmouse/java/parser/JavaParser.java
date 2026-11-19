package org.jmouse.java.parser;

import org.jmouse.java.parser.grammar.JavaParsingMode;
import org.jmouse.java.parser.grammar.JavaRecoveryPolicy;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.jmouse.java.parser.projection.JavaDeclarationProjection;
import org.jmouse.java.parser.projection.JavaExecutableProjection;

import java.util.Objects;

/**
 * Java source facade over one configurable syntax grammar and its immutable navigation projections.
 * Malformed source produces positioned diagnostics and the declarations that could be retained.
 * Declaration requests explicitly retain coverage for unread bodies; executable requests read full
 * syntax.
 */
public final class JavaParser {
    public static final String REVISION = "5";
    private final JavaSyntaxParser syntax;

    public JavaParser() {
        this(new JavaSyntaxParser());
    }

    public JavaParser(JavaSyntaxParser syntax) {
        this.syntax = Objects.requireNonNull(syntax, "syntax");
    }

    public enum Level {
        DECLARATIONS,
        EXPRESSIONS
    }

    public JavaSourceFile parse(String sourceName, CharSequence text) {
        return parse(sourceName, text, Level.DECLARATIONS);
    }

    public JavaSourceFile parse(String sourceName, CharSequence text, Level level) {
        JavaSyntaxResult<JavaCompilationUnitNode> result = parseSyntax(sourceName, text, level);
        return level == Level.EXPRESSIONS
                ? JavaExecutableProjection.project(result)
                : JavaDeclarationProjection.project(result);
    }

    /**
     * Full managed AST. Source, problems and coverage are available through {@link #parseSyntax}.
     */
    public JavaCompilationUnitNode parseTree(String sourceName, CharSequence text) {
        return parseSyntax(sourceName, text, Level.EXPRESSIONS).root();
    }

    public JavaSyntaxResult<JavaCompilationUnitNode> parseSyntax(
            String sourceName, CharSequence text) {
        return parseSyntax(sourceName, text, Level.EXPRESSIONS);
    }

    public JavaSyntaxResult<JavaCompilationUnitNode> parseSyntax(
            String sourceName, CharSequence text, Level level) {
        Objects.requireNonNull(level, "level");
        JavaParsingMode mode =
                level == Level.DECLARATIONS
                        ? JavaParsingMode.DECLARATIONS_ONLY
                        : JavaParsingMode.COMPLETE;
        return syntax.parseSource(
                JavaUnitRoles.COMPILATION_UNIT, sourceName, text, mode, JavaRecoveryPolicy.RECOVER);
    }
}
