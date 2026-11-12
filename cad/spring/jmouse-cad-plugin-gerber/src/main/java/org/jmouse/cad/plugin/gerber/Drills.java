package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.gerber.excellon.compose.DrillHole;
import org.jmouse.gerber.excellon.compose.ExcellonComposer;

/** The pcb's holes ({@link PcbDrills}) as Excellon: the plated in one program, the unplated in another. */
final class Drills {

    /** The plated holes and the unplated ones: the pcb house plates one file and not the other. */
    record Programs(String plated, String unplated) {
    }

    private Drills() {
    }

    static Programs of(PcbFabrication fabrication) {
        ExcellonComposer plated = new ExcellonComposer(true);
        ExcellonComposer unplated = new ExcellonComposer(false);
        for (PcbDrills.Drill drill : PcbDrills.of(fabrication.pcb(), fabrication.footprints())) {
            DrillHole hole = drill.round() ? new DrillHole.Round(PcbGerber.out(drill.from().x(), drill.from().y()))
                    : new DrillHole.Slot(PcbGerber.out(drill.from().x(), drill.from().y()), PcbGerber.out(drill.to().x(), drill.to().y()));
            (drill.plated() ? plated : unplated).hole(drill.width(), hole);
        }
        return new Programs(plated.program().toSource(), unplated.program().toSource());
    }
}
