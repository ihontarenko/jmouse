package org.jmouse.el.writer;

/**
 * Something that writes itself as source: a node of a syntax tree, or a value a node holds — an argument, a number as
 * it was read. It says its words to a {@link SourceWriter}; what lies between them, where its lines break and how far
 * they are indented is the writer's layout, never its own.
 */
public interface SourceWritable {

    void writeSource(SourceWriter writer);
}
