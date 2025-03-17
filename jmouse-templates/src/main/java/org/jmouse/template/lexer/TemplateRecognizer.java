package org.jmouse.template.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.recognizer.Recognizer;
import org.jmouse.template.TemplateToken;

import java.util.Optional;

public class TemplateRecognizer implements Recognizer<Token.Type, RawToken> {

    @Override
    public Optional<Token.Type> recognize(RawToken subject) {
        return subject.type() == RawToken.Type.RAW_TEXT ? Optional.of(TemplateToken.T_RAW_TEXT) : Optional.empty();
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
