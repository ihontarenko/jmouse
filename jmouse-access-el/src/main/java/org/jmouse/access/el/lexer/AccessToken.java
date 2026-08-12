package org.jmouse.access.el.lexer;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * The keywords a {@code .jmp} policy file adds on top of the expression language.
 *
 * <p>Everything else a policy is made of — identifiers, strings, {@code @}, {@code :}, {@code .},
 * braces — is already a {@link org.jmouse.el.lexer.BasicToken}. Only the words below are the
 * language's own, which is a fair measure of how small it is.</p>
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

    /**
     * {@code declare scopes { … }} — the optional prefix on a block stating <em>structure</em>: what
     * exists at all.
     *
     * <p>⚠️ <strong>Optional in the grammar, canonical in the writer.</strong> Policy revisions are
     * stored as source text and the control room can revert to one, so a hard break would make every
     * revision written before this unparseable — turning revert into a way to break an installation.
     * Both spellings produce the same node, and {@code toSource()} always writes the prefix, so a
     * document converges on the canonical form the first time it is saved through the editor.
     */
    T_DECLARE(11000, "declare"),

    /**
     * {@code assign subject 'usr' { … }} — the optional prefix on a block stating a <em>case</em>:
     * who has what.
     *
     * <p>The pair with {@link #T_DECLARE} is not decoration. It is ADR-0018's line — <em>the document
     * owns structure, the row owns the case</em> — made visible at the left margin, so a reader who
     * learns the distinction once never has to look a block up again.
     *
     * <p>⚠️ {@code include} is deliberately left bare: it is an operation on the file rather than a
     * statement about access, and being the one unprefixed keyword is a useful signal in itself.
     */
    T_ASSIGN(11100, "assign"),

    /** {@code scopes { … }} — the vocabulary of floors. */
    T_SCOPES(10100, "scopes"),

    /** {@code permissions { … }} — the vocabulary of permissions. */
    T_PERMISSIONS(10200, "permissions"),

    /** {@code role NAME { … }} — a bundle of permissions and how far each reaches. */
    T_ROLE(10300, "role"),

    /** {@code subject id { … }} — one account's assignments and personal grants. */
    T_SUBJECT(10400, "subject"),

    /** {@code capabilities { … }} — the vocabulary of what a grant can be about. */
    T_CAPABILITIES(10500, "capabilities"),

    /** {@code plans { … }} — the named bundles of capabilities. */
    T_PLANS(10600, "plans"),

    /** {@code plan code "Name" { … }} — one bundle. */
    T_PLAN(10700, "plan"),

    /** {@code entitlements { … }} — who is on what, and until when. */
    T_ENTITLEMENTS(10800, "entitlements"),

    /** {@code actions { … }} — the vocabulary of what calls are doing. */
    T_ACTIONS(10900, "actions"),

    /** {@code grants ROLE @SCOPE} — assigns a role to the enclosing subject. */
    T_GRANTS(20000, "grants"),

    /** The effect that is always written, because a denial is never accidental. */
    T_DENY(20100, "deny"),

    /** The effect that is implied when nothing is written. */
    T_ALLOW(20200, "allow"),

    /** {@code when <expression>} — a condition, kept as raw text and compiled by stage 2. */
    T_WHEN(20300, "when"),

    /** {@code paid a, b} — capabilities that are closed until something grants them. */
    T_PAID(20400, "paid"),

    /** {@code limit seat "Seats"} — a capability carrying a standing count. */
    T_LIMIT(20500, "limit"),

    /** {@code quota storage-byte "…"} — a capability consumed over a window. */
    T_QUOTA(20600, "quota"),

    /** {@code gate custody "Custody"} — a capability that is open or closed, with no number. */
    T_GATE(20700, "gate"),

    /** {@code per organization, space} — where a capability may be granted; also {@code per month}. */
    T_PER(20800, "per"),

    /** {@code order 20} — display order of a bundle. */
    T_ORDER(20900, "order"),

    /** {@code note "…"} — the sentence a screen shows beside a bundle. */
    T_NOTE(22000, "note"),

    /** {@code extends business} — the bundle this one starts from. */
    T_EXTENDS(22100, "extends"),

    /** ⚠️ No ceiling — never a very large number. */
    T_UNLIMITED(22200, "unlimited"),

    /** {@code trial business until …} — a bundle with an end and a provenance of its own. */
    T_TRIAL(22300, "trial"),

    /** {@code from 2026-08-01} — when a grant starts applying. */
    T_FROM(22400, "from"),

    /** {@code until 2026-09-01} — when it stops. */
    T_UNTIL(22500, "until"),

    /** {@code reason "…"} — why, and the words a refusal repeats back. */
    T_REASON(22600, "reason"),

    /** {@code publishes purpose, tier} — the values one action carries into a condition. */
    T_PUBLISHES(22700, "publishes"),

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
     * A plain identifier, or any of the words above read as one.
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
    public static Token.Type[] nameTokens() {
        AccessToken[]  keywords = values();
        Token.Type[]   names    = new Token.Type[keywords.length + 1];

        names[0] = BasicToken.T_IDENTIFIER;
        System.arraycopy(keywords, 0, names, 1, keywords.length);

        return names;
    }

    /** The same, plus the {@code *} that names a whole namespace. */
    public static Token.Type[] nameTokensWithWildcard() {
        Token.Type[] names    = nameTokens();
        Token.Type[] extended = new Token.Type[names.length + 1];

        System.arraycopy(names, 0, extended, 0, names.length);
        extended[names.length] = BasicToken.T_MULTIPLY;

        return extended;
    }

}
