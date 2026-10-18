package org.jmouse.java.index;

import org.jmouse.java.parser.model.BodyReference;

/** One definite usage of a declared member. */
public record MemberUsage(String source, BodyReference reference) {}
