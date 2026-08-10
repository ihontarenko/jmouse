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

    private record PolicyMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(T_POLICY, T_STRING, T_OPEN_CURLY);
        }
    }

    private record ScopesMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(T_SCOPES, T_OPEN_CURLY);
        }
    }

    private record PermissionsMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(T_PERMISSIONS, T_OPEN_CURLY);
        }
    }

    private record RoleMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(T_ROLE, T_IDENTIFIER, T_OPEN_CURLY)
                    || cursor.matchesSequence(T_ROLE, T_STRING, T_OPEN_CURLY);
        }
    }

    private record SubjectMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            int name = nameLength(cursor, 1);
            return checkAt(cursor, 0, T_SUBJECT) && name != NO_MATCH
                    && checkAt(cursor, 1 + name, T_OPEN_CURLY);
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
     *       {@code role} is not the block keyword — see {@link AccessToken#namesAndKeywords()}.
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
        boolean opens = checkAt(cursor, start, AccessToken.namesAndKeywords())
                && checkAt(cursor, start + 1, T_COLON)
                && checkAt(cursor, start + 2, AccessToken.namesKeywordsAndWildcard());

        if (!opens) {
            return NO_MATCH;
        }

        int length = 3;

        // A wildcard ends the permission: `form:*:anything` names nothing, and reading further would
        // measure a shape the binder is only going to refuse under a less helpful name.
        while (!checkAt(cursor, start + length - 1, T_MULTIPLY)
               && checkAt(cursor, start + length, T_COLON)
               && checkAt(cursor, start + length + 1, AccessToken.namesKeywordsAndWildcard())) {

            length += 2;
        }

        return length;
    }

    /**
     * Measures a name written as a bare identifier, a quoted string or a {@code ${placeholder}}.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the name is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    private static int nameLength(TokenCursor cursor, int start) {
        if (checkAt(cursor, start, T_IDENTIFIER, T_STRING)) {
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
