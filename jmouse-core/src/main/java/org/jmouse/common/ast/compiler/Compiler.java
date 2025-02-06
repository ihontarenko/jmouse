package org.jmouse.common.ast.compiler;

import org.jmouse.common.ast.node.Node;

/**
 * This interface defines the contract for compiling nodes in an abstract syntax tree (AST).
 * Implementations of this interface should transform AST nodes into a desired result type.
 *
 * @param <N> the type of AST node to be compiled
 * @param <R> the result type of the compilation
 */
public interface Compiler<N extends Node, R> {

    /**
     * Compiles the given AST node into a result.
     *
     * @param node the node to be compiled
     * @param evaluationContext  the evaluation context providing contextual information for the compilation
     * @return the result of the compilation
     */
    R compile(N node, EvaluationContext evaluationContext);

    /**
     * Specifies the type of AST node this compiler can handle.
     *
     * @return the class type of the node this compiler is designed to process
     */
    Class<? extends N> nodeType();

}
