package org.jmouse.formula.el;

import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.parser.OperatorParser;
import org.jmouse.el.parser.Parser;

/**
 * A jME container assembled for formulas from {@link FormulaCoreExtension} alone — jME's core extension is not
 * imported. jME looks a parser up by class, so the formula grammar is substituted at that lookup: every parser
 * that asks for the operator parser (the expression parser, parentheses, function arguments) receives it.
 */
public class FormulaExtensionContainer extends StandardExtensionContainer {

    private final FormulaOperatorParser operatorParser = new FormulaOperatorParser();

    public FormulaExtensionContainer() {
        importExtension(new FormulaCoreExtension());
    }

    @Override
    public Parser getParser(Class<? extends Parser> type) {
        if (type == org.jmouse.el.parser.PrimaryExpressionParser.class || type == FormulaPrimaryExpressionParser.class) {
            return super.getParser(FormulaPrimaryExpressionParser.class);
        }
        if (type == OperatorParser.class || type == FormulaOperatorParser.class) {
            return operatorParser;
        }
        return super.getParser(type);
    }
}
