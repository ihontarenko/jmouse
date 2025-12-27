package org.jmouse.jdbc.parameters.lexer;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.*;
import org.jmouse.jdbc.parameters.SQLParameterToken;

import java.util.ArrayList;
import java.util.List;

public final class SQLParameterTokenizer implements Tokenizer<TokenizableSource, Token> {

    private final Splitter<List<RawToken>, TokenizableSource> splitter;

    public SQLParameterTokenizer(Splitter<List<RawToken>, TokenizableSource> splitter) {
        this.splitter = splitter;
    }

    public static StringSource source(String name, String sql) {
        return new StringSource(name, sql);
    }

    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<RawToken> raws   = splitter.split(source);
        List<Token>    tokens = new ArrayList<>(raws.size());

        int ordinal = 0;

        for (RawToken raw : raws) {
            SQLParameterToken type  = classify(raw);
            Token             token = new Token(raw.value(), type, ordinal++, raw.offset(), raw.line());

            // the key: store token metadata in StringSource
            source.entry(raw.offset(), raw.length(), token.type());

            tokens.add(token);
        }

        return tokens;
    }

    private SQLParameterToken classify(RawToken raw) {
        // We reuse RawToken.Type categories you already have.
        if (raw.type() == RawToken.Type.STRING) {
            return SQLParameterToken.T_STRING;
        }

        // RawToken.Type.IDENTIFIER is used by splitter for ":name|..."
        if (raw.type() == RawToken.Type.IDENTIFIER) {
            return SQLParameterToken.T_NAMED_PARAMETER;
        }

        // RawToken.Type.OPERATOR used by splitter for '?'
        if (raw.type() == RawToken.Type.OPERATOR && SQLParameterSplitter.Q_MARK_STRING.equals(raw.value())) {
            return SQLParameterToken.T_POSITIONAL_PARAMETER;
        }

        return SQLParameterToken.T_TEXT;
    }
}
