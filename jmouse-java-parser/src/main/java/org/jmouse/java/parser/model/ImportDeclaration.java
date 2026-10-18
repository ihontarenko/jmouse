package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

/**
 * One {@code import} declaration.
 *
 * @param name     the imported name, without a trailing {@code .*}
 * @param isStatic whether it is an {@code import static}
 * @param onDemand whether it ends in {@code .*}
 * @param nameSpan the imported name as written, from its first identifier to its last, without a trailing {@code .*}
 * @param span     from the keyword to the semicolon
 */
public record ImportDeclaration(String name, boolean isStatic, boolean onDemand, SourceSpan nameSpan, SourceSpan span) {
}
