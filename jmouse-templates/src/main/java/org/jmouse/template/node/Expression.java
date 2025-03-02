package org.jmouse.template.node;

/**
 * Represents an expression node within the abstract syntax tree (AST) of the template.
 * <p>
 * An expression is a specialized type of {@link Node} that, when evaluated,
 * produces a value. This interface does not declare additional methods,
 * but it serves to semантично відокремити вирази від інших типів вузлів.
 * </p>
 */
public interface Expression extends Node {

}