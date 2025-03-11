package org.jmouse.template.evaluation;

import org.jmouse.template.node.Node;

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
     * @param evaluationContext  the context context providing contextual information for the compilation
     * @return the result of the compilation
     */
    R compile(N node, EvaluationContext evaluationContext);

    /**
     * Specifies the type of AST node this evaluation can handle.
     *
     * @return the class type of the node this evaluation is designed to process
     */
    Class<? extends N> nodeType();

}
