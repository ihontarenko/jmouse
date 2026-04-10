package org.jmouse.jdbc.parameters;

import org.jmouse.core.Verify;
import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.jdbc.parameters.compile.SQLPlanCompiler;
import org.jmouse.jdbc.parameters.lexer.SQLParameterTokenizer;

import java.util.List;

/**
 * High-level processor for SQL parameter handling.
 *
 * <p>Coordinates tokenization and compilation phases:
 * parse → tokenize → compile into {@link SQLPlan}.</p>
 */
public final class SQLParameterProcessor {

    private final SQLParameterTokenizer tokenizer;
    private final SQLPlanCompiler       compiler;

    /**
     * Creates a processor with required tokenizer and compiler.
     *
     * @param tokenizer the SQL parameter tokenizer
     * @param compiler the SQL plan compiler
     */
    public SQLParameterProcessor(SQLParameterTokenizer tokenizer, SQLPlanCompiler compiler) {
        this.compiler = Verify.nonNull(compiler, "compiler");
        this.tokenizer = Verify.nonNull(tokenizer, "tokenizer");
    }

    /**
     * Parses raw SQL into a tokenized representation.
     *
     * <p>Produces {@link SQLParsed} containing the source and detected parameter tokens.</p>
     *
     * @param name logical SQL name or identifier
     * @param sql raw SQL string
     * @return parsed SQL structure
     */
    public SQLParsed parse(String name, String sql) {
        StringSource source = SQLParameterTokenizer.source(name, sql);
        List<Token>  tokens = tokenizer.tokenize(source);
        return new SQLParsed(name, source, List.copyOf(tokens));
    }

    /**
     * Compiles parsed SQL into an executable plan.
     *
     * <p>Rewrites parameters into JDBC placeholders and produces binding metadata.</p>
     *
     * @param parsed the parsed SQL
     * @return compiled SQL representation
     */
    public SQLCompiled compile(SQLParsed parsed) {
        return new SQLCompiled(compiler.compile(parsed.source()));
    }

}