package org.jmouse.query.el;

import org.jmouse.el.parser.Parser;
import org.jmouse.query.el.dialect.QueryDialect;
import org.jmouse.query.el.parser.OrderParser;
import org.jmouse.query.el.parser.QueryDocumentParser;
import org.jmouse.query.el.parser.QueryFunctionParser;
import org.jmouse.query.el.parser.SourceParser;
import org.jmouse.query.el.parser.ViewParser;

import java.util.ArrayList;
import java.util.List;

/**
 * What a {@code .jmq} <strong>document</strong> is written in: the condition dialect, plus the tags.
 *
 * <h2>⚠️ It EXTENDS the dialect rather than restating it, and that is the whole point</h2>
 *
 * <p>The alternative — two independent extensions, one for documents and one for bare expressions — is
 * what {@code .jmp} does, and for {@code .jmp} it is right: a policy <em>file</em> and a policy
 * <em>condition</em> are meant to be different languages.</p>
 *
 * <p>Here they are meant to be the same one. A {@code where} written into a URL and the identical
 * {@code where} written into a file must never come to mean different things, and two independently
 * maintained lists agree on the day they are written and drift on the day one is touched. Deriving one
 * from the other makes the agreement <strong>structural</strong>: there is no list to keep in step,
 * because there is only one list.</p>
 *
 * <p>So this class adds exactly three parsers and nothing else. If it ever needs to add an operator, a
 * filter or a test, that is the signal that the thing belongs in {@link QueryDialect} instead — a
 * document has no business understanding a value a condition cannot.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryExtension extends QueryDialect {

    @Override
    public List<Parser> getParsers() {
        List<Parser> parsers = new ArrayList<>(super.getParsers());

        parsers.add(new QueryDocumentParser());
        parsers.add(new ViewParser());
        parsers.add(new SourceParser());
        parsers.add(new QueryFunctionParser());

        // ⚠️ Registered even though nothing DISPATCHES to it: a language is built by naming its root
        // parser, and a root the container has never heard of is refused at construction. `?jmq:order=`
        // starts here, which is what keeps a sort out of a hand-assembled document.
        parsers.add(new OrderParser());

        return parsers;
    }
}
