package org.jmouse.sheet;

import org.jmouse.sheet.parser.SheetDocumentParser;
import org.jmouse.sheet.parser.EntryParser;
import org.jmouse.sheet.parser.FieldParser;
import org.jmouse.sheet.parser.UnreadableLineParser;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.parser.AutodetectFirstParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.QuantityParser;

import java.util.List;

/**
 * What sheet adds to the expression engine: its parsers, and the engine's own two a block is read with.
 *
 * <p>⚠️ <strong>The whole of a sheet reader's vocabulary, deliberately without {@code CoreExtension}.</strong> A sheet
 * value is a word, a number with its unit, a text or a link — never an expression — so the engine's operators,
 * functions, filters and expression parsers would only be places for a document to mean something it does not say.
 * The engine's {@link QuantityParser} reads a value's number and unit, as every language with units does.
 * The engine's {@link StatementsParser} and {@link AutodetectFirstParser} are listed because they are not anyone's
 * but {@code CoreExtension}'s otherwise, and a container they were never added to answers {@code null}.</p>
 */
public class SheetExtension implements Extension {

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new StatementsParser(),
                new AutodetectFirstParser(),
                new SheetDocumentParser(),
                new EntryParser(),
                new FieldParser(),
                new UnreadableLineParser(),
                new QuantityParser()
        );
    }
}
