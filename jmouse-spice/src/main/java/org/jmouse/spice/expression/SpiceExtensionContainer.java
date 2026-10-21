package org.jmouse.spice.expression;

import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.parser.OperatorParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.PrimaryExpressionParser;

/**
 * A jME container assembled for SPICE expressions from {@link SpiceCoreExtension} alone. jME looks a parser up by class,
 * so SPICE's grammar is substituted at that lookup, as jMF's is: every parser that asks for the operator or the primary
 * parser — the expression parser, parentheses, a call's arguments — receives SPICE's.
 */
final class SpiceExtensionContainer extends StandardExtensionContainer {

    SpiceExtensionContainer() {
        importExtension(new SpiceCoreExtension());
    }

    @Override
    public Parser getParser(Class<? extends Parser> type) {
        if (type == PrimaryExpressionParser.class) {
            return super.getParser(SpicePrimaryExpressionParser.class);
        }
        if (type == OperatorParser.class) {
            return super.getParser(SpiceOperatorParser.class);
        }
        return super.getParser(type);
    }
}
