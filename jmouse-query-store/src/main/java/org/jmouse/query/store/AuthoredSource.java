package org.jmouse.query.store;

import java.time.Instant;

/**
 * A declaration somebody wrote and kept — the {@code structure} and {@code mapping} for one source.
 *
 * <h2>⚠️ One body, not two columns</h2>
 *
 * <p>The two halves are one document. Splitting them into a {@code structure} column and a
 * {@code mapping} column would let a row exist in which the mapping binds an attribute the structure
 * never declared — which the language refuses at load time, so the row would be unloadable and the
 * screen that wrote it would have had no way to know. Kept whole, a save either parses or is refused
 * before it becomes a row.</p>
 *
 * @param sourceKey the source this declares — {@code issues}, {@code inventory}
 * @param owner     whose declaration it is; {@link QueryOwner#installation()} for the installation's
 * @param body      the jMQ, verbatim as it was written
 * @param author    the product's own identifier for whoever last wrote it
 * @param updatedAt when that was
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record AuthoredSource(
        String sourceKey,
        QueryOwner owner,
        String body,
        String author,
        Instant updatedAt) {

    /**
     * ⚠️ The same cap the saved-query body carries, and for the same reason: the column is sized in
     * bytes while this counts characters, so a declaration written in Cyrillic would otherwise reach the
     * limit at a quarter of the length an English one does — and the engine truncates rather than
     * refusing.
     */
    public static final int MAXIMUM_BODY_LENGTH = 16_000;

    public AuthoredSource {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("an authored source needs a body");
        }

        if (body.length() > MAXIMUM_BODY_LENGTH) {
            throw new IllegalArgumentException(
                    "a declaration may be at most %d characters; this one is %d"
                            .formatted(MAXIMUM_BODY_LENGTH, body.length()));
        }
    }
}
