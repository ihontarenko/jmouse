package org.jmouse.access.el;

import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

import static org.jmouse.access.el.lexer.AccessToken.*;
import static org.jmouse.el.lexer.BasicToken.*;

/**
 * The shape of every {@code .jmp} construction, as a lookahead over tokens.
 *
 * <p>Dispatch in the expression language is "first parser whose {@code supports} says yes, in
 * priority order", so a matcher that is merely <em>nearly</em> right is a statement quietly handed
 * to the wrong parser. Two rules keep that from happening here:</p>
 *
 * <ul>
 *   <li><strong>Every matcher is anchored.</strong> It reads from the cursor's own position outwards
 *       and never scans the rest of the file for a token it hopes to find.</li>
 *   <li><strong>Overlapping shapes are made disjoint, not ordered.</strong> A scope declaration and a
 *       grant both open {@code @ NAME NAME}; the declaration additionally requires that no colon
 *       follows, so exactly one of them can match and priority never has to break the tie.</li>
 * </ul>
 *
 * <p>One ambiguity survives and cannot be removed: {@code @INSTALLATION form:read} is the same five
 * tokens in a {@code role} body and in a {@code subject} body. It is one syntax meaning two things
 * depending on where it is written, so it parses to one node and the enclosing block decides — see
 * {@link org.jmouse.access.el.node.RoleNode} and {@link org.jmouse.access.el.node.SubjectNode}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CursorMatcher {

    /**
     * How far ahead a {@code ${…}} placeholder is allowed to run before it is judged unterminated.
     * Generous for a property path, and short enough that a stray {@code $} cannot make a matcher
     * read the rest of the file.
     */
    private static final int PLACEHOLDER_LOOKAHEAD = 32;

    /** Returned by the scanners below for "this is not that shape". */
    private static final int NO_MATCH = -1;

    private CursorMatcher() {
    }

    /** {@code policy "name" { … }} */
    public static Matcher<TokenCursor> policy() {
        return new PolicyMatcher();
    }

    /** {@code scopes { … }} */
    public static Matcher<TokenCursor> scopes() {
        return new ScopesMatcher();
    }

    /** {@code @NAME nature [parameter=name]} — one line of a {@code scopes} block. */
    public static Matcher<TokenCursor> scopeDeclaration() {
        return new ScopeDeclarationMatcher();
    }

    /** {@code permissions { … }} */
    public static Matcher<TokenCursor> permissions() {
        return new PermissionsMatcher();
    }

    /** {@code form:read "Read forms"} — one line of a {@code permissions} block. */
    public static Matcher<TokenCursor> permissionDeclaration() {
        return new PermissionDeclarationMatcher();
    }

    /** {@code form:read} or {@code form:*} */
    public static Matcher<TokenCursor> permissionValue() {
        return new PermissionValueMatcher();
    }

    /** {@code actions { … }} */
    public static Matcher<TokenCursor> actions() {
        return new ActionsMatcher();
    }

    /** {@code entry.listByPurpose "…" publishes purpose, tier} — one line of an {@code actions} block. */
    public static Matcher<TokenCursor> actionDeclaration() {
        return new ActionDeclarationMatcher();
    }

    /** {@code capabilities { … }} */
    public static Matcher<TokenCursor> capabilities() {
        return new CapabilitiesMatcher();
    }

    /** {@code limit seat "Seats" per organization} — one line of a {@code capabilities} block. */
    public static Matcher<TokenCursor> capabilityDeclaration() {
        return new CapabilityDeclarationMatcher();
    }

    /** {@code paid custody, parametric-search} */
    public static Matcher<TokenCursor> paidCapabilities() {
        return new PaidCapabilitiesMatcher();
    }

    /** {@code plans { … }} */
    public static Matcher<TokenCursor> plans() {
        return new PlansMatcher();
    }

    /** {@code plan business "Business" order 30 … { … }} */
    public static Matcher<TokenCursor> plan() {
        return new PlanMatcher();
    }

    /** {@code storage-byte 100GB per month} — one line of a plan's body. */
    public static Matcher<TokenCursor> planGrant() {
        return new PlanGrantMatcher();
    }

    /** {@code entitlements { … }} */
    public static Matcher<TokenCursor> entitlements() {
        return new EntitlementsMatcher();
    }

    /** {@code @ORGANIZATION:acme plan team} — one line of an {@code entitlements} block. */
    public static Matcher<TokenCursor> entitlement() {
        return new EntitlementMatcher();
    }

    /** {@code role NAME { … }} */
    public static Matcher<TokenCursor> role() {
        return new RoleMatcher();
    }

    /** {@code subject id { … }} */
    public static Matcher<TokenCursor> subject() {
        return new SubjectMatcher();
    }

    /** {@code grants ROLE @SCOPE} */
    public static Matcher<TokenCursor> roleAssignment() {
        return new RoleAssignmentMatcher();
    }

    /** {@code @SPACE} or {@code @SPACE:kyiv} */
    public static Matcher<TokenCursor> singleScope() {
        return new SingleScopeMatcher();
    }

    /** {@code @SCOPE[:instance] permission [allow|deny] [when …]} */
    public static Matcher<TokenCursor> grant() {
        return new GrantMatcher();
    }

    /**
     * How many tokens the optional {@code declare} / {@code assign} prefix occupies here — 1 or 0.
     *
     * <p>⚠️ <strong>Learned once, in this one method.</strong> The alternative is an {@code if} inside
     * nine matchers and nine parsers, which is the shape where one of them is eventually forgotten and
     * a block quietly stops being writable in the canonical form.
     *
     * <p>It is a prefix only where a block keyword follows it, and that qualifier is not optional:
     * {@link AccessToken#nameTokens()} reads every keyword of this grammar as a name wherever a name
     * belongs, so {@code declare} is also an ordinary permission segment, capability key or property
     * name. A rule reading {@code resource.declare} must not be mistaken for the start of a block.
     */
    private static int blockPrefixLength(TokenCursor cursor) {
        boolean prefixed = checkAt(cursor, 0, T_DECLARE, T_ASSIGN)
                && checkAt(cursor, 1, T_POLICY, T_SCOPES, T_PERMISSIONS, T_ACTIONS, T_CAPABILITIES,
                           T_PLANS, T_ENTITLEMENTS, T_ROLE, T_SUBJECT);

        return prefixed ? 1 : 0;
    }

    /** {@code [declare|assign] KEYWORD &#123;} — a block opened by one word and a brace. */
    private static boolean opensBlock(TokenCursor cursor, Token.Type keyword) {
        int prefix = blockPrefixLength(cursor);

        return checkAt(cursor, prefix, keyword) && checkAt(cursor, prefix + 1, T_OPEN_CURLY);
    }

    private record PolicyMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int prefix = blockPrefixLength(cursor);

            return checkAt(cursor, prefix, T_POLICY)
                    && checkAt(cursor, prefix + 1, T_STRING)
                    && checkAt(cursor, prefix + 2, T_OPEN_CURLY);
        }
    }

    private record ScopesMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return opensBlock(cursor, T_SCOPES);
        }
    }

    private record PermissionsMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return opensBlock(cursor, T_PERMISSIONS);
        }
    }

    private record ActionsMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return opensBlock(cursor, T_ACTIONS);
        }
    }

    /**
     * An action declaration is a <strong>dotted</strong> name followed by its description.
     *
     * <p>⚠️ <strong>The dot is required, and it is the whole of what makes this shape disjoint.</strong>
     * Without it the shape is {@code NAME STRING} — which is also, exactly,
     * {@code include 'startup.jmp'}. {@code include} is a keyword that {@link AccessToken#nameTokens()}
     * deliberately reads as a name wherever a name belongs, so a dot-less matcher takes every include
     * line in the language and reports the loss as "a policy cannot hold this statement".
     *
     * <p>That is the same pinning a permission already has, and it is not the grammar dictating a
     * product's vocabulary. {@code form:read} is a permission <em>because of its colon</em> wherever it
     * is written; {@code entry.list} is an action because of its dot. Both are asking a product to
     * spell a thing the way this language spells that kind of thing — the same as a scope's {@code @}.
     * A one-word action is the price, and "dots, never colons" was the convention before it was a rule.
     */
    private record ActionDeclarationMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int name = dottedNameLength(cursor, 0);

            return name > 1 && checkAt(cursor, name, T_STRING);
        }
    }

    private record CapabilitiesMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return opensBlock(cursor, T_CAPABILITIES);
        }
    }

    /**
     * A capability declaration is pinned by its leading kind word, which nothing else in the language
     * begins with — so no lookahead beyond one token is needed to tell it from a {@code paid} line or
     * from anything a block might hold by mistake.
     */
    private record CapabilityDeclarationMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_GATE, T_LIMIT, T_QUOTA)
                    && hyphenatedNameLength(cursor, 1) != NO_MATCH;
        }
    }

    private record PaidCapabilitiesMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_PAID) && hyphenatedNameLength(cursor, 1) != NO_MATCH;
        }
    }

    private record EntitlementsMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return opensBlock(cursor, T_ENTITLEMENTS);
        }
    }

    /**
     * An entitlement opens like a grant — {@code @SCOPE[:instance]} — and is told apart from one by
     * the word that follows: a grant names a permission, which always brings a colon, and this always
     * names one of four effects. Disjoint by construction, so priority never has to break the tie.
     */
    private record EntitlementMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int place = placeLength(cursor);

            return place != NO_MATCH && checkAt(cursor, place, T_PLAN, T_TRIAL, T_ALLOW, T_DENY);
        }

        /**
         * ⚠️ Measures {@code @SCOPE[:instance]} with a <strong>hyphen-tolerant</strong> instance, which
         * is what {@code scopeReferenceLength} deliberately is not.
         *
         * <p>A grant's instance may be quoted where it needs a hyphen — {@code @SPACE:'my-space'} — and
         * that is a fair price for a rule written once. An entitlement's instance is a <em>slug</em>,
         * and slugs are hyphenated by nature: {@code acme-warehouse} is the normal case rather than the
         * exception, and a block where every second line needs quotes is a block nobody proofreads.
         */
        private int placeLength(TokenCursor cursor) {
            if (!checkAt(cursor, 0, T_AT) || !checkAt(cursor, 1, T_IDENTIFIER)) {
                return NO_MATCH;
            }

            if (!checkAt(cursor, 2, T_COLON)) {
                return 2;
            }

            int instance = hyphenatedNameLength(cursor, 3);

            return instance == NO_MATCH ? NO_MATCH : 3 + instance;
        }
    }

    private record PlansMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return opensBlock(cursor, T_PLANS);
        }
    }

    /**
     * A bundle header runs from {@code plan} to the brace and may hold three optional clauses in any
     * order, so it is anchored on the keyword and the code rather than measured to the end.
     */
    private record PlanMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_PLAN)
                    && hyphenatedNameLength(cursor, 1) != NO_MATCH;
        }
    }

    /**
     * A plan-body line is a capability and, optionally, how much of it.
     *
     * <p>⚠️ It is the one shape in the language opening with a bare name, so it is made disjoint by
     * what may <em>follow</em>: an amount, {@code unlimited}, {@code per}, or the end of the line. A
     * colon is excluded because that is a permission, which is the only other bare-name shape and
     * lives in a different block.
     */
    private record PlanGrantMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int name = hyphenatedNameLength(cursor, 0);

            if (name == NO_MATCH || checkAt(cursor, name, T_COLON)) {
                return false;
            }

            return checkAt(cursor, name, T_NUMERIC, T_INT, T_LONG, T_UNLIMITED, T_PER,
                           T_NEW_LINE, T_EOL, T_SEMICOLON, T_CLOSE_CURLY);
        }
    }

    private record RoleMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int prefix = blockPrefixLength(cursor);

            return checkAt(cursor, prefix, T_ROLE)
                    && checkAt(cursor, prefix + 1, T_IDENTIFIER, T_STRING)
                    && checkAt(cursor, prefix + 2, T_OPEN_CURLY);
        }
    }

    private record SubjectMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int prefix = blockPrefixLength(cursor);
            int name   = nameLength(cursor, prefix + 1);

            return checkAt(cursor, prefix, T_SUBJECT) && name != NO_MATCH
                    && checkAt(cursor, prefix + 1 + name, T_OPEN_CURLY);
        }
    }

    private record RoleAssignmentMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(T_GRANTS, T_IDENTIFIER)
                    || cursor.matchesSequence(T_GRANTS, T_STRING);
        }
    }

    /**
     * A scope declaration is {@code @ NAME nature}, and is told apart from a grant by what does
     * <em>not</em> follow: a grant's permission always brings a colon, a nature never does.
     */
    private record ScopeDeclarationMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return checkAt(cursor, 0, T_AT)
                    && checkAt(cursor, 1, T_IDENTIFIER)
                    && checkAt(cursor, 2, T_IDENTIFIER, T_STRING)
                    && !checkAt(cursor, 3, T_COLON);
        }
    }

    private record PermissionValueMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return permissionValueLength(cursor, 0) != NO_MATCH;
        }
    }

    private record PermissionDeclarationMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int permission = permissionValueLength(cursor, 0);
            return permission != NO_MATCH && checkAt(cursor, permission, T_STRING);
        }
    }

    private record SingleScopeMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return scopeReferenceLength(cursor, 0) != NO_MATCH;
        }
    }

    private record GrantMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int scope = scopeReferenceLength(cursor, 0);
            return scope != NO_MATCH && permissionValueLength(cursor, scope) != NO_MATCH;
        }
    }

    /**
     * Measures {@code @KIND} or {@code @KIND:instance}, where the instance may be a name, a
     * {@code *}, a quoted string or a {@code ${placeholder}}.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the reference is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    private static int scopeReferenceLength(TokenCursor cursor, int start) {
        if (!checkAt(cursor, start, T_AT) || !checkAt(cursor, start + 1, T_IDENTIFIER)) {
            return NO_MATCH;
        }

        int length = 2;

        if (!checkAt(cursor, start + length, T_COLON)) {
            return length;
        }

        length++;

        if (checkAt(cursor, start + length, T_DOLLAR)) {
            int placeholder = placeholderLength(cursor, start + length);
            return placeholder == NO_MATCH ? NO_MATCH : length + placeholder;
        }

        if (checkAt(cursor, start + length, T_IDENTIFIER, T_MULTIPLY, T_STRING)) {
            return length + 1;
        }

        return NO_MATCH;
    }

    /**
     * Measures {@code namespace:action}, {@code namespace:action:qualifier…}, or {@code namespace:*}.
     *
     * <p>⚠️ <strong>Two things a permission is allowed to be, and both are a product's business
     * rather than a language's.</strong>
     *
     * <ul>
     *   <li><strong>More than two segments.</strong> {@code form:write:system} and
     *       {@code space:module:restrict} are real permissions in a real installation. A grammar that
     *       stopped at one colon would refuse the file, and "rename your permission" is a language
     *       dictating a product's vocabulary.
     *   <li><strong>Any segment may be one of the language's own words.</strong> {@code role:read}
     *       is ordinary wherever roles are administered, and the lexer has no way to know that this
     *       {@code role} is not the block keyword — see {@link AccessToken#nameTokens()}.
     * </ul>
     *
     * <p>Neither costs any ambiguity. The colon is what identifies this shape, and nothing that may
     * follow a permission — {@code allow}, {@code deny}, {@code when}, a description, the end of the
     * line — begins with one, so the run cannot swallow what comes next.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the permission is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    private static int permissionValueLength(TokenCursor cursor, int start) {
        boolean opens = checkAt(cursor, start, AccessToken.nameTokens())
                && checkAt(cursor, start + 1, T_COLON)
                && checkAt(cursor, start + 2, AccessToken.nameTokensWithWildcard());

        if (!opens) {
            return NO_MATCH;
        }

        int length = 3;

        // A wildcard ends the permission: `form:*:anything` names nothing, and reading further would
        // measure a shape the binder is only going to refuse under a less helpful name.
        while (!checkAt(cursor, start + length - 1, T_MULTIPLY)
               && checkAt(cursor, start + length, T_COLON)
               && checkAt(cursor, start + length + 1, AccessToken.nameTokensWithWildcard())) {

            length += 2;
        }

        return length;
    }

    /**
     * Measures a name that may carry hyphens — {@code storage-byte} — or a quoted string.
     *
     * <p>The counterpart of {@code SourceReader.hyphenatedName}, and the same run: the lexer gives
     * {@code storage}, {@code -}, {@code byte}, exactly as it gives a permission's colons separately.
     * The run cannot overshoot, because nothing that may follow a capability key begins with a hyphen.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the name is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    private static int hyphenatedNameLength(TokenCursor cursor, int start) {
        if (checkAt(cursor, start, T_STRING)) {
            return 1;
        }
        if (!checkAt(cursor, start, AccessToken.nameTokens())) {
            return NO_MATCH;
        }

        int length = 1;

        while (checkAt(cursor, start + length, T_MINUS)
               && checkAt(cursor, start + length + 1, AccessToken.nameTokens())) {

            length += 2;
        }

        return length;
    }

    /**
     * Measures a name written with dots — {@code entry.listByPurpose}.
     *
     * <p>The counterpart of {@link #hyphenatedNameLength}, and the same run: the lexer gives
     * {@code entry}, {@code .}, {@code listByPurpose} separately, exactly as it gives a permission's
     * colons separately.
     *
     * <p>A single segment measures 1 here rather than being refused, because measuring is not
     * deciding — {@link ActionDeclarationMatcher} is what requires the dot, and it says there why.
     *
     * <p>The run cannot overshoot: nothing that may follow an action name begins with a dot.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the name is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    private static int dottedNameLength(TokenCursor cursor, int start) {
        if (!checkAt(cursor, start, AccessToken.nameTokens())) {
            return NO_MATCH;
        }

        int length = 1;

        while (checkAt(cursor, start + length, T_DOT)
               && checkAt(cursor, start + length + 1, AccessToken.nameTokens())) {

            length += 2;
        }

        return length;
    }

    /**
     * Measures a name written as a bare identifier, a quoted string, a {@code ${placeholder}} — or the
     * {@code *} that means <em>everybody</em>.
     *
     * <p>⚠️ Read only where a <strong>subject</strong> belongs, which is the whole of what makes the
     * wildcard safe to admit here. {@code subject * { }} is how a rule is written about every account
     * at once, and {@link org.jmouse.access.policy.PolicyBinder} refuses everything in such a block
     * except a denial — a universal allow is a hole with a comment explaining itself, and a role
     * assigned to everybody is the same hole wearing a bundle.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the name is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    private static int nameLength(TokenCursor cursor, int start) {
        if (checkAt(cursor, start, T_IDENTIFIER, T_STRING, T_MULTIPLY)) {
            return 1;
        }

        return checkAt(cursor, start, T_DOLLAR) ? placeholderLength(cursor, start) : NO_MATCH;
    }

    /**
     * Measures {@code ${…}} from its dollar through its closing brace.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the placeholder is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH} when it never closes
     */
    private static int placeholderLength(TokenCursor cursor, int start) {
        if (!checkAt(cursor, start, T_DOLLAR) || !checkAt(cursor, start + 1, T_OPEN_CURLY)) {
            return NO_MATCH;
        }

        for (int index = start + 2; index < start + PLACEHOLDER_LOOKAHEAD; index++) {
            if (checkAt(cursor, index, T_CLOSE_CURLY)) {
                return index - start + 1;
            }

            if (checkAt(cursor, index, T_NEW_LINE, T_EOL, T_SEMICOLON)) {
                return NO_MATCH;
            }
        }

        return NO_MATCH;
    }

    /**
     * Reads the token at a lookahead offset, tolerating the end of the stream.
     *
     * @param cursor   the cursor to read from
     * @param offset   the lookahead offset
     * @param expected the types the token may have
     * @return {@code true} when a token is there and has one of those types
     */
    private static boolean checkAt(TokenCursor cursor, int offset, Token.Type... expected) {
        Token token = cursor.lookAt(offset);
        return token != null && cursor.checkAt(offset, expected);
    }
}
