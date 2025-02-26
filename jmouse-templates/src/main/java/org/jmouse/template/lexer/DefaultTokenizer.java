package org.jmouse.template.lexer;

import org.jmouse.template.lexer.recognizer.EnumTokenRecognizer;

import java.util.ArrayList;
import java.util.List;

public class DefaultTokenizer implements Tokenizer {

    private final RawSplitter                        splitter   = new RawSplitter();
    private final EnumTokenRecognizer<StandardToken> recognizer = new EnumTokenRecognizer<>(StandardToken.class, 1);

    private static final List<RawToken.Type> RECOGNIZABLE = new ArrayList<>();

    static {
        RECOGNIZABLE.add(RawToken.Type.OPERATOR);
        RECOGNIZABLE.add(RawToken.Type.IDENTIFIER);
    }

    @Override
    public List<Token> tokenize(CharSequence text) {
        List<Token>    tokens    = new ArrayList<>();
        List<RawToken> rawTokens = splitter.split(text, 0, text.length());

        for (RawToken rawToken : rawTokens) {

            if (RECOGNIZABLE.contains(rawToken.type())) {
                System.out.println("Recognized: " + recognizer.recognize(rawToken.token()));
            }
        }

        return tokens;
    }

}
