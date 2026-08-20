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
     * ✅ The same, plus the <strong>inert text</strong> formats: notes, logs, exports, configuration.
     *
     * <p>For a tracker or a knowledge base, where the commonest attachment after a screenshot is a log
     * or a Markdown note — and where {@link #ALLOW_DOCUMENTS_AND_IMAGES} refuses both, so the file a
     * developer actually wants to hand over is the one that cannot be sent.</p>
     *
     * <p>⚠️ <strong>INERT text only, and the word is doing work.</strong> {@code text/html},
     * {@code application/xhtml+xml}, {@code image/svg+xml} and every JavaScript type stay out — they are
     * text by encoding and a script host by specification. A product serving any of these bytes from its
     * own origin (a public avatar or file route) depends on that exclusion for its safety, so widening
     * this set is not a cosmetic change.</p>
     */
    ALLOW_DOCUMENTS_IMAGES_AND_TEXT,

    /**
     * 🛠️ Neither — read the configured mode and lists instead.
     */
    CUSTOM
}
