package org.jmouse.parallax.footprint;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * How a package's dimensions become copper, read from {@code footprint-standards/land-pattern-rules.sht}: every
 * generated footprint follows these, so a change of taste is one edit there and a regeneration.
 *
 * @param license the licence every footprint of our library is published under
 * @param fillets the surface lead rules by name — {@code gull-wing}, {@code chip}, {@code chip-small},
 *                {@code chip-hand-soldering}, {@code melf} …: every entry that writes a toe
 */
public record LandPatternRules(String license, Map<String, Fillets> fillets, Tolerances tolerances, ThroughHole throughHole,
                               Drawing drawing) {

    public LandPatternRules {
        fillets = Map.copyOf(fillets);
    }

    /** The solder fillets a surface lead's pad leaves room for (IPC-7351), and the courtyard kept beyond. */
    public record Fillets(double toe, double heel, double side, double courtyard) {
    }

    /** What the board's making and the part's placing add to a land: IPC-7351's F and P. */
    public record Tolerances(double fabrication, double placement) {
    }

    /** A hole drilled for a lead and the ring of copper around it (IPC-2222). */
    public record ThroughHole(double drillAllowance, double drillStep, double ring, double gap, double courtyard) {

        /** The hole for a lead {@code width} by {@code thickness} (0 for a round lead): its diagonal and the allowance, up to the drill step. */
        public double drill(double width, double thickness) {
            double diagonal = Math.hypot(width, thickness);
            return Math.ceil((diagonal + drillAllowance) / drillStep - 1e-9) * drillStep;
        }

        /** The pad a drill is ringed with, before a neighbour narrows it. */
        public double pad(double drill) {
            return drill + 2 * ring;
        }

        /** The pad around a drill, narrowed where it would come nearer than the gap to its neighbour. */
        public double pad(double drill, double pitch) {
            return Math.min(pad(drill), pitch - gap);
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

        /** How far a silkscreen line's middle stands from copper: the clearance and half the line. */
        public double clearOfCopper() {
            return silkscreenClearance + silkscreenWidth / 2;
        }
    }

    /** The fillets of that name; refused when the rules have none. */
    public Fillets fillets(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException("The land pattern rules have no fillets '" + name + "'"));
    }

    public Optional<Fillets> find(String name) {
        return Optional.ofNullable(fillets.get(name));
    }

    public static LandPatternRules of(Sheet map) {
        Map<String, Fillets> fillets = new LinkedHashMap<>();
        for (SheetEntry entry : map.entries()) {
            if (entry.value("toe").isPresent()) {
                PackageTable table = new PackageTable(entry);
                fillets.put(entry.id(), new Fillets(table.length("toe"), table.length("heel"), table.length("side"),
                        table.length("courtyard")));
            }
        }
        return new LandPatternRules(
                map.field("license").map(field -> field.value() instanceof SheetValue.Text(String text) ? text : field.value().written())
                        .orElseThrow(() -> new IllegalArgumentException("The land pattern rules name no licence")),
                fillets, tolerances(new PackageTable(entry(map, "tolerances"))),
                throughHole(new PackageTable(entry(map, "through-hole"))), drawing(new PackageTable(entry(map, "drawing"))));
    }

    private static Tolerances tolerances(PackageTable table) {
        return new Tolerances(table.length("fabrication"), table.length("placement"));
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

    private static SheetEntry entry(Sheet map, String id) {
        return map.entry(id).orElseThrow(() -> new IllegalArgumentException("The land pattern rules have no '" + id + "'"));
    }
}
