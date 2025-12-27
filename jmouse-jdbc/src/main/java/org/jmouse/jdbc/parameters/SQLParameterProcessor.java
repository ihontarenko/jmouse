package org.jmouse.jdbc.parameters;

import org.jmouse.core.Contract;
import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.jdbc.parameters.compile.SQLPlanCompiler;
import org.jmouse.jdbc.parameters.lexer.SQLParameterTokenizer;

import java.util.List;

public final class SQLParameterProcessor {

    private final SQLParameterTokenizer tokenizer;
    private final SQLPlanCompiler       compiler;

    public SQLParameterProcessor(SQLParameterTokenizer tokenizer, SQLPlanCompiler compiler) {
        this.compiler = Contract.nonNull(compiler, "compiler");
        this.tokenizer = Contract.nonNull(tokenizer, "tokenizer");
    }

    public SQLParsed parse(String name, String sql) {
        StringSource source = SQLParameterTokenizer.source(name, sql);
        List<Token> tokens = tokenizer.tokenize(source);
        return new SQLParsed(name, source, List.copyOf(tokens));
    }

    public SQLCompiled compile(SQLParsed parsed) {
        return new SQLCompiled(compiler.compile(parsed.source()));
    }

}
