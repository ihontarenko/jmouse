package org.jmouse.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.expression.ast.LiteralNode;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

/**
 * The {@code LiteralCompiler} class compiles {@link LiteralNode} objects into their respective Java structured representations.
 * This includes conversions for numeric, boolean, and null literals.
 */
public class LiteralCompiler implements Compiler<LiteralNode, Object> {

    /**
     * Compiles a {@link LiteralNode} into its Java structured representation.
     *
     * @param node the literal node to compile
     * @param evaluationContext  the context context (not used in this implementation)
     * @return the compiled Java structured representation of the literal node
     */
    @Override
    public Object compile(LiteralNode node, EvaluationContext evaluationContext) {
        Object result = node.getValue();
        Enum<?> token = (Enum<?>) node.entry().token();

        if (T_INT.equals(token)) {
            result = parseInt((String) result);
        } else if (T_FLOAT.equals(token)) {
            String value = (String) result;
            int decimalIndex = value.indexOf('.');
            if (decimalIndex != -1) {
                if ((value.length() - decimalIndex - 1) <= 7) {
                    result = parseFloat(value);
                } else {
                    result = parseDouble(value);
                }
            } else {
                result = parseDouble(value);
            }
        } else if (T_NULL.equals(token)) {
            result = null;
        } else if (T_FALSE.equals(token) || T_TRUE.equals(token)) {
            result = parseBoolean((String) result);
        }

        return result;
    }

    /**
     * Specifies the type of node this evaluation can handle.
     *
     * @return the {@link LiteralNode} class
     */
    @Override
    public Class<? extends LiteralNode> nodeType() {
        return LiteralNode.class;
    }

}
