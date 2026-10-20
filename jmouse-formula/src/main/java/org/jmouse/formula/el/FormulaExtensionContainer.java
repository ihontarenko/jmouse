package org.jmouse.formula.el;

import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.parser.OperatorParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.formula.el.FormulaOperatorParser.Juxtaposition;

/**
 * A jME container assembled for formulas from {@link FormulaCoreExtension} alone — jME's core extension is not
 * imported. jME looks a parser up by class, so the formula grammar is substituted at that lookup: every parser
 * that asks for the operator parser (the expression parser, parentheses, function arguments) receives it.
 */
public class FormulaExtensionContainer extends StandardExtensionContainer {

    private final FormulaOperatorParser operatorParser;

    /** Formulas as mathematics writes them: {@code 2 pi f} is a product. */
    public FormulaExtensionContainer() {
        this(Juxtaposition.MULTIPLIES);
    }

    /**
     * @param juxtaposition what two operands side by side mean: a language whose lines go on after a value —
     *                      {@code R = 10 kOhm in 1 .. 100} — chooses {@link Juxtaposition#ENDS_EXPRESSION}
     */
    public FormulaExtensionContainer(Juxtaposition juxtaposition) {
        this.operatorParser = new FormulaOperatorParser(juxtaposition);
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
