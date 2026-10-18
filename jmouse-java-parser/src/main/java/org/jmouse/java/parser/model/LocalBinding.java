package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

/** A parameter or local variable; null type means inference is required, never an invented type. */
public record LocalBinding(
        String name, String type, SourceSpan nameSpan, SourceSpan scope, Expression initializer) {}
