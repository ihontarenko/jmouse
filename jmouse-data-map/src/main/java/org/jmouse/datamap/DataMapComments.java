package org.jmouse.datamap;

import java.util.List;
import java.util.Optional;

/**
 * What the author wrote about a line: the comments above it, and the one beside it. Kept so that a document read and
 * written again still explains itself.
 *
 * @param above  the comment lines written above it, {@code #} included, in order
 * @param beside the comment at the end of its own line, {@code #} included; empty for none
 */
public record DataMapComments(List<String> above, Optional<String> beside) {

    public static final DataMapComments NONE = new DataMapComments(List.of(), Optional.empty());

    public DataMapComments {
        above = List.copyOf(above);
    }

    public boolean isEmpty() {
        return above.isEmpty() && beside.isEmpty();
    }
}
