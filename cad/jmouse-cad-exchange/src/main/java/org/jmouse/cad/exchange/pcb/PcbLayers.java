package org.jmouse.cad.exchange.pcb;

import org.jmouse.settings.Bounds;
import org.jmouse.settings.Colour;
import org.jmouse.settings.Label;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A pcb as solids, for every format that models one in 3D — STEP, GLB —: the pcb's area with its holes, the outer
 * copper, the solder mask and the silkscreen of each side as thin layers over its faces, and a part's body raised from
 * its face — each an area of the pcb's plane, in the drawing's frame, with the height it starts at and how thick it
 * is. The layers are drawn from the same {@link PcbArtwork} as the Gerber files: the copper clipped to the pcb, the
 * mask the pcb less its openings, the silkscreen only where the mask is, and every hole left open through all of them.
 */
public final class PcbLayers {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/solid/layers. */
    public static final SettingsSection<Settings> SECTION = new SettingsSection<>("solid/layers", "PCB layers in 3D", Settings.class);

    private static final GeometryFactory AREAS = new GeometryFactory();

    /**
     * How a solid looks, as glTF's metallic-roughness model says it; a format that knows colours alone takes the colour.
     *
     * @param colour    {@code #rrggbb}
     * @param metallic  0 a dielectric — epoxy, ink —, 1 a bare metal
     * @param roughness 0 a mirror, 1 matte
     */
    public record Finish(@Colour @Label("Colour") String colour,
                         @Bounds(minimum = 0, maximum = 1, step = 0.01) @Label("Metallic") double metallic,
                         @Bounds(minimum = 0, maximum = 1, step = 0.01) @Label("Roughness") double roughness) {
    }

    /**
     * A thin layer over a face of the pcb.
     *
     * @param name      what its solid is called after the pcb and the side: {@code copper}
     * @param thickness how thick it is
     * @param finish    how it looks
     */
    public record Layer(@Label("Name") String name,
                        @Label(value = "Thickness", unit = "mm") double thickness,
                        @Label("Finish") Finish finish) {
    }

    /**
     * The layers' data: the settings tree's {@code solid/layers}.
     *
     * @param bare       the pcb where no mask is modelled over it: the mask's look, as the pcb is seen
     * @param core       the pcb's own look, under a mask modelled as a layer of its own
     * @param copper     the outer copper, as a layer over the pcb's face
     * @param mask       the solder mask, as a layer over the copper
     * @param silkscreen the silkscreen, as a layer over the mask
     * @param body       a part shown as its footprint's body
     */
    public record Settings(@Label("Bare PCB") Finish bare,
                           @Label("PCB core") Finish core,
                           @Label("Copper") Layer copper,
                           @Label("Mask") Layer mask,
                           @Label("Silkscreen") Layer silkscreen,
                           @Label("Part body") Finish body) {
    }

    /**
     * An area raised into a solid.
     *
     * @param area   its polygons, in the drawing's frame: millimetres, Y growing downwards
     * @param bottom the height its underside is at, the pcb's bottom face at 0
     */
    public record RaisedArea(String name, Finish finish, double bottom, double thickness, Geometry area) {
    }

    private final Settings settings;

    public PcbLayers(Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    /**
     * The outer copper, the solder mask and the silkscreen of each side the stack has, as the pcb is made ({@link PcbSolid}),
     * upwards from the top face and downwards from the bottom one, each starting where the one under it ends; a layer with
     * nothing on a side is left out.
     */
    public List<RaisedArea> layersOf(PcbSolid solid, PcbArtwork drawn, double thickness) {
        List<String> stack = drawn.fabrication().pcb().copperLayerIds();
        String name = drawn.fabrication().name();
        List<RaisedArea> layers = new ArrayList<>();
        for (String side : List.of(PcbDrawing.TOP, PcbDrawing.BOTTOM)) {
            if (!stack.contains(side)) {
                continue;
            }
            PcbSolid.Side made = solid.side(side).orElseThrow();
            boolean top = side.equals(PcbDrawing.TOP);
            double face = top ? thickness : 0;
            double reached = 0;
            for (Map.Entry<Layer, Geometry> layer : List.of(Map.entry(settings.copper(), made.copper()), Map.entry(settings.mask(), made.mask()),
                    Map.entry(settings.silkscreen(), made.silkscreen()))) {
                double depth = layer.getKey().thickness();
                double bottom = top ? face + reached : face - reached - depth;
                reached += depth;
                if (layer.getValue().isEmpty()) {
                    continue;
                }
                layers.add(new RaisedArea(name + " " + side + " " + layer.getKey().name(), layer.getKey().finish(), bottom, depth,
                        layer.getValue()));
            }
        }
        return layers;
    }

    /** A part's body raised from the face its part is on: up from the top, down from the bottom; only a body that is raised ({@link PartBodies.PartBody#raised}). */
    public RaisedArea bodyOf(PartBodies.PartBody body, double thickness) {
        List<Coordinate> ring = new ArrayList<>(body.outline().stream().map(point -> new Coordinate(point.x(), point.y())).toList());
        if (!ring.getFirst().equals2D(ring.getLast())) {
            ring.add(ring.getFirst());
        }
        Polygon outline = AREAS.createPolygon(ring.toArray(Coordinate[]::new));
        boolean bottomSide = PcbDrawing.BOTTOM.equals(body.side());
        double height = body.height().orElseThrow(() -> new IllegalArgumentException(body.designator() + "'s footprint gives no body height"));
        return new RaisedArea(body.designator(), settings.body(), bottomSide ? -height : thickness, height,
                outline.isValid() ? outline : outline.buffer(0));
    }

}
