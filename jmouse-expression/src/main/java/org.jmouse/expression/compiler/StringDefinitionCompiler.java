package svit.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.common.ast.node.Node;
import svit.expression.ast.StringDefinitionNode;
import svit.expression.ast.VariableNode;

import java.util.AbstractMap;
import java.util.Map;

/**
 * The {@code StringDefinitionCompiler} class compiles {@link StringDefinitionNode} objects into key-value pairs.
 * The key represents the variable name, and the value is the result of evaluating the associated command node.
 */
public class StringDefinitionCompiler implements Compiler<StringDefinitionNode, Map.Entry<String, Object>> {

    /**
     * Compiles a {@link StringDefinitionNode} into a {@link Map.Entry} mapping the variable name to its evaluated value.
     *
     * @param node               the string definition node to compile
     * @param evaluationContext  the evaluation context for evaluating the node
     * @return a {@link Map.Entry} with the variable name as the key and the evaluated value as the value
     */
    @Override
    public Map.Entry<String, Object> compile(StringDefinitionNode node, EvaluationContext evaluationContext) {
        VariableNode variable = (VariableNode) node.getHandler();
        Node         command  = node.getCommand();

        return new AbstractMap.SimpleImmutableEntry<>(
                variable.getVariableName(), command.evaluate(evaluationContext));
    }

    /**
     * Specifies the type of node this compiler can handle.
     *
     * @return the {@link StringDefinitionNode} class
     */
    @Override
    public Class<? extends StringDefinitionNode> nodeType() {
        return StringDefinitionNode.class;
    }

}
