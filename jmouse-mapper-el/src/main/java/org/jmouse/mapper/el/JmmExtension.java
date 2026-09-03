package org.jmouse.mapper.el;

import org.jmouse.el.extension.Extension;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.mapper.el.parser.AlwaysParser;
import org.jmouse.mapper.el.parser.FragmentParser;
import org.jmouse.mapper.el.parser.FromParser;
import org.jmouse.mapper.el.parser.IncludeParser;
import org.jmouse.mapper.el.parser.LetParser;
import org.jmouse.mapper.el.parser.MappingDocumentParser;
import org.jmouse.mapper.el.parser.MappingParser;
import org.jmouse.mapper.el.parser.RefuseParser;
import org.jmouse.mapper.el.parser.RuleParser;
import org.jmouse.mapper.el.parser.TargetParser;
import org.jmouse.mapper.el.parser.UnmappedParser;
import org.jmouse.mapper.el.parser.UseParser;

import java.util.List;

/**
 * What the {@code .jmm} language adds to the expression engine.
 *
 * <p>Only the parsers. A mapping document has no functions, filters or tests of its own: a rule's value
 * is sliced out of the file and compiled by a plain {@code ExpressionLanguage} later, and that is where
 * a product's vocabulary is decided.</p>
 *
 * <p>⚠️ {@link StatementsParser} is on the list although it belongs to the engine, because it is not in
 * {@code CoreExtension} — the block parsers call it by type through the context, and a container it was
 * never added to answers {@code null}.</p>
 *
 * <p>⚠️ The order of this list carries no meaning. Dispatch sorts by
 * {@link org.jmouse.mapper.el.parser.JmmParserPriority}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JmmExtension implements Extension {

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new StatementsParser(),
                new MappingDocumentParser(),
                new MappingParser(),
                new UseParser(),
                new FragmentParser(),
                new TargetParser(),
                new UnmappedParser(),
                new RefuseParser(),
                new AlwaysParser(),
                new FromParser(),
                new LetParser(),
                new IncludeParser(),
                new RuleParser()
        );
    }
}
