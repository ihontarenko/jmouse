package org.jmouse.validator.el;

import org.jmouse.el.extension.Extension;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.validator.el.parser.AlwaysParser;
import org.jmouse.validator.el.parser.CheckLineParser;
import org.jmouse.validator.el.parser.GateParser;
import org.jmouse.validator.el.parser.InvariantParser;
import org.jmouse.validator.el.parser.ValidationDocumentParser;
import org.jmouse.validator.el.parser.ValidationParser;
import org.jmouse.validator.el.parser.WhenParser;

import java.util.List;

/**
 * What the {@code .jmv} language adds to the expression engine.
 *
 * <p>Only the parsers, because that is all it adds. A validation document has no functions, filters or
 * tests of its own: the expressions inside it are compiled by a plain
 * {@link org.jmouse.el.ExpressionLanguage} later, and <em>that</em> is where a product's vocabulary is
 * decided. Contributing a function here would put it in the parser's container, where nothing ever
 * evaluates anything.</p>
 *
 * <p>⚠️ {@link StatementsParser} is on the list although it belongs to the engine, because it is not in
 * {@code CoreExtension} — the block parsers call it by type through the context, and a container it was
 * never added to answers {@code null}. jMP registers it for the same reason.</p>
 *
 * <p>⚠️ The order of this list carries no meaning. Dispatch sorts by
 * {@link org.jmouse.validator.el.parser.JmvParserPriority}, and that class — not this list — is where
 * the reason one shape is offered before another is written down.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JmvExtension implements Extension {

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new StatementsParser(),
                new ValidationDocumentParser(),
                new ValidationParser(),
                new GateParser(),
                new AlwaysParser(),
                new WhenParser(),
                new InvariantParser(),
                new CheckLineParser()
        );
    }
}
