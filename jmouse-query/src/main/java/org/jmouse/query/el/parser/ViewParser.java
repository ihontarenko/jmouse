package org.jmouse.query.el.parser;

import org.jmouse.core.MimeParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.ViewNode;

/**
 * Parses {@code view "name" on target { … }}.
 *
 * <pre>
 *   view "Мої косарки" on inventory {
 *     where   entry[component_name] is contains("кос")
 *     order   entry[quantity] | int asc
 *     columns component_name, quantity, location
 *   }
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ViewParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(QueryToken.T_VIEW);

        ViewNode view = new ViewNode();

        // ⚠️ Unquoted here. The token carries the quotes as they were typed, and they are how the title
        // was written rather than part of what it says — a view whose stored title includes its own
        // quotation marks shows them on every screen that displays it.
        view.setTitle(MimeParser.unquote(cursor.ensure(BasicToken.T_STRING).value()));

        // ⚠️ A colon after a name always means IDENTIFIER — what another declaration writes down, as
        // opposed to what a screen shows. A title is translated and reworded; a reference must not be.
        if (cursor.consumeIf(BasicToken.T_COLON)) {
            view.setIdentifier(Declarations.name(cursor));
        }

        Declarations.parameters(cursor, context).forEach(view::addParameter);

        // ⚠️ `uses(…)` names the values this view reads from the context it is run in. Declared, because a
        // free name is either null or somebody else's value wherever nobody remembered to set it.
        if (cursor.consumeIf(QueryToken.T_USES)) {
            Declarations.parameters(cursor, context).forEach(view::addAmbient);
        }

        // ⚠️ `on x` in the header is the OLDER spelling and is still read; the current one writes
        // `from: x` inside the block, like every other thing a block says about itself. Nothing emits the
        // header form — see toSource().
        if (cursor.consumeIf(QueryToken.T_ON)) {
            target(cursor, view);
        }

        cursor.ensure(BasicToken.T_OPEN_CURLY);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            Documents.skipBlankSpace(cursor);

            if (cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
                break;
            }

            Documents.refuseReserved(cursor);

            Token opening = cursor.peek();

            // ⚠️ `from:` is not a clause. It says what the block is ABOUT, which is a different kind of
            // statement from the ones that filter, sort and project it — and giving it a ClauseKind would
            // mean inventing a capability every backend must declare in order to be handed a subject.
            if (cursor.consumeIf(QueryToken.T_FROM_TABLE)) {
                Declarations.optionalColon(cursor);
                target(cursor, view);

                continue;
            }

            view.addClause(ClauseParser.parse(cursor, context), opening);
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        parent.add(view);
    }

    /**
     * What the block is about — {@code request} or {@code $source}.
     *
     * <p>⚠️ The {@code $} is what keeps a source really called {@code source} apart from a binding called
     * {@code source}. Without it one word would mean two things, decided by whichever registry answered
     * first.</p>
     */
    private void target(TokenCursor cursor, ViewNode view) {
        // ⚠️ BOTH fields, every time. A document may say `on x` in the header and `from: y` in the body
        // while the older spelling is still read, and the second must replace the first COMPLETELY —
        // leaving `targetBound` from the first left a pinned view looking late-bound and refusing to run.
        view.setTargetBound(cursor.consumeIf(BasicToken.T_DOLLAR));

        if (view.isTargetBound()) {
            view.setTarget(bindingName(cursor));

            return;
        }

        // ⚠️ A name and nothing more. Resolving what it names — a section, a purpose, a table — belongs to
        // the product that holds the data, and a language that resolved it could serve only the one
        // product whose answer it had baked in.
        view.setTarget(Declarations.name(cursor));
    }

    /**
     * The name after a {@code $}, whatever the lexer happened to call that word.
     *
     * <h2>⚠️ A keyword is a legal binding name here, and refusing one would be a bug</h2>
     *
     * <p>{@code on $source} is the most natural thing anybody writes, and {@code source} is a keyword of
     * this language — so reading the binding with the ordinary identifier rule refuses the obvious case
     * and leaves a message about an unexpected {@code T_SOURCE} for somebody to decode.</p>
     *
     * <p>Nothing is lost by being permissive: after a {@code $} no keyword can be meant as a keyword,
     * because there is exactly one thing that may appear there. So the token is taken for its spelling,
     * and only a word-shaped one is accepted — which keeps {@code on $} followed by a brace from reading it as a name
     * and swallowing the block.</p>
     */
    private String bindingName(TokenCursor cursor) {
        Token token = cursor.next();
        String name = token.value();

        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new QueryParseException(
                    "'$' has to be followed by the name of a binding; found '%s' at line %d"
                            .formatted(name, token.lineNumber()));
        }

        return name;
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(QueryToken.T_VIEW);
    }
}
