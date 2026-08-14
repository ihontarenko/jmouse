package org.jmouse.ai.view;

import java.time.Instant;
import java.util.List;

/**
 * What has been called, by whom, and how it went.
 *
 * <p>The trail read back. {@link org.jmouse.ai.spi.InvocationTrace} is the writing half and this is the
 * reading half, and they are deliberately separate interfaces: a product that records tool calls into
 * its own audit trail — in its own vocabulary, alongside the things people did — implements the first
 * and answers this one from whatever it already had. Joining them would have forced every product
 * recording a trail to store it in a shape this library chose.
 *
 * <p><strong>No implementation ships.</strong> {@link #none()} is what a product without a trail gets,
 * and it is honest about being empty rather than pretending nothing was ever called. A product that
 * wants the screen to say something implements this over its own rows.
 */
public interface ToolCallHistory {

    /**
     * One call, as a screen shows it.
     *
     * @param operationId    the identifier the guards minted for it, which is what a person quotes
     * @param callerId       who called, as the caller resolver named them
     * @param actingSubject  who it was done on behalf of; equal to {@code callerId} for a person
     * @param qualifiedName  {@code tool.action}, the spelling a person reads
     * @param scopeId        where it ran, or null for an action confined to nowhere
     * @param scopeLabel     that scope's name, for a screen that should not show identifiers
     * @param outcome        the verdict or the refusal reason, whichever this call ended as
     * @param affectedCount  how many records it reached, as the guards resolved them
     * @param at             when
     */
    record Entry(
            String  operationId,
            String  callerId,
            String  actingSubject,
            String  qualifiedName,
            String  scopeId,
            String  scopeLabel,
            String  outcome,
            long    affectedCount,
            Instant at
    ) {
    }

    /** The most recent calls, newest first. */
    List<Entry> recent(int limit);

    /** The most recent calls of one action, named as {@code tool.action}. */
    List<Entry> forAction(String qualifiedName, int limit);

    /** The most recent calls by one caller. */
    List<Entry> forCaller(String callerId, int limit);

    /**
     * A product that keeps no trail.
     *
     * <p>⚠️ Empty, and a screen over it should say <em>"no trail is configured"</em> rather than
     * <em>"nothing has been called"</em>. The two look identical here and mean opposite things.
     */
    static ToolCallHistory none() {
        return new ToolCallHistory() {

            @Override
            public List<Entry> recent(int limit) {
                return List.of();
            }

            @Override
            public List<Entry> forAction(String qualifiedName, int limit) {
                return List.of();
            }

            @Override
            public List<Entry> forCaller(String callerId, int limit) {
                return List.of();
            }
        };
    }
}
