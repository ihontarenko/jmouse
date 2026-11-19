package org.jmouse.java.parser.grammar;

import java.util.Objects;
import java.util.Set;
import org.jmouse.el.lexer.Token;

/** Synchronization tokens at the current nesting level; extensions may supply their own token types. */
public record JavaRecoveryBoundary(Set<Token.Type> stops, Set<Token.Type> consumedStops,
                                   JavaRecoveryBlockExit blockExit) {
    public JavaRecoveryBoundary {
        stops = Set.copyOf(stops);
        consumedStops = Set.copyOf(consumedStops);
        Objects.requireNonNull(blockExit, "blockExit");
        if (!stops.containsAll(consumedStops)) {
            throw new IllegalArgumentException("Consumed recovery tokens must be synchronization tokens");
        }
    }
}
