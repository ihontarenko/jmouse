package org.jmouse.cad.spice;

import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceCommand;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.units.Quantity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ngspice's way: {@code .options temp tnom}, then a control block that runs the deck, puts its points on the even grid
 * ({@code linearize}) and prints every vector, wide and exact, in a section of its own ({@link SpicePrintout}).
 */
public final class NgspiceDialect implements SpiceDialect {

    /** The section the control block prints the run in. */
    private static final String SECTION = "tran";

    /** The column ngspice prints the time in. */
    private static final List<String> TIME = List.of("time");

    /** What a branch's current is printed as: its element's name, then this. */
    private static final String BRANCH = "#branch";

    @Override
    public String program() {
        return "ngspice";
    }

    @Override
    public List<SpiceStatement> runAndPrint(double step, double celsius) {
        return List.of(
                new SpiceCard.Directive("options", List.of(assignment("temp", celsius), assignment("tnom", celsius)),
                        SpicePosition.NONE),
                new SpiceCard.Control(List.of(
                        new SpiceCommand.Command("set", List.of(word("nobreak"))),
                        new SpiceCommand.Command("set", List.of(assignment("width", 100000))),
                        new SpiceCommand.Command("set", List.of(assignment("numdgt", 7))),
                        SpiceCommand.Command.of("run"),
                        SpiceCommand.Command.of("linearize"),
                        SpiceCommand.Command.of("echo", SpicePrintout.MARKER, SECTION),
                        SpiceCommand.Command.of("print", "all")), SpicePosition.NONE));
    }

    @Override
    public PrintedVectors read(String output) {
        SpicePrintout.Table table = SpicePrintout.of(output).table(SECTION);
        int axis = table.axisColumn(TIME);
        Map<String, double[]> nodes = new LinkedHashMap<>();
        Map<String, double[]> branches = new LinkedHashMap<>();
        for (int column = 0; column < table.names().size(); column++) {
            if (column == axis) {
                continue;
            }
            String printed = table.names().get(column).toLowerCase(Locale.ROOT);
            if (printed.endsWith(BRANCH)) {
                branches.putIfAbsent(printed.substring(0, printed.length() - BRANCH.length()), table.column(column));
            } else {
                nodes.putIfAbsent(printed, table.column(column));
            }
        }
        return new PrintedVectors(table.column(axis), nodes, branches);
    }

    private static SpiceArgument word(String text) {
        return new SpiceArgument.Word(text, SpicePosition.NONE);
    }

    private static SpiceArgument assignment(String name, double value) {
        return new SpiceArgument.Assignment(name, word(Quantity.plain(value).written()), SpicePosition.NONE);
    }
}
