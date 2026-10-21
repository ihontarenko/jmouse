package org.jmouse.parallax.script.reader;

import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.node.ScriptNode;

import java.util.Optional;

/**
 * The blocks whose bodies another language reads: {@code behaviour} and {@code run} hold jMS, read by jMS's own
 * statement reader through the same context; {@code formulas} holds jMF definitions, kept as written for jMF.
 */
final class ScriptBlocks {

    private final Reading reading;

    ScriptBlocks(Reading reading) {
        this.reading = reading;
    }

    Block.Behaviour behaviour() {
        int start = reading.cursor.current().offset();
        ScriptNode script = script("behaviour");
        return new Block.Behaviour(script, body(start));
    }

    Block.Run run() {
        int start = reading.cursor.current().offset();
        ScriptNode script = script("run");
        return new Block.Run(script, body(start));
    }

    /** The text between the brace at {@code open} and the one just read, which closed it. */
    private SourceText body(int open) {
        int end = reading.cursor.lookAt(-1).offset() + 1;
        return reading.text(open + 1, end - 1);
    }

    /** {@code formulas { jMF definitions }}. A shared library is pulled in at the top with {@code use <slug>}. */
    Block.Formulas formulas(Token keyword) {
        if (reading.at(BasicToken.T_OPEN_CURLY)) {
            return new Block.Formulas(Optional.of(reading.rawBlock("formulas")));
        }
        throw reading.refusal(keyword, "formulas are '{ definitions }'; to pull in a shared library write "
                + "'use <slug>' at the top of the document");
    }

    /** A brace-delimited jMS body, read by jMS's own statement reader. */
    private ScriptNode script(String name) {
        if (!reading.at(BasicToken.T_OPEN_CURLY)) {
            throw reading.refusal("'" + name + "' is followed by '{ … }' holding jMS");
        }
        Node holder = BasicNode.forToken(reading.cursor.current());
        try {
            reading.context.getParser(StatementsParser.class).parse(reading.cursor, holder, reading.context);
        } catch (ScriptParseException failure) {
            throw new ExperienceParseException(reading.document(), failure.at().line(), failure.at().column(), failure.detail());
        } catch (RuntimeException failure) {
            throw reading.refusal("in '" + name + "': " + failure.getMessage());
        }
        ScriptNode script = new ScriptNode();
        script.setName(name);
        for (Node statement : holder.getChildren().getFirst().getChildren()) {
            script.addExpression((Expression) statement);
        }
        return script;
    }
}
