package org.jmouse.validator.el.lexer;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * The words a {@code .jmv} validation file adds on top of the expression language.
 *
 * <p>Everything else a validation document is made of — identifiers, strings, numbers, {@code :},
 * {@code ,}, braces, parentheses, and the whole of a guard's condition — is already a
 * {@link BasicToken} or belongs to jMouse EL. Only the words below are the language's own, and there
 * are eight of them, which is a fair measure of how small it is.</p>
 *
 * <p>Ids are grouped so a reader can tell what a token is for at a glance: {@code 10xxx} opens a
 * document, {@code 20xxx} declares a block inside one, and {@code 30xxx} modifies a single check.
 * Every id is distinct — they identify a token type, and two words sharing one would be two names for
 * a single thing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum JmvToken implements Token.Type {

    /** {@code validation "name" { … }} — the document wrapper, one per file. */
    T_VALIDATION(10000, "validation"),

    /**
     * {@code gate { … }} — checks that run first and, failing, answer for the whole document.
     *
     * <p>The one word here with no sibling in {@code .jmp}, {@code .jmm} or {@code .jmq}, because none
     * of those three has anything shaped like it: a block whose failure means the rest of the file was
     * never worth evaluating. A record of the wrong shape is not a record with several faults — it is a
     * record nothing else in the document is about.</p>
     */
    T_GATE(20000, "gate"),

    /** {@code always { … }} — the checks that hold whatever the record looks like. */
    T_ALWAYS(20100, "always"),

    /**
     * {@code when quantity > 0 { … }} — checks that apply only while a condition holds.
     *
     * <p>⚠️ Nests, and nesting is conjunction: a {@code when} inside a {@code when} carries both
     * guards. {@code when a { when b { … } }} and {@code when a and b { … }} are the same document,
     * so a file can be written flat or deep depending on which reads better.</p>
     */
    T_WHEN(20200, "when"),

    /**
     * {@code } otherwise { … }} — the other branch of the {@link #T_WHEN} it follows.
     *
     * <p>⚠️ It binds to its own {@code when} and never to an outer one. Both branches are brace blocks,
     * so the dangling-else ambiguity cannot be written down rather than being settled by a precedence
     * rule nobody remembers.</p>
     */
    T_OTHERWISE(20300, "otherwise"),

    /**
     * {@code invariant min <= quantity : '…'} — an assertion about the record rather than one field.
     *
     * <p>Kept a separate word from a check because it has no field to belong to. Attaching it to one of
     * the fields it mentions would put the error on whichever was named first, which is a coin toss
     * dressed up as an answer.</p>
     */
    T_INVARIANT(20400, "invariant"),

    /**
     * {@code required stop} — having failed, this check silences the rest of its own field's.
     *
     * <p>⚠️ Its own field's, and no further. A failed precondition makes its followers noise —
     * complaining that a blank part number does not match a pattern helps nobody — but silencing a
     * <em>sibling field</em> would make somebody fix a form one round trip at a time.</p>
     */
    T_STOP(30000, "stop"),

    /**
     * {@code optional} — said out loud, where a field's absence is deliberate.
     *
     * <p>Every constraint treats {@code null} as valid, so this changes no behaviour by itself. It is
     * here because a line carrying only {@code url(…)} reads as an oversight, and a reader cannot tell
     * a field nobody thought about from one somebody decided about.</p>
     */
    T_OPTIONAL(30100, "optional");

    private final int      type;
    private final String[] values;

    JmvToken(final int type, final String... values) {
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
     * know that.</strong> It turns {@code stop} into {@link #T_STOP} wherever the word appears — and
     * every one of these eight words is a plausible <em>field name</em>. A form with a field called
     * {@code stop}, {@code gate}, {@code always} or {@code optional} is not exotic; read as a keyword
     * there, such a field is not one that validates oddly, it is one that <strong>cannot be written
     * down at all</strong>. The file will not parse, and the only advice left is "rename your field",
     * which is a validation language dictating a product's vocabulary.</p>
     *
     * <p>So wherever a bare name belongs — the left-hand side of a check line, a check's own name, a
     * named argument's key — this is what may be there. It costs no ambiguity: every position that
     * reads a name is already pinned by what surrounds it, so widening what counts as a name cannot
     * make two shapes match the same tokens.</p>
     *
     * @return the identifier token followed by every keyword
     */
    public static Token.Type[] nameTokens() {
        return NAME_TOKENS;
    }

    /**
     * The set {@link #nameTokens()} hands out, built once.
     *
     * <p>⚠️ <strong>Shared, therefore never written into.</strong> Every caller passes it straight to a
     * varargs matcher that only reads it. Handing out a copy would give that guarantee back and undo
     * the point of building it once, so the guarantee is stated here instead.</p>
     */
    private static final Token.Type[] NAME_TOKENS = buildNameTokens();

    /**
     * Builds the set, once, for {@link #NAME_TOKENS}.
     *
     * @return the identifier token followed by every keyword
     */
    private static Token.Type[] buildNameTokens() {
        JmvToken[]   keywords = values();
        Token.Type[] names    = new Token.Type[keywords.length + 1];

        names[0] = BasicToken.T_IDENTIFIER;
        System.arraycopy(keywords, 0, names, 1, keywords.length);

        return names;
    }
}
