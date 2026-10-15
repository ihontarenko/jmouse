package org.jmouse.spice.node;

import org.jmouse.spice.SpicePosition;

/** One statement of a deck: an element line, or a dot card. */
public abstract sealed class SpiceStatement extends SpiceNode permits SpiceElement, SpiceCard {

    protected SpiceStatement(SpicePosition position) {
        super(position);
    }
}
