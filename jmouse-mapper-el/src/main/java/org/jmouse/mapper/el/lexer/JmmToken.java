package org.jmouse.mapper.el.lexer;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * The words a {@code .jmm} mapping file adds on top of the expression language.
 *
 * <p>Everything else a mapping is made of — identifiers, strings, numbers, {@code :}, {@code .},
 * braces, and the whole of an expression's right-hand side — is already a {@link BasicToken} or
 * belongs to jMouse EL. Only the words below are the language's own, and there are fourteen of them,
 * which is a fair measure of how small it is.</p>
 *
 * <p>Ids are grouped so a reader can tell what a token is for at a glance: {@code 10xxx} opens a
 * document, {@code 20xxx} declares a block inside one, {@code 30xxx} modifies a single rule, and
 * {@code 40xxx} is a value a declaration takes. Every id is distinct — they identify a token type,
 * and two words sharing one would be two names for a single thing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum JmmToken implements Token.Type {

    /** {@code mapping "name" { … }} — the document wrapper, one per file. */
    T_MAPPING(10000, "mapping"),

    /** {@code use fully.qualified.Type} — brings a type into the file under its simple name. */
    T_USE(10100, "use"),

    /**
     * {@code fragment auditing { … }} — rules shared across targets, declared at file level.
     *
     * <p>Flat by design: a fragment does not include another one, so a cycle cannot be written and a
     * resolution order never has to be defined.</p>
     */
    T_FRAGMENT(10200, "fragment"),

    /** {@code include auditing} — pulls a fragment's rules into the block it is written in. */
    T_INCLUDE(10300, "include"),

    /** {@code target OrderTarget { … }} — everything about building one type. */
    T_TARGET(20000, "target"),

    /** {@code from OrderSource { … }} — the rules for one source of the enclosing target. */
    T_FROM(20100, "from"),

    /** {@code always { … }} — the rules that hold whatever the source is. */
    T_ALWAYS(20200, "always"),

    /**
     * {@code refuse target before { … }} — assertions that stop a mapping.
     *
     * <p>Reads with a subject and a phase: {@link #T_SOURCE} or {@link #T_TARGET}, then
     * {@link #T_BEFORE} or {@link #T_AFTER}.</p>
     */
    T_REFUSE(20300, "refuse"),

    /**
     * {@code unmapped fail} — whether a target property fed by nothing refuses the file.
     *
     * <p>Takes {@link #T_FAIL} or {@link #T_IGNORE}; the default is to ignore, which is how a mapping
     * with no file at all behaves.</p>
     */
    T_UNMAPPED(20400, "unmapped"),

    /**
     * {@code source} — the subject of a {@code refuse} block.
     *
     * <p>⚠️ This word is also a legal <em>expression</em>: {@code source.total} is how a rule names the
     * object it reads from when a bare path would be ambiguous. The recognizer therefore offers
     * {@link BasicToken} before this enum, so {@code source} inside an expression is read as an
     * identifier and only a {@code refuse} block reads it as a keyword. A parser that assumed the
     * keyword everywhere would break every rule that spells the root out.</p>
     */
    T_SOURCE(30000, "source"),

    /** {@code before} — the phase of a {@code refuse} block, ahead of any construction or writing. */
    T_BEFORE(30100, "before"),

    /** {@code after} — the phase of a {@code refuse} block, once the target is fully written. */
    T_AFTER(30200, "after"),

    /**
     * {@code name : value when condition} — writes only while the condition holds.
     *
     * <p>Not a ternary: a false condition writes <em>nothing</em>, leaving the target property as it
     * was, which is an outcome a ternary cannot produce.</p>
     */
    T_WHEN(30300, "when"),

    /** {@code let full = a ~ " " ~ b} — names an expression for reuse within one block. */
    T_LET(30400, "let"),

    /** {@code fail} — the strict value of {@link #T_UNMAPPED}. */
    T_FAIL(40000, "fail"),

    /**
     * {@code ignore} — a target property deliberately not carried, and the lax value of
     * {@link #T_UNMAPPED}.
     *
     * <p>The one word reserved in expression position, and the only right-hand side that is not
     * evaluated.</p>
     */
    T_IGNORE(40100, "ignore");

    private final int      type;
    private final String[] values;

    JmmToken(final int type, final String... values) {
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
        return (Class<E>) getDeclaringClass();
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
     * A plain identifier, or any of the words above read as one.
     *
     * <p>⚠️ <strong>A keyword is only a keyword where the grammar expects one, and the lexer cannot
     * know that.</strong> It turns {@code source} into {@link #T_SOURCE} wherever the word appears —
     * and every one of these fourteen words is a plausible <em>property name</em>. A target with a
     * property called {@code target}, {@code from}, {@code when} or {@code status}-adjacent
     * {@code fail} is not exotic; read as a keyword there, such a property is not one that fails to
     * bind, it is one that <strong>cannot be written down at all</strong>. The file will not parse,
     * and the only advice left is "rename your field", which is the language dictating a product's
     * vocabulary.</p>
     *
     * <p>So wherever a bare name belongs — the left-hand side of a rule, the root of a source path,
     * a fragment's name — this is what may be there. It costs no ambiguity: every position that reads
     * a name is already pinned by what surrounds it, so widening what counts as a name cannot make
     * two shapes match the same tokens.</p>
     *
     * @return the identifier token followed by every keyword
     */
    public static Token.Type[] nameTokens() {
        return NAME_TOKENS;
    }

    /**
     * The set {@link #nameTokens()} hands out, built once.
     *
     * <p>⚠️ It used to be rebuilt on every call — twice over, since {@code values()} copies as well —
     * and it is asked several times for every line parsed: once to tell a binding from a property called
     * {@code let}, once for an include, once for the rule's own name, and again for every segment of a
     * type name.</p>
     *
     * <p>⚠️ <strong>Shared, therefore never written into.</strong> Every caller passes it straight to a
     * varargs matcher that only reads it. Handing out a copy would give that guarantee back and undo the
     * whole point, so the guarantee is stated here instead.</p>
     */
    private static final Token.Type[] NAME_TOKENS = buildNameTokens();

    /**
     * Builds the set, once, for {@link #NAME_TOKENS}.
     *
     * @return the identifier token followed by every keyword
     */
    private static Token.Type[] buildNameTokens() {
        JmmToken[]   keywords = values();
        Token.Type[] names    = new Token.Type[keywords.length + 1];

        names[0] = BasicToken.T_IDENTIFIER;
        System.arraycopy(keywords, 0, names, 1, keywords.length);

        return names;
    }
}
