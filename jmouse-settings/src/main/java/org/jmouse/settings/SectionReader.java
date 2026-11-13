package org.jmouse.settings;

/**
 * 🧾 How an application reads a structured section's folded parameters ({@link ParameterTree#fold}) into its record — and so checks
 * them: a tree it cannot read is refused with its reason. The library names no format; the application that declares structured
 * sections says how they are read.
 */
public interface SectionReader {

    /**
     * The tree read into its record, every text value taken as the type the record says.
     *
     * @throws IllegalArgumentException saying where and why the tree is not that record
     */
    <T> T read(Object tree, Class<T> type);
}
