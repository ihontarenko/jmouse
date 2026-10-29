package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.board.geometry.CopperNets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The bare board's netlist for its electrical test, as IPC-D-356 writes it and KiCad exports it: one fixed-column record
 * a pad, via or hole — which net it is on, where it stands, how big it is, whether it is drilled, from which side a probe
 * reaches it and whether the mask leaves it bare. A board house tests every board it makes against it: two points of one
 * net must be joined and two of different nets apart, so a short or an open etched into the copper is found before a part
 * is soldered. Units are KiCad's: a ten-thousandth of an inch, Y upwards.
 */
final class TestNetlist {

    /** A record's kind: a drilled feature, a surface one, a hole of no copper. */
    private static final int THROUGH    = 317;
    private static final int SURFACE    = 327;
    private static final int MECHANICAL = 367;

    /** The net of copper joined to nothing. */
    private static final String UNCONNECTED = "N/C";

    /** How long a net name a record's column holds; a longer one is named by an alias declared in the header. */
    private static final int NET_COLUMN = 14;

    /** A ten-thousandth of an inch, in millimetres. */
    private static final double UNIT = 0.00254;

    /**
     * @param midpoint a via: a point between parts, not a part's own pin
     * @param drill    millimetres; 0 for a feature with no hole
     * @param access   0 both sides, 1 the top, the copper layer count the bottom
     * @param mask     where the mask is open over it: 0 nowhere, 1 the top, 2 the bottom, 3 both
     */
    private record TestRecord(int kind, String net, String designator, String pin, boolean midpoint, double drill, int access,
                              double x, double y, double width, double height, double rotation, int mask) {
    }

    private TestNetlist() {
    }

    static String of(BoardFabrication fabrication, CopperNets nets) {
        BoardDrawing board = fabrication.board();
        int bottom = board.copperLayerIds().size();
        List<TestRecord> records = new ArrayList<>();
        for (int index = 0; index < nets.pads().size(); index++) {
            CopperNets.PlacedPad placed = nets.pads().get(index);
            FootprintDrawing.Pad pad = placed.pad();
            boolean through = !pad.isSurface();
            boolean top = placed.placement().side().equals(BoardDrawing.TOP);
            double drill = Math.min(pad.drillWidth(), pad.drillHeight() > 0 ? pad.drillHeight() : pad.drillWidth());
            records.add(new TestRecord(through ? THROUGH : SURFACE, nets.padNets().get(index).orElse(UNCONNECTED),
                    placed.designator(), pad.number(), false, through ? drill : 0, through ? 0 : top ? 1 : bottom,
                    placed.centre().x(), placed.centre().y(), pad.width(), pad.height(),
                    pad.rotation() + placed.placement().rotation(), through ? 3 : top ? 1 : 2));
        }
        // A footprint's own bare holes: a probe never lands in one, but the tester's fixture is drilled clear of it.
        for (BoardFabrication.SeatedPart part : fabrication.parts()) {
            for (FootprintDrawing.Pad pad : part.drawing().map(FootprintDrawing::pads).orElse(List.of())) {
                if (pad.isUnplated()) {
                    BoardDrawing.Point at = part.placement().placed(pad.x(), pad.y());
                    records.add(new TestRecord(MECHANICAL, UNCONNECTED, part.designator(), pad.number(), false, pad.drillWidth(),
                            0, at.x(), at.y(), pad.drillWidth(), pad.drillWidth(), 0, 0));
                }
            }
        }
        // Vias are tented: the mask covers them, so the tester reaches them through the copper alone.
        for (int index = 0; index < board.vias().size(); index++) {
            BoardDrawing.Via via = board.vias().get(index);
            records.add(new TestRecord(THROUGH, nets.viaNets().get(index).orElse(UNCONNECTED), "VIA", "", true, via.drill(), 0,
                    via.x(), via.y(), via.diameter(), via.diameter(), 0, 0));
        }
        for (int index = 0; index < board.holes().size(); index++) {
            BoardDrawing.Hole hole = board.holes().get(index);
            boolean plated = hole.ring() > 0;
            double size = plated ? hole.ring() : hole.drill();
            records.add(new TestRecord(plated ? THROUGH : MECHANICAL, UNCONNECTED, "HOLE", String.valueOf(index + 1), false,
                    hole.drill(), 0, hole.x(), hole.y(), size, size, 0, plated ? 3 : 0));
        }
        Map<String, String> aliases = new LinkedHashMap<>();
        records.stream().map(TestRecord::net).filter(net -> net.length() > NET_COLUMN).distinct()
                .forEach(net -> aliases.put(net, "NNAME" + (aliases.size() + 1)));
        List<String> lines = new ArrayList<>(List.of(
                "C  IPC-D-356 netlist, for the bare board's electrical test", "C", "C  Board: " + fabrication.name(), "C",
                "P  JOB   " + fabrication.name(), "P  CODE  00", "P  UNITS CUST 0", "P  DIM   N", "P  VER   IPC-D-356",
                "P  IMAGE PRIMARY"));
        aliases.forEach((net, alias) -> lines.add("P  " + padded(alias, 6) + " " + net));
        lines.add("C");
        records.forEach(record -> lines.add(line(record, aliases)));
        lines.add("999");
        lines.add("");
        return String.join("\n", lines);
    }

    private static String line(TestRecord record, Map<String, String> aliases) {
        String hole = record.drill() > 0 ? "D" + size(record.drill()) + (record.kind() == MECHANICAL ? "U" : "P") : "      ";
        return record.kind()
                + padded(aliases.getOrDefault(record.net(), record.net()), NET_COLUMN)
                + "   "
                + padded(record.designator(), 6)
                + (record.pin().isEmpty() ? " " : "-")
                + padded(record.pin(), 4)
                + (record.midpoint() ? "M" : " ")
                + hole
                + "A" + String.format("%02d", record.access())
                + "X" + signed(record.x()) + "Y" + signed(-record.y())
                + "X" + size(record.width()) + "Y" + size(record.height())
                + "R" + String.format("%03d", Math.round(((record.rotation() % 360) + 360) % 360))
                + " S" + record.mask();
    }

    /** A text in a column of this width: cut, or filled with spaces. */
    private static String padded(String text, int width) {
        return text.length() >= width ? text.substring(0, width) : text + " ".repeat(width - text.length());
    }

    /** A size column holds four digits. */
    private static String size(double millimetres) {
        return String.format("%04d", Math.min(9999, Math.round(millimetres / UNIT)));
    }

    private static String signed(double millimetres) {
        long value = Math.round(millimetres / UNIT);
        return (value < 0 ? "-" : "+") + String.format("%06d", Math.abs(value));
    }
}
