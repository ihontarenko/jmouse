package org.jmouse.el.renderable.lexer;

import org.jmouse.el.core.lexer.BasicToken;
import org.jmouse.el.core.lexer.RawToken;
import org.jmouse.el.core.lexer.Token;
import org.jmouse.el.core.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.core.lexer.recognizer.EnumTokenRecognizer;
import org.jmouse.el.core.lexer.recognizer.Recognizer;

import java.util.Optional;

public class TemplateRecognizer extends CompositeRecognizer {

    public TemplateRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 2000));
        addRecognizer(new EnumTokenRecognizer<>(TemplateToken.class, 1000));
        addRecognizer(new RawTextRecognizer());
    }

    @Override
    public Optional<Token.Type> recognize(RawToken subject) {
        return super.recognize(subject);
    }

    @Override
    public int getOrder() {
        return -10;
    }

    static class RawTextRecognizer implements Recognizer<Token.Type, RawToken> {

        @Override
        public Optional<Token.Type> recognize(RawToken subject) {
            return subject.type() == RawToken.Type.RAW_TEXT ? Optional.of(TemplateToken.T_RAW_TEXT) : Optional.empty();
        }

        @Override
        public int getOrder() {
            return 10;
        }

    }

}
