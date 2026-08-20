package org.jmouse.liveblocks;

/**
 * How one directive turned out.
 *
 * <h2>⚠️ A miss is a status, never an exception</h2>
 *
 * <p>Documents outlive their subjects. An issue gets deleted, a sprint gets renamed, a project stops
 * being visible to whoever is reading — all of those are ordinary, and every one of them has to reach
 * the page as something a person can read. A resolver that threw would cost the batch; a block that
 * vanished would read as <em>"nothing to report"</em>, which is a lie about a document whose whole
 * promise is that its numbers are live.
 */
public enum DirectiveStatus {

    /** Answered. The payload fields carry what to draw. */
    RESOLVED,

    /**
     * This product owns the directive and has nothing by that argument.
     *
     * <p>⚠️ Distinct from {@link #NO_ACCESS} on purpose, and the distinction is a disclosure decision a
     * resolver makes deliberately: telling a reader that {@code TSSR-4} exists but is not theirs is
     * itself information. A resolver that would rather not say answers this for both.
     */
    NOT_FOUND,

    /**
     * Nothing here claims that name.
     *
     * <p>The honest answer to a document written against a product that has since dropped the concept,
     * or to a namespace pointed at the wrong address.
     */
    UNKNOWN_DIRECTIVE,

    /** It exists and this reader may not see it. */
    NO_ACCESS

}
