package org.jmouse.access.el.lexer;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * The keywords a {@code .jmp} policy file adds on top of the expression language.
 *
 * <p>Everything else a policy is made of — identifiers, strings, {@code @}, {@code :}, braces — is
 * already a {@link org.jmouse.el.lexer.BasicToken}. Only these ten words are the language's own,
 * which is a fair measure of how small it is.</p>
 *
 * <p>Ids are grouped so a reader can tell what a token is for at a glance: {@code 10xxx} opens a
 * block, {@code 20xxx} is written inside one, {@code 21xxx} composes files. Every id is distinct —
 * they identify a token type and two words sharing one would be two names for a single thing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum AccessToken implements Token.Type {

    /** {@code policy "name" { … }} — the optional document wrapper. */
    T_POLICY(10000, "policy"),

    /** {@code scopes { … }} — the vocabulary of floors. */
    T_SCOPES(10100, "scopes"),

    /** {@code permissions { … }} — the vocabulary of permissions. */
    T_PERMISSIONS(10200, "permissions"),

    /** {@code role NAME { … }} — a bundle of permissions and how far each reaches. */
    T_ROLE(10300, "role"),

    /** {@code subject id { … }} — one account's assignments and personal grants. */
    T_SUBJECT(10400, "subject"),

    /** {@code grants ROLE @SCOPE} — assigns a role to the enclosing subject. */
    T_GRANTS(20000, "grants"),

    /** The effect that is always written, because a denial is never accidental. */
    T_DENY(20100, "deny"),

    /** The effect that is implied when nothing is written. */
    T_ALLOW(20200, "allow"),

    /** {@code when <expression>} — a condition, kept as raw text and compiled by stage 2. */
    T_WHEN(20300, "when"),

    /** {@code include 'path'} — records a path; the loader, not the parser, follows it. */
    T_INCLUDE(21000, "include");

    private final int      type;
    private final String[] values;

    AccessToken(final int type, final String... values) {
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
     * A plain identifier, or any of these ten words read as one.
     *
     * <p>⚠️ <strong>A keyword is only a keyword where the grammar expects one.</strong> The lexer
     * cannot know that, so it turns {@code role} into {@link #T_ROLE} wherever it appears — including
     * in {@code role:read}, which is an ordinary permission in a product that administers roles. Read
     * as a keyword there it is not a permission that fails to bind, it is a permission that
     * <strong>cannot be written down at all</strong>: the file will not parse, and the answer "rename
     * your permission" is the language dictating a product's vocabulary.</p>
     *
     * <p>So wherever a bare NAME belongs, this is what may be there. It costs no ambiguity: every
     * position that reads a name is already pinned by something around it — a permission by its
     * colon, a role body by its brace — so widening what counts as a name cannot make two shapes
     * match the same tokens.</p>
     *
     * @return the identifier token followed by every keyword
     */
    public static Token.Type[] namesAndKeywords() {
        AccessToken[]  keywords = values();
        Token.Type[]   names    = new Token.Type[keywords.length + 1];

        names[0] = BasicToken.T_IDENTIFIER;
        System.arraycopy(keywords, 0, names, 1, keywords.length);

        return names;
    }

    /** The same, plus the {@code *} that names a whole namespace. */
    public static Token.Type[] namesKeywordsAndWildcard() {
        Token.Type[] names    = namesAndKeywords();
        Token.Type[] extended = new Token.Type[names.length + 1];

        System.arraycopy(names, 0, extended, 0, names.length);
        extended[names.length] = BasicToken.T_MULTIPLY;

        return extended;
    }

}
