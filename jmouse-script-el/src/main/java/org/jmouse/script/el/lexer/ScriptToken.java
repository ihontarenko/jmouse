package org.jmouse.script.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.script.el.lexer.StatementToken;

/**
 * The keywords jMS adds on top of the statement core — the file-scope constructions and the handler. The
 * statement keywords ({@code include}, {@code do}, {@code then}, {@code end}, {@code let}, {@code return},
 * {@code for}) are the core's {@link org.jmouse.script.el.lexer.StatementToken}.
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
    T_WHEN(11100, "when");

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

    /** Everything that may stand where jMS expects a name: the core's names, which include these keywords. */
    public static Token.Type[] nameTokens() {
        return StatementToken.nameTokens();
    }

    static {
        StatementToken.registerDialectKeywords(values());
    }

}
