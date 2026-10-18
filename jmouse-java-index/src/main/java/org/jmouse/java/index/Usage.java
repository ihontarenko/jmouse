package org.jmouse.java.index;

/**
 * One place a type is named.
 *
 * @param source the file it is written in
 * @param reference the name as written there
 */
public record Usage(String source, IndexedReference reference) {}
