package org.jmouse.cad.pcb.layout;

import org.jmouse.settings.Bounds;
import org.jmouse.settings.Label;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperShape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 🧭 A circuit's pcb laid out from the circuit as it is drawn: each part starts where it stands on the sheet — what the sheet
 * keeps together the pcb keeps together —, scaled so the parts' own area fills a share of the pcb, then moved apart until no
 * two courtyards come nearer than the gap; turned as the sheet turns it. The nets the bench drives come out on a pin header
 * beside the sheet's left edge, the edge is drawn round everything, and the bottom is poured with the ground. No copper is
 * laid: that is the router's. Its numbers are data, the {@code circuit} of {@link PcbLayout}.
 */
public final class CircuitLayout {

    /** How a curve of a footprint's outline is followed when its extent is measured. */
    private static final int CURVE_STEPS = 16;

    /** The places a laid point is written to. */
    private static final double WRITTEN = 1000;

    /**
     * How a circuit's parts are first laid out on its pcb.
     *
     * @param partsShare     the share of the pcb the parts' own area fills
     * @param gap            how far apart two courtyards stand at least
     * @param grid           the grid every part stands on
     * @param margin         how far the edge stands off every courtyard
     * @param passes         how many passes push the courtyards apart at most
     * @param planeInset     how far inside the edge the ground is poured
     * @param planeClearance the gap the ground keeps from other nets
     * @param corner         the radius of the edge's corners
     * @param connector      the header the bench's nets come out on
     */
    public record Settings(@Bounds(minimum = 0, maximum = 1, step = 0.01) @Label("Share the parts fill") double partsShare,
                           @Label(value = "Gap", unit = "mm") double gap,
                           @Label(value = "Grid", unit = "mm") double grid,
                           @Label(value = "Margin", unit = "mm") double margin,
                           @Label("Passes") int passes,
                           @Label(value = "Plane inset", unit = "mm") double planeInset,
                           @Label(value = "Plane clearance", unit = "mm") double planeClearance,
                           @Label(value = "Corner", unit = "mm") double corner,
                           @Label("Connector") Connector connector) {
    }

    /**
     * The pin header the nets the bench drives come out on, beside the sheet's left edge.
     *
     * @param footprint   the header's footprint, its number of pins written as {@code {pins}}
     * @param designator  the letters its designator starts with
     * @param beforeSheet how far left of the sheet's leftmost part it stands, in the sheet's units
     */
    public record Connector(@Label("Footprint") String footprint,
                            @Label("Designator") String designator,
                            @Label("Distance before the sheet") double beforeSheet) {

        public String footprintFor(int pins) {
            return footprint.replace("{pins}", Integer.toString(pins));
        }
    }

    /** Where a part stands on its sheet, in the sheet's units, and how it is turned there, in degrees. */
    public record SheetPlace(double x, double y, double rotation) {
    }

    /** A part the pcb seats: its designator, the footprint it stands on — none read —, and each pad's net. */
    public record BoardPart(String designator, Optional<String> footprint, Map<String, String> nets) {
    }

    /**
     * What a pcb is laid out from.
     *
     * @param circuit    the circuit's slug: the pcb lays it out
     * @param parts      the parts that stand on a pcb — the bench's sources and instruments are none of them
     * @param footprints every footprint the parts and the bench header stand on, by slug
     * @param places     where each part stands on the sheet, by designator; a part of a sheet within is at the sheet's corner
     * @param benchNets  the nets the bench drives or reads that reach a part on the pcb: what the header brings out, ground first
     * @param ground     the net poured on the bottom; empty for a circuit without one
     */
    public record Request(String circuit, List<BoardPart> parts, Map<String, FootprintDrawing> footprints, Map<String, SheetPlace> places,
                          List<String> benchNets, Optional<String> ground) {
    }

    private record Box(double left, double top, double right, double bottom) {
    }

    private final Settings           settings;
    private final PcbLayout.Unseated unseated;

    /** A layout by the settings of a pcb's layout: its circuit's, and what a part with no footprint takes. */
    public CircuitLayout(PcbLayout layout) {
        this.settings = layout.circuit();
        this.unseated = layout.unseated();
    }

