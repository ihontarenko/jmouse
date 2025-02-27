package org.jmouse.template.lexer;

import org.jmouse.template.parser.Parser;

import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Stream.of;

public class SyntaxErrorException extends Error {

    public SyntaxErrorException(Parser parser, Token token, Token.Type... expected) {
        this(parser, token, of(expected).map(Objects::toString).collect(Collectors.joining(", ")));
    }

    public SyntaxErrorException(Parser parser, Token token, String expected) {
        this("[%s] expected type %s, but encountered %s at offset %d".formatted(
                parser.getClass().getSimpleName(), expected, token.value(), token.offset()));
    }

    public SyntaxErrorException(String message) {
        super(message);
    }

}
