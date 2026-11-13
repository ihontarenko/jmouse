package org.jmouse.cad.drawing;

import java.util.List;

/** A drawing whose items can be grouped and locked: a sheet's layout, a pcb, a panel. */
public interface ArrangedDrawing {

    /** Every item that can be held, as it stands. */
    List<StandingItem> standingItems();

    /** The groups it declares, asked whether an item is held. */
    DrawnGroups drawnGroups();
}
