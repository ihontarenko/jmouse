package org.jmouse.cad.circuit;

import org.jspecify.annotations.Nullable;
import org.jmouse.cad.Formula;

/**
 * A channel of a bench's oscilloscope: what it reads — a net's voltage, a part's current, a logic level —, against which
 * net, at what scale, and what its screen shows of the reading.
 *
 * @param channel   the channel's name on the oscilloscope: {@code A}, {@code B} …
 * @param reading   {@code voltage}, {@code current} or {@code level}
 * @param subject   what it reads: a net, or the part whose current it reads
 * @param reference the second net of a voltage, or null for ground
 * @param scale     a division's worth — {@code 2 V/div} —, or null where the bench chooses
 * @param shows     what the screen shows, {@code X} being the reading, or null for the reading itself
 */
public record Probe(String channel, String reading, String subject, @Nullable String reference, @Nullable Formula scale, @Nullable Formula shows) {
}