    /** The pcb laid out: its placements, edge and ground, no copper. */
    public PcbDrawing laidOut(Request request) {
        List<PcbDrawing.Placement> placements = new ArrayList<>();
        Map<String, Optional<FootprintDrawing>> footprints = new LinkedHashMap<>();
        Box sheet = spanOf(request.places().values().stream().map(place -> new double[] {place.x(), place.y()}).toList());
        for (BoardPart part : request.parts()) {
            SheetPlace place = request.places().get(part.designator());
            double rotation = place == null ? 0 : ((360 - place.rotation()) % 360 + 360) % 360;
            placements.add(new PcbDrawing.Placement(part.designator(), place == null ? sheet.left() : place.x(), place == null ? sheet.top() : place.y(),
                    rotation, PcbDrawing.TOP, Optional.empty()));
            footprints.put(part.designator(), part.footprint().map(request.footprints()::get));
        }
        if (!request.benchNets().isEmpty()) {
            String designator = freeDesignator(request.parts());
            String footprint = settings.connector().footprintFor(request.benchNets().size());
            Map<String, String> nets = new LinkedHashMap<>();
            for (int index = 0; index < request.benchNets().size(); index++) {
                nets.put(Integer.toString(index + 1), request.benchNets().get(index));
            }
            placements.add(new PcbDrawing.Placement(designator, sheet.left() - settings.connector().beforeSheet(), (sheet.top() + sheet.bottom()) / 2, 0,
                    PcbDrawing.TOP, Optional.empty(), Optional.empty(), Optional.of(footprint), nets));
            footprints.put(designator, Optional.ofNullable(request.footprints().get(footprint)));
        }
        // The sheet's longer span scaled to the side of a square whose area the parts fill the share the settings ask for: a sheet
        // drawn in one row is not blown up by its want of height.
        double partsArea = footprints.values().stream().mapToDouble(footprint -> {
            Box extent = extentOf(footprint);
            return (extent.right() - extent.left()) * (extent.bottom() - extent.top());
        }).sum();
        Box span = spanOf(placements.stream().map(placement -> new double[] {placement.x(), placement.y()}).toList());
        double scale = Math.sqrt(partsArea / settings.partsShare())
                       / Math.max(1, Math.max(span.right() - span.left(), span.bottom() - span.top()));
        List<PcbDrawing.Placement> laid = new ArrayList<>();
        for (PcbDrawing.Placement placement : placements) {
            laid.add(moved(placement, (placement.x() - span.left()) * scale, (placement.y() - span.top()) * scale));
        }
        // Apart by the gap and one step of the grid, then on the grid: a snap moves each part half a step at most, so two parts
        // come at most one step nearer and keep the gap.
        laid = snapped(apart(laid, footprints, settings.gap() + settings.grid()));
        // The edge the margin away from every courtyard, moved by whole steps of the grid so every place stays on it.
        List<Box> boxes = laid.stream().map(placement -> boundsOf(placement, footprints.get(placement.designator()))).toList();
        double left = boxes.stream().mapToDouble(Box::left).min().orElse(0);
        double top = boxes.stream().mapToDouble(Box::top).min().orElse(0);
        double right = boxes.stream().mapToDouble(Box::right).max().orElse(0);
        double bottom = boxes.stream().mapToDouble(Box::bottom).max().orElse(0);
        double shiftX = settings.margin() - Math.floor(left / settings.grid()) * settings.grid();
        double shiftY = settings.margin() - Math.floor(top / settings.grid()) * settings.grid();
        List<PcbDrawing.Placement> placed = laid.stream().map(placement -> moved(placement, written(placement.x() + shiftX), written(placement.y() + shiftY)))
                .toList();
        double width = Math.ceil(right + shiftX + settings.margin());
        double height = Math.ceil(bottom + shiftY + settings.margin());
        double inset = settings.planeInset();
        List<PcbDrawing.Zone> zones = request.ground().map(net -> List.of(new PcbDrawing.Zone(PcbDrawing.BOTTOM, net, settings.planeClearance(), 0,
                "M" + number(inset) + " " + number(inset) + "H" + number(width - inset) + "V" + number(height - inset) + "H" + number(inset) + "Z")))
                .orElse(List.of());
        return new PcbDrawing(request.circuit(), Optional.empty(), List.of(), null, List.of(), List.of(), 2, rectangularOutline(width, height, settings.corner()),
                placed, List.of(), List.of(), List.of(), zones, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** A placement at another place, all else kept. */
    private static PcbDrawing.Placement moved(PcbDrawing.Placement placement, double x, double y) {
        return new PcbDrawing.Placement(placement.designator(), x, y, placement.rotation(), placement.side(), placement.reference(), placement.shown(),
                placement.footprint(), placement.nets(), placement.arrangement(), placement.role());
    }

    /** The box a footprint takes in its own frame: its pads and outlines; an unseated part's box and courtyard. */
    private Box extentOf(Optional<FootprintDrawing> footprint) {
        if (footprint.isEmpty()) {
            double halfWidth = unseated.width() / 2 + unseated.courtyard();
            double halfHeight = unseated.height() / 2 + unseated.courtyard();
            return new Box(-halfWidth, -halfHeight, halfWidth, halfHeight);
        }
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (FootprintDrawing.Pad pad : footprint.get().pads()) {
            double reach = Math.hypot(pad.width(), pad.height()) / 2;
            left = Math.min(left, pad.x() - reach);
            top = Math.min(top, pad.y() - reach);
            right = Math.max(right, pad.x() + reach);
            bottom = Math.max(bottom, pad.y() + reach);
        }
        for (FootprintDrawing.Outline outline : footprint.get().outlines()) {
            Optional<CopperShape> drawn = CopperShape.ofPath(outline.path(), false, 0, CURVE_STEPS);
            if (drawn.isPresent()) {
                CopperShape.Box box = drawn.get().box();
                left = Math.min(left, box.left());
                top = Math.min(top, box.top());
                right = Math.max(right, box.right());
                bottom = Math.max(bottom, box.bottom());
            }
        }
        return Double.isFinite(left) ? new Box(left, top, right, bottom) : new Box(0, 0, 0, 0);
    }

    /** The box a placed part takes on the pcb: its footprint's turned and set where it stands. */
    private Box boundsOf(PcbDrawing.Placement placement, Optional<FootprintDrawing> footprint) {
        Box extent = extentOf(footprint);
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (double[] corner : new double[][] {{extent.left(), extent.top()}, {extent.right(), extent.top()}, {extent.left(), extent.bottom()},
                                               {extent.right(), extent.bottom()}}) {
            PcbDrawing.Point placed = placement.placed(corner[0], corner[1]);
            left = Math.min(left, placed.x());
            top = Math.min(top, placed.y());
            right = Math.max(right, placed.x());
            bottom = Math.max(bottom, placed.y());
        }
        return new Box(left, top, right, bottom);
    }

    /** Courtyards pushed apart: each pair nearer than the gap along the way it overlaps least, half each way. */
    private List<PcbDrawing.Placement> apart(List<PcbDrawing.Placement> placements, Map<String, Optional<FootprintDrawing>> footprints, double gap) {
        List<PcbDrawing.Placement> moved = new ArrayList<>(placements);
        for (int pass = 0; pass < settings.passes(); pass++) {
            boolean pushed = false;
            for (int first = 0; first < moved.size(); first++) {
                for (int second = first + 1; second < moved.size(); second++) {
                    PcbDrawing.Placement one = moved.get(first);
                    PcbDrawing.Placement two = moved.get(second);
                    Box a = boundsOf(one, footprints.get(one.designator()));
                    Box b = boundsOf(two, footprints.get(two.designator()));
                    double acrossX = Math.min(a.right(), b.right()) - Math.max(a.left(), b.left()) + gap;
                    double acrossY = Math.min(a.bottom(), b.bottom()) - Math.max(a.top(), b.top()) + gap;
                    if (acrossX <= 0 || acrossY <= 0) {
                        continue;
                    }
                    pushed = true;
                    if (acrossX < acrossY) {
                        double way = Math.signum(two.x() - one.x()) == 0 ? 1 : Math.signum(two.x() - one.x());
                        moved.set(first, moved(one, one.x() - way * acrossX / 2, one.y()));
                        moved.set(second, moved(two, two.x() + way * acrossX / 2, two.y()));
                    } else {
                        double way = Math.signum(two.y() - one.y()) == 0 ? 1 : Math.signum(two.y() - one.y());
                        moved.set(first, moved(one, one.x(), one.y() - way * acrossY / 2));
                        moved.set(second, moved(two, two.x(), two.y() + way * acrossY / 2));
                    }
                }
            }
            if (!pushed) {
                break;
            }
        }
        return moved;
    }

    private List<PcbDrawing.Placement> snapped(List<PcbDrawing.Placement> placements) {
        double grid = settings.grid();
        return placements.stream().map(placement -> moved(placement, Math.round(placement.x() / grid) * grid, Math.round(placement.y() / grid) * grid))
                .toList();
    }

    private static Box spanOf(List<double[]> points) {
        if (points.isEmpty()) {
            return new Box(0, 0, 1, 1);
        }
        return new Box(points.stream().mapToDouble(point -> point[0]).min().orElse(0), points.stream().mapToDouble(point -> point[1]).min().orElse(0),
                points.stream().mapToDouble(point -> point[0]).max().orElse(1), points.stream().mapToDouble(point -> point[1]).max().orElse(1));
    }

    /** The first designator of the header's letters no part takes: {@code J1}, else {@code J2}… */
    private String freeDesignator(List<BoardPart> parts) {
        Set<String> taken = new HashSet<>();
        parts.forEach(part -> taken.add(part.designator()));
        int number = 1;
        while (taken.contains(settings.connector().designator() + number)) {
            number += 1;
        }
        return settings.connector().designator() + number;
    }

    private static double written(double value) {
        return Math.round(value * WRITTEN) / WRITTEN;
    }

    private static String number(double value) {
        String written = String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        return written.equals("-0") ? "0" : written;
    }

    /** A rectangle's outline from the origin, its corners rounded by a radius — none for sharp ones. */
    public static String rectangularOutline(double width, double height, double corner) {
        if (corner <= 0) {
            return "M0 0H" + number(width) + "V" + number(height) + "H0Z";
        }
        double radius = Math.min(corner, Math.min(width / 2, height / 2));
        String rounded = number(radius);
        return "M" + rounded + " 0H" + number(width - radius) + "A" + rounded + " " + rounded + " 0 0 1 " + number(width) + " " + rounded + "V" + number(height - radius)
               + "A" + rounded + " " + rounded + " 0 0 1 " + number(width - radius) + " " + number(height) + "H" + rounded + "A" + rounded + " " + rounded + " 0 0 1 0 "
               + number(height - radius) + "V" + rounded + "A" + rounded + " " + rounded + " 0 0 1 " + rounded + " 0Z";
    }
}
