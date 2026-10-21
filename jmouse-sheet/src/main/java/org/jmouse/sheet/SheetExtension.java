package org.jmouse.datamap;

import org.jmouse.datamap.parser.DataMapDocumentParser;
import org.jmouse.datamap.parser.EntryParser;
import org.jmouse.datamap.parser.FieldParser;
import org.jmouse.datamap.parser.UnreadableLineParser;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.parser.AutodetectFirstParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.QuantityParser;

import java.util.List;

/**
 * What jMD adds to the expression engine: its parsers, and the engine's own two a block is read with.
 *
 * <p>⚠️ <strong>The whole of a jMD reader's vocabulary, deliberately without {@code CoreExtension}.</strong> A jMD
 * value is a word, a number with its unit, a text or a link — never an expression — so the engine's operators,
 * functions, filters and expression parsers would only be places for a document to mean something it does not say.
 * The engine's {@link QuantityParser} reads a value's number and unit, as every language with units does.
 * The engine's {@link StatementsParser} and {@link AutodetectFirstParser} are listed because they are not anyone's
 * but {@code CoreExtension}'s otherwise, and a container they were never added to answers {@code null}.</p>
 */
public class DataMapExtension implements Extension {

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new StatementsParser(),
                new AutodetectFirstParser(),
                new DataMapDocumentParser(),
                new EntryParser(),
                new FieldParser(),
                new UnreadableLineParser(),
                new QuantityParser()
        );
    }
}
