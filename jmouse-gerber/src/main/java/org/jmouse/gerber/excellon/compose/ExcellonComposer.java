package org.jmouse.gerber.excellon.compose;

import org.jmouse.gerber.GerberNumbers;
import org.jmouse.gerber.GerberPoint;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.excellon.node.ExcellonProgram;
import org.jmouse.gerber.excellon.node.ExcellonStatement;
import org.jmouse.gerber.excellon.node.ExcellonStatement.DrillCoordinates;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Composes an Excellon drill file (XNC, as KiCad and every board house read it): millimetres in decimal, a tool for
 * each diameter, then each tool's holes. Plated and unplated holes are separate files: the board house plates one and
 * not the other. It builds the {@link ExcellonProgram}; the text is its {@code toSource()}.
 */
public final class ExcellonComposer {

    private final boolean                          plated;
    private final Map<BigDecimal, List<DrillHole>> tools = new TreeMap<>();

    /** @param plated whether the holes are plated through ({@code PTH}) or not ({@code NPTH}) */
    public ExcellonComposer(boolean plated) {
        this.plated = plated;
    }

    /** A hole made with a tool of this diameter. */
    public ExcellonComposer hole(double diameter, DrillHole hole) {
        tools.computeIfAbsent(number(diameter), ignored -> new ArrayList<>()).add(hole);
        return this;
    }

    public boolean isEmpty() {
        return tools.isEmpty();
    }

    /** The whole file: the header with the tools, then each tool's holes, the end. */
    public ExcellonProgram program() {
        List<ExcellonStatement> statements = new ArrayList<>();
        statements.add(new ExcellonStatement.HeaderStart(GerberPosition.NONE));
        statements.add(new ExcellonStatement.Comment((plated ? "Plated" : "Non-plated") + " through holes", GerberPosition.NONE));
        statements.add(new ExcellonStatement.Comment("#@! TF.FileFunction," + (plated ? "Plated" : "NonPlated") + ",1,2,"
                + (plated ? "PTH" : "NPTH"), GerberPosition.NONE));
        statements.add(new ExcellonStatement.Format(2, GerberPosition.NONE));
        statements.add(new ExcellonStatement.Unit(true, Optional.empty(), GerberPosition.NONE));
        List<BigDecimal> diameters = List.copyOf(tools.keySet());
        for (int index = 0; index < diameters.size(); index++) {
            statements.add(new ExcellonStatement.ToolDefinition(index + 1, diameters.get(index), GerberPosition.NONE));
        }
        statements.add(new ExcellonStatement.HeaderEnd(GerberPosition.NONE));
        statements.add(new ExcellonStatement.Code(ExcellonStatement.Code.ABSOLUTE, GerberPosition.NONE));
        statements.add(new ExcellonStatement.Code(ExcellonStatement.Code.DRILL, GerberPosition.NONE));
        for (int index = 0; index < diameters.size(); index++) {
            statements.add(new ExcellonStatement.ToolSelect(index + 1, GerberPosition.NONE));
            for (DrillHole hole : tools.get(diameters.get(index))) {
                statements.add(switch (hole) {
                    case DrillHole.Round round -> new ExcellonStatement.Hole(coordinates(round.at()), GerberPosition.NONE);
                    case DrillHole.Slot slot -> new ExcellonStatement.Slot(coordinates(slot.from()), coordinates(slot.to()),
                            GerberPosition.NONE);
                });
            }
        }
        statements.add(new ExcellonStatement.ToolSelect(0, GerberPosition.NONE));
        statements.add(new ExcellonStatement.Code(ExcellonStatement.Code.END, GerberPosition.NONE));
        return ExcellonProgram.of(statements);
    }

    private static DrillCoordinates coordinates(GerberPoint point) {
        return DrillCoordinates.of(number(point.x()), number(point.y()));
    }

    private static BigDecimal number(double value) {
        return GerberNumbers.rounded(value, GerberNumbers.DRILL_DECIMALS);
    }
}
