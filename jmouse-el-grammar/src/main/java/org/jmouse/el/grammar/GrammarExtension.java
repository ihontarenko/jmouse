package org.jmouse.el.grammar;

import org.jmouse.el.extension.Extension;
import org.jmouse.el.parser.Parser;

import java.util.List;

/**
 * What the grammar library adds to the expression engine: the parsers its values are read with — a quantity with its unit
 * ({@code 4.7 kΩ}), a point ({@code -30, 0}), a size ({@code 1 x 2}) —, which a context asks by class. None of them is
 * offered to the engine's dispatch: they never take a number in another language's body read through the same context.
 */
public final class GrammarExtension implements Extension {

    @Override
    public List<Parser> getParsers() {
        return List.of(new QuantityValueParser(), new PointParser(), new ExtentParser());
    }
}
