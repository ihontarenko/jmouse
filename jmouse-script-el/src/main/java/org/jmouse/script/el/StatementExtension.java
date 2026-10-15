package org.jmouse.script.el;

import org.jmouse.el.extension.Extension;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.script.el.parser.AssignmentParser;
import org.jmouse.script.el.parser.BranchParser;
import org.jmouse.script.el.parser.FunctionDeclarationParser;
import org.jmouse.script.el.parser.IncludeParser;
import org.jmouse.script.el.parser.LocalParser;
import org.jmouse.script.el.parser.LoopParser;
import org.jmouse.script.el.parser.ReturnParser;
import org.jmouse.script.el.parser.ScriptBodyParser;

import java.util.List;

/**
 * The statement core as a jME extension: {@code include}, {@code function}, {@code if}, {@code for}, {@code local},
 * {@code return} and assignment, and the body they are written in. It contributes parsers only — no operators,
 * functions, filters or tests — because those are what makes a dialect a dialect: jMS takes jME's grammar and jMF's mathematics.
 *
 * <p>A dialect composes it rather than extending it: its own extension lists its expression set, this extension's
 * parsers, and its own constructions. On its own, imported beside jME's core extension, it is already a small
 * procedural language.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StatementExtension implements Extension {

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new StatementsParser(),
                new ScriptBodyParser(),
                new IncludeParser(),
                new FunctionDeclarationParser(),
                new BranchParser(),
                new LoopParser(),
                new LocalParser(),
                new ReturnParser(),
                new AssignmentParser()
        );
    }

}
