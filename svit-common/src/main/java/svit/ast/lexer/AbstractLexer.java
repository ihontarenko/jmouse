package svit.ast.lexer;

import svit.ast.token.Token;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractLexer extends ImmutableListIterator<Token.Entry> implements Lexer {

    public AbstractLexer(List<Token.Entry> entries) {
        super(entries);
    }

    @Override
    public void forward(Predicate<Token.Entry> predicate) {
        while (hasNext() && !predicate.test(current())) {
            next();
        }
    }

    @Override
    public void forward(Token token) {
        forward(e -> e.is(token));
    }

    @Override
    public void forward(Token.Entry entry) {
        forward(e -> e.is(entry));
    }

    @Override
    public void backward(Predicate<Token.Entry> predicate) {
        while (hasPrevious() && !predicate.test(current())) {
            previous();
        }
    }

    @Override
    public void backward(Token token) {
        backward(e -> e.is(token));
    }

    @Override
    public void backward(Token.Entry entry) {
        backward(e -> e.is(entry));
    }

    @Override
    public Token.Entry current() {
        return entries.get(cursor);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public int cursor() {
        return cursor;
    }

    @Override
    public void cursor(int cursor) {
        this.cursor = cursor;
    }

    @Override
    public boolean is(int limit, int offset, Token... tokens) {
        if (cursor + offset < 0) {
            offset = -cursor;
        }

        Lexer       lexer    = lexer(offset);
        List<Token> expected = Arrays.asList(tokens);

        while (limit-- > 0 && lexer.hasNext()) {
            if (expected.contains(lexer.next().token())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Token.Entry lookOver(Token start, Token end) {
        Lexer       lexer  = lexer();
        Token.Entry result = null;
        int         depth  = 0;
        Token.Entry next   = lexer.next();

        while (lexer.hasNext()) {
            if (next.is(start)) {
                depth++;
            }

            if (next.is(end)) {
                depth--;
            }

            if (depth == 0) {
                result = next;
                break;
            }

            next = lexer.next();
        }

        if (result == null) {
            throw new LexerException("CANNOT FIND END FOR: %s".formatted(end));
        }

        return result;
    }

    @Override
    public Lexer lexer(int offset) {
        int   cursor = AbstractLexer.this.cursor;
        Lexer lexer  = new AbstractLexer(AbstractLexer.this.entries) {};

        lexer.cursor(cursor + offset);

        return lexer;
    }

}
