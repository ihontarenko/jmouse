package org.jmouse.template.evaluation;

import org.jmouse.common.support.context.AbstractVariablesContext;
import org.jmouse.template.node.Node;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Represents the context context for AST (Abstract Syntax Tree) compilation.
 * This context provides utilities to manage functions, variables, and compilers
 * specific to node types in the AST.
 */
public class EvaluationContext extends AbstractVariablesContext {

    /**
     * Retrieves a function by its name from the context context.
     *
     * @param name the name of the function.
     * @return the {@link Method} structured representing the function.
     * @throws EvaluationContextException if the function is not found in the context.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Method method = context.getFunction("calculateSum");
     * method.invoke(null, 5, 10);
     * }</pre>
     */
    public Method getFunction(String name) {
        Method function = getProperty(name);

        if (function == null) {
            throw new EvaluationContextException(
                    "Evaluation context does not contain the required function '%s'.".formatted(name));
        }

        return function;
    }

    /**
     * Adds or updates a function in the context context.
     *
     * @param name   the name of the function.
     * @param method the {@link Method} structured representing the function.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * context.setFunction("calculateSum", MyClass.class.getMethod("calculateSum", int.class, int.class));
     * }</pre>
     */
    public void setFunction(String name, Method method) {
        setProperty(name, method);
    }

    /**
     * Adds or updates a function in the context context using the function's name.
     *
     * @param method the {@link Method} structured representing the function.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * context.setFunction(MyClass.class.getMethod("calculateSum", int.class, int.class));
     * }</pre>
     */
    public void setFunction(Method method) {
        setProperty(method.getName(), method);
    }

    /**
     * Retrieves a variable by its name, ensuring it exists in the context.
     *
     * @param name the name of the variable.
     * @return the variable structured.
     * @throws EvaluationContextException if the variable is not found in the context.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Object variable = context.requireVariable("currentValue");
     * System.out.println(variable);
     * }</pre>
     */
    public Object requireVariable(String name) {
        Object variable = getVariable(name);

        if (variable == null) {
            throw new EvaluationContextException(
                    "Evaluation context does not contain the required variable '%s'.".formatted(name));
        }

        return variable;
    }

    /**
     * Adds a evaluation for a specific node type to the context context.
     *
     * @param nodeType the class representing the node type.
     * @param compiler the {@link Compiler} structured for the node type.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * context.addCompiler(MyNode.class, new MyNodeCompiler());
     * }</pre>
     */
    public void addCompiler(Class<? extends Node> nodeType, Compiler<?, ?> compiler) {
        setProperty(nodeType, compiler);
    }

    /**
     * Adds a evaluation to the context context. The evaluation's node type is inferred
     * from its {@code nodeType()} method.
     *
     * @param compiler the {@link Compiler} structured to add.
     * @throws NullPointerException if the evaluation's {@code nodeType()} method returns {@code null}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Compiler<MyNode> evaluation = new MyNodeCompiler();
     * context.addCompiler(evaluation);
     * }</pre>
     */
    public void addCompiler(Compiler<?, ?> compiler) {
        addCompiler(Objects.requireNonNull(
                compiler.nodeType(), "Compiler '%s' must return a valid node type class from the nodeType() method."
                        .formatted(compiler.getClass().getName())), compiler);
    }

    /**
     * Retrieves a evaluation for a specific node type from the context context.
     *
     * @param nodeType the class representing the node type.
     * @param <N>      the type of the node.
     * @return the {@link Compiler} structured for the node type.
     * @throws EvaluationContextException if no evaluation is found for the specified node type.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Compiler<MyNode> evaluation = context.getCompiler(MyNode.class);
     * }</pre>
     */
    public <N extends Node> Compiler<N, ?> getCompiler(Class<? extends N> nodeType) {
        Compiler<N, ?> compiler = getProperty(nodeType);

        if (compiler == null) {
            throw new EvaluationContextException(
                    "Evaluation context does not contain the required evaluation for '%s' node."
                            .formatted(nodeType.getName()));
        }

        return compiler;
    }

    /**
     * Retrieves a evaluation for a specific node structured from the context context.
     *
     * @param nodeObject the node structured whose evaluation is to be retrieved.
     * @param <N>        the type of the node.
     * @return the {@link Compiler} structured for the node type.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MyNode node = new MyNode();
     * Compiler<MyNode> evaluation = context.getCompiler(node);
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <N extends Node> Compiler<N, ?> getCompiler(N nodeObject) {
        return (Compiler<N, ?>) getCompiler(nodeObject.getClass());
    }
}

