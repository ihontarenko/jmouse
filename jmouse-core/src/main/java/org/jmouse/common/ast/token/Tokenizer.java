package org.jmouse.common.ast.token;

import org.jmouse.common.ast.Pattern;
import org.jmouse.common.ast.recognizer.Recognizer;

import java.util.List;

public interface Tokenizer {

    List<Token.Entry> tokenize(CharSequence sequence);

    void addPattern(Pattern<String> expression);

    void addRecognizer(Recognizer<Token, String> recognizer);

    default Token.Entry entry(Token token, String value, final int position, final int ordinal) {
        return Token.Entry.of(token, value, position, ordinal);
    }

}
