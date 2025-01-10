package svit.ast.parser;

import svit.ast.token.Token;

import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Stream.of;

public class SyntaxErrorException extends Error {

    public SyntaxErrorException(Parser parser, Token.Entry entry, Token... expected) {
        this(parser, entry, of(expected).map(Objects::toString).collect(Collectors.joining(", ")));
    }

    public SyntaxErrorException(Parser parser, Token.Entry entry, String expected) {
        this("[%s] expected token %s, but encountered %s at position %d".formatted(
                parser.getClass().getSimpleName(), expected, entry.token(), entry.position()));
    }

    public SyntaxErrorException(String message) {
        super(message);
    }

}
