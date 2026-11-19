package org.jmouse.el.lexer;

import org.jmouse.el.InputUnit;
import org.jmouse.el.Source;

/** Progress and provenance shared by token and byte cursors, without imposing a reading grammar. */
public interface SourceCursor<S extends Source> {
    /** A legacy token cursor may have no associated source. */
    S source();

    long offset();

    long remainingUnits();

    boolean hasNext();

    InputUnit positionUnit();
}
