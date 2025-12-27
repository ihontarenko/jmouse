package org.jmouse.jdbc.parameters;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;

import java.util.List;

public record SQLParsed(String name, StringSource source, List<Token> tokens) {

    public String original() {
        return source.toString();
    }

}