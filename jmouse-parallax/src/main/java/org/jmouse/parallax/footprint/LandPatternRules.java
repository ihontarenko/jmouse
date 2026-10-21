package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMap;
import org.jmouse.datamap.DataMapEntry;
import org.jmouse.datamap.DataMapValue;

/**
 * How a package's dimensions become copper, read from {@code footprint-standards/land-pattern-rules.jmd}: every
 * generated footprint follows these, so a change of taste is one edit there and a regeneration.
 *
 * @param license the licence every footprint of our library is published under
 */
public record LandPatternRules(String license, Fillets gullWing, Fillets chip, Fillets chipSmall,
                               Fillets chipHandSoldering, Fillets chipSmallHandSoldering, ThroughHole throughHole,
                               Drawing drawing) {

    /** The solder fillets a surface lead's pad leaves room for (IPC-7351), and the courtyard kept beyond. */
    public record Fillets(double toe, double heel, double side, double courtyard) {
    }

    /** A hole drilled for a lead and the ring of copper around it (IPC-2222). */
    public record ThroughHole(double drillAllowance, double drillStep, double ring, double gap, double courtyard) {

        /** The hole for a lead {@code width} by {@code thickness}: its diagonal and the allowance, up to the drill step. */
        public double drill(double width, double thickness) {
            double diagonal = Math.hypot(width, thickness);
            return Math.ceil((diagonal + drillAllowance) / drillStep - 1e-9) * drillStep;
        }

        /** The pad around a drill, narrowed where it would come nearer than the gap to its neighbour. */
        public double pad(double drill, double pitch) {
            return Math.min(drill + 2 * ring, pitch - gap);
        }
    }

    /**
     * The lines a footprint draws and the grid its copper snaps to.
     *
     * @param silkscreenClearance how far a silkscreen line keeps from copper
     * @param silkscreenOffset    how far a silkscreen line stands outside the body it outlines
     * @param pinNotchRadius      the notch a dual in-line outline marks its first end with
     */
    public record Drawing(double grid, double roundness, double fabricationWidth, double silkscreenWidth,
                          double silkscreenClearance, double silkscreenOffset, double courtyardWidth, double chamfer,
                          double pinMarkRadius, double pinNotchRadius) {

        /** A length to the nearest grid step, a half step rounded away from zero. */
        public double nearest(double length) {
            return Math.signum(length) * Math.round(Math.abs(length) / grid + 1e-9) * grid;
        }

        /** A length up to the next grid step. */
        public double up(double length) {
            return Math.ceil(length / grid - 1e-9) * grid;
        }
    }

    public static LandPatternRules of(DataMap map) {
        return new LandPatternRules(
                map.field("license").map(field -> field.value() instanceof DataMapValue.Text(String text) ? text : field.value().written())
                        .orElseThrow(() -> new IllegalArgumentException("The land pattern rules name no licence")),
                fillets(map, "gull-wing"), fillets(map, "chip"), fillets(map, "chip-small"),
                fillets(map, "chip-hand-soldering"), fillets(map, "chip-small-hand-soldering"),
                throughHole(new PackageTable(entry(map, "through-hole"))), drawing(new PackageTable(entry(map, "drawing"))));
    }

    private static Fillets fillets(DataMap map, String id) {
        PackageTable table = new PackageTable(entry(map, id));
        return new Fillets(table.length("toe"), table.length("heel"), table.length("side"), table.length("courtyard"));
    }

    private static ThroughHole throughHole(PackageTable table) {
        return new ThroughHole(table.length("drill-allowance"), table.length("drill-step"), table.length("ring"),
                table.length("gap"), table.length("courtyard"));
    }

    private static Drawing drawing(PackageTable table) {
        return new Drawing(table.length("grid"), table.number("roundness"), table.length("fabrication-width"),
                table.length("silkscreen-width"), table.length("silkscreen-clearance"), table.length("silkscreen-offset"),
                table.length("courtyard-width"), table.length("chamfer"), table.length("pin-mark-radius"),
                table.length("pin-notch-radius"));
    }

    private static DataMapEntry entry(DataMap map, String id) {
        return map.entry(id).orElseThrow(() -> new IllegalArgumentException("The land pattern rules have no '" + id + "'"));
    }
}
