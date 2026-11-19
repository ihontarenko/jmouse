package org.jmouse.java.bytecode.signature;

import org.jmouse.el.Source;
import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Descriptor/signature punctuation is contextual: a letter may also occur inside an identifier. */
public final class JvmTextLexer implements Lexer {
    private static final Map<Integer, Token.Type> TERMINALS = terminals();

    @Override
    public TokenCursor tokenize(CharSequence text) {
        String name = text instanceof Source source ? source.getName() : JvmTextNames.SOURCE;
        StringSource source = new StringSource(name, text.toString());
        List<Token> tokens = new ArrayList<>(source.length() + 1);
        for (int offset = 0; offset < source.length(); offset++) {
            Token.Type type =
                    TERMINALS.getOrDefault((int) source.charAt(offset), JvmTextToken.CHARACTER);
            source.entry(offset, 1, type);
            tokens.add(
                    new Token(
                            Character.toString(source.charAt(offset)),
                            type,
                            tokens.size(),
                            offset,
                            source.getLineNumber(offset)));
        }
        source.entry(source.length(), 0, JvmTextToken.END);
        tokens.add(
                new Token(
                        "",
                        JvmTextToken.END,
                        tokens.size(),
                        source.length(),
                        source.getLineNumber(source.length())));
        return new Cursor(source, tokens);
    }

    private static Map<Integer, Token.Type> terminals() {
        Map<Integer, Token.Type> terminals = new HashMap<>();
        for (JvmSignatureToken terminal : JvmSignatureToken.values()) {
            terminals.put(terminal.code(), terminal);
        }
        return Map.copyOf(terminals);
    }
}
