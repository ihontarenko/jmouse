package org.jmouse.script.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The keywords the statement core adds on top of the expression language — what every script dialect built on
 * it shares: composition, body delimiters, and the statements written inside a body.
 *
 * <p>Deliberately small. {@code if}, {@code elseif}, {@code else} and {@code function} are {@link LanguageToken}s
 * the engine already owns, and {@code in} is a {@link BasicToken} operator. A dialect's own words — jMS's
 * {@code script}, {@code behaviour}, {@code on}, {@code when} — live in its own token set.</p>
 *
 * <p>Ids keep the grouping they had in jMS, so a reader can tell what a token is for at a glance: {@code 10xxx}
 * is file scope, {@code 12xxx} delimits a body, {@code 13xxx} is a statement written inside one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum StatementToken implements Token.Type {

    /** {@code include 'common.jms'} — composition, recorded and not followed. */
    T_INCLUDE(10000, "include"),

    T_DO(12000, "do"),
    T_THEN(12100, "then"),
    T_END(12200, "end"),

    /** {@code let} — and {@code local}, its other spelling: one keyword, one meaning. */
    T_LET(13000, "let", "local"),
    T_CONST(13010, "const"),
    T_RETURN(13100, "return"),
    T_FOR(13200, "for");

    private final int      type;
    private final String[] values;

    StatementToken(final int type, final String... values) {
        this.type = type;
        this.values = values;
    }

    @Override
    public int getTypeId() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) getEnumType().getClass();
    }

    @Override
    public String[] getTokenTemplates() {
        return values;
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }

    /** The keywords dialects have registered as words a host may also use as names. */
    private static volatile Token.Type[] dialectKeywords = new Token.Type[0];

    /**
     * Everything that may stand where a statement expects a <em>name</em>: an identifier, every keyword of the
     * core, and every keyword a dialect has registered.
     *
     * <p>⚠️ <strong>A keyword is also an ordinary word.</strong> The lexer has no idea whether the {@code end} it
     * just read closes a body or is the name of a facade method, and a host is entitled to an event called
     * {@code on} or a function called {@code let} — those are its nouns, not this language's. Reading a name
     * against this set rather than against {@link BasicToken#T_IDENTIFIER} is what keeps a grammar from dictating
     * a host's vocabulary. It costs no ambiguity, because a name is only ever read at a position where the
     * construction around it has already been identified.</p>
     *
     * <p>A registered keyword in a dialect that does not have it is harmless: that dialect's lexer never
     * produces the token, so the name is an identifier there anyway.</p>
     */
    public static Token.Type[] nameTokens() {
        Token.Type[] dialect = dialectKeywords;
        StatementToken[] keywords = values();
        Token.Type[] names = new Token.Type[1 + keywords.length + dialect.length];
        names[0] = BasicToken.T_IDENTIFIER;
        System.arraycopy(keywords, 0, names, 1, keywords.length);
        System.arraycopy(dialect, 0, names, 1 + keywords.length, dialect.length);
        return names;
    }

    /** Called once by a dialect for its own keywords, so the core's statements accept them as names too. */
    public static synchronized void registerDialectKeywords(Token.Type... keywords) {
        Set<Token.Type> merged = new LinkedHashSet<>(List.of(dialectKeywords));
        merged.addAll(List.of(keywords));
        dialectKeywords = new ArrayList<>(merged).toArray(Token.Type[]::new);
    }
}
