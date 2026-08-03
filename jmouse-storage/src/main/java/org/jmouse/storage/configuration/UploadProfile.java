package org.jmouse.storage.configuration;

/**
 * 🚦 The acceptance configurations that ship ready to use.
 *
 * <p>Both are already in production somewhere, and both are long: sixty extensions between them,
 * every one of which matters and any one of which is easy to mistype. Naming them means a product
 * picks its posture in a line, and a correction to either reaches every product that chose it.</p>
 */
public enum UploadProfile {

    /**
     * ⛔ Refuse anything that executes — native binaries, shell and server-side scripts, Java
     * archives, and markup including SVG.
     *
     * <p>For a product accepting arbitrary user files, where enumerating what is <em>safe</em> is
     * not possible.</p>
     */
    BLOCK_DANGEROUS_CONTENT,

    /**
     * ✅ Admit only images, PDF and the common Office formats.
     *
     * <p>For a product whose supported formats are a closed set, where anything unrecognised is a
     * mistake rather than a file somebody meant to keep.</p>
     */
    ALLOW_DOCUMENTS_AND_IMAGES,

    /**
     * 🛠️ Neither — read the configured mode and lists instead.
     */
    CUSTOM
}
