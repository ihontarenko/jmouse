package org.jmouse.script.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * The keywords a {@code .jms} script adds on top of the expression language.
 *
 * <p>Deliberately small. {@code if}, {@code elseif}, {@code else} and {@code function} are
 * {@link LanguageToken}s the engine already owns, {@code in} is a {@link BasicToken} operator, and
 * everything a script is otherwise made of — identifiers, strings, {@code @}, braces, parentheses —
 * was already there. What is left below is the house the dialect builds around them.</p>
 *
 * <p>Ids are grouped so a reader can tell what a token is for at a glance: {@code 10xxx} opens a file
 * scope construction, {@code 11xxx} opens a handler, {@code 12xxx} delimits a body, {@code 13xxx} is a
 * statement written inside one. Every id is distinct — they identify a token type and two words
 * sharing one would be two names for a single thing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum ScriptToken implements Token.Type {

    /** {@code include 'common.jms'} — composition, recorded and not followed. */
    T_INCLUDE(10000, "include"),

    /** {@code script "slice-01" { … }} — the story half of a file. */
    T_SCRIPT(10100, "script"),

    /**
     * {@code behaviour "gatherer" do … end} — the mechanic half.
     *
     * <p>Both spellings are accepted and produce one token. The word is written both ways by people
     * who are equally sure, and a file refused over a {@code u} is a file nobody enjoys.</p>
     */
    T_BEHAVIOUR(10200, "behaviour", "behavior"),

    /** {@code on unload when … do … end} — a handler bound to a host event. */
    T_ON(11000, "on"),

    /** The optional guard on a handler. */
    T_WHEN(11100, "when"),

    /** Opens a word-delimited body — a handler's, a behaviour's, a loop's. */
    T_DO(12000, "do"),

    /** Opens a branch's body. */
    T_THEN(12100, "then"),

    /** Closes every word-delimited body in the language. */
    T_END(12200, "end"),

    /** {@code local slot = @store.next_slot(entry)} — a name bound in this body. */
    T_LOCAL(13000, "local"),

    /** {@code return}, with or without a value. */
    T_RETURN(13100, "return"),

    /** {@code for entry in @store.pending('inbox') do … end} */
    T_FOR(13200, "for");

    private final int      type;
    private final String[] values;

    ScriptToken(final int type, final String... values) {
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

    /**
     * Everything that may stand where this language expects a <em>name</em>.
     *
     * <p>⚠️ <strong>A keyword is also an ordinary word.</strong> The lexer has no idea whether the
     * {@code end} it just read closes a body or is the name of a facade method, and a host is entitled
     * to an event called {@code on} or a function called {@code local} — those are its nouns, not this
     * language's. Reading a name against this set rather than against
     * {@link BasicToken#T_IDENTIFIER} is what keeps a grammar from dictating a host's vocabulary.</p>
     *
     * <p>It costs no ambiguity, because a name is only ever read at a position where the construction
     * around it has already been identified.</p>
     *
     * @return the identifier token, plus every keyword of this dialect
     */
    public static Token.Type[] nameTokens() {
        ScriptToken[] keywords = values();
        Token.Type[]  names    = new Token.Type[keywords.length + 1];

        names[0] = BasicToken.T_IDENTIFIER;
        System.arraycopy(keywords, 0, names, 1, keywords.length);

        return names;
    }

}
