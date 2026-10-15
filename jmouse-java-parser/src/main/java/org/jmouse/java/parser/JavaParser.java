package org.jmouse.java.parser;

import org.jmouse.java.parser.javadoc.JavaDocParser;
import org.jmouse.java.parser.lexer.JavaLexer;
import org.jmouse.java.parser.lexer.JavaSourceText;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.node.CompilationUnitNode;

/**
 * Reads the declaration structure of one Java source file, on jME's lexer, cursor and node contracts.
 *
 * <p>Structural, not a full grammar: it produces the package, imports, every type (nested ones included)
 * and every member with its modifiers, annotations, type parameters, parameters, throws clause,
 * signature and JavaDoc, each with exact source spans and a stable {@link org.jmouse.java.parser.model.SymbolId
 * symbol id}. Method, constructor, initializer and enum-constant bodies are opaque spans; no expression or
 * statement is parsed, so local and anonymous classes inside bodies are not reported.
 *
 * <p>Two views of one result:
 * <ul>
 *     <li>{@link #parseTree} — the jME node tree ({@link CompilationUnitNode} and its children), walked
 *     with a jME {@link org.jmouse.el.node.Visitor}, comments attached as leading trivia;</li>
 *     <li>{@link #parse} — the immutable record projection of that tree, {@link JavaSourceFile}.</li>
 * </ul>
 *
 * <p>Nothing is executed or resolved: no class is loaded, no type name is looked up, no annotation
 * processor runs, no inherited documentation is copied.
 *
 * <p>Parsing never throws for malformed input. A declaration that cannot be read becomes a
 * {@link org.jmouse.java.parser.source.ParseProblem} with its position and is skipped as a whole, up to
 * its semicolon or the end of its body; everything before and after it is still produced. Check
 * {@link JavaSourceFile#isComplete()} before treating a result as the whole file.
 *
 * <p>The accepted syntax is the declaration syntax of Java 21, including records, sealed hierarchies and
 * text blocks. Instances are stateless and safe to share between threads.
 */
public final class JavaParser {

    /**
     * The revision of the parsing algorithm. It changes whenever the same input could produce a
     * different result, so a consumer that freezes results can tell them apart.
     */
    public static final String REVISION = "1";

    private final JavaLexer     lexer         = new JavaLexer();
    private final JavaDocParser javaDocParser = new JavaDocParser();

    /**
     * Parses one compilation unit into the immutable record model.
     *
     * @param sourceName a name for the text — a path or file name — used in nothing but the result
     * @param text       the source exactly as stored; it is never normalised
     * @return the declaration records, with every problem found
     */
    public JavaSourceFile parse(String sourceName, CharSequence text) {
        return parseTree(sourceName, text).toSourceFile();
    }

    /**
     * Parses one compilation unit into a jME node tree.
     *
     * @param sourceName a name for the text — a path or file name
     * @param text       the source exactly as stored; it is never normalised
     * @return the root node, holding every problem found
     */
    public CompilationUnitNode parseTree(String sourceName, CharSequence text) {
        return new StructureParser(lexer.tokenize(new JavaSourceText(sourceName, text)), javaDocParser).parse();
    }
}
