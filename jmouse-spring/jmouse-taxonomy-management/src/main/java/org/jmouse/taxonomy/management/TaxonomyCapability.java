package org.jmouse.taxonomy.management;

/**
 * 🔑 What a taxonomy route asks to be allowed, in the library's words; a product says which of its permissions each
 * means ({@link TaxonomyAccessRules}). Reading asks for nothing: every answer is narrowed to what the reader may see.
 */
public enum TaxonomyCapability {

    /** Shape the trees: create, rename, move and delete categories. */
    CATEGORY_WRITE,

    /** File entities in categories, and take them out. */
    ASSIGNMENT_WRITE
}
