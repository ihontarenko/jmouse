package org.jmouse.cad.plugin.ipc2581;

import org.jmouse.settings.Label;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExportPurpose;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.ArtworkFeature;
import org.jmouse.cad.exchange.pcb.BillOfMaterials;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PadStandardShape;
import org.jmouse.cad.exchange.pcb.PcbProfile;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.pcb.PcbLayers;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.ipc2581.Ipc2581Composer;
import org.jmouse.ipc2581.Ipc2581Graphic;
import org.jmouse.ipc2581.Ipc2581Point;
import org.jmouse.ipc2581.Ipc2581Polygon;
import org.jmouse.ipc2581.Ipc2581Shape;
import org.jmouse.strokefont.StrokeFont;

import java.time.Clock;
import java.util.function.Supplier;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * A pcb as one IPC-2581 revision C file: its layers from the top silkscreen to the bottom one with the dielectric
 * between the copper, its stackup as thick as its rules say, its edge as the profile, each footprint once as a package,
 * each part as a component on it, the nets as the circuit wires the pins, the bill of materials, and on each layer what
 * the Gerber set draws there — the same {@link PcbArtwork} — with every hole on the drill layer. A pad is flashed with a
 * standard shape where it is one turned a whole quarter, else as its outline. The file's frame is the drawing's with Y
 * turned upwards.
 */
public final class PcbIpc2581Export implements DocumentExport {

    /** The rule a pcb's thickness is in. */
    private static final String THICKNESS = "pcb-thickness";

    /**
     * What a layer on each face is called — the mask, the paste, the silkscreen — and the function the standard gives it.
     *
     * @param top      how the top one is called
     * @param bottom   how the bottom one is called
     * @param function the function IPC-2581 gives the group
     */
    public record FaceNames(@Label("Top") String top,
                            @Label("Bottom") String bottom,
                            @Label("Function") String function) {

        /** The one on a side: {@code top} or {@code bottom}. */
        String of(String side) {
            return PcbDrawing.TOP.equals(side) ? top : bottom;
        }
    }

    /**
     * What the copper layers are called: the two outer ones, and every one between by its number from the top.
     *
     * @param top      how the top one is called
     * @param bottom   how the bottom one is called
     * @param inner    how an inner layer is called, by its number from the top: {@code INNER_%d}
     * @param function the function IPC-2581 gives the group
     */
    public record CopperNames(@Label("Top") String top,
                              @Label("Bottom") String bottom,
                              @Label("Inner") String inner,
                              @Label("Function") String function) {

        /** The outer one on a side: {@code top} or {@code bottom}. */
        String of(String side) {
            return new FaceNames(top, bottom, function).of(side);
        }
    }

    /**
     * What the dielectric between two copper layers is called.
     *
     * @param between  how it is called, by its number from the top: {@code DIELECTRIC_%d}
     * @param function the function IPC-2581 gives it
     */
    public record DielectricNames(@Label("Between") String between,
                                  @Label("Function") String function) {
    }

    /**
     * The export's data ({@code settings/plugin/cad-ipc2581/export}).
     *
     * @param id            its word on the exchange routes
     * @param title         what the export is called where formats are offered
     * @param sender        who the file is from and what wrote it
     * @param copper        how the copper layers are called
     * @param mask          how the solder mask layers are called
     * @param paste         how the paste layers are called
     * @param silkscreen    how the silkscreen layers are called
     * @param dielectric    what the dielectric between two copper layers is called, by its number from the top
     * @param drill         what the layer of the through holes is called
     * @param packageMargin how far a package's outline stands off its pads
     */
    public record Settings(@Label("ID") String id,
                           @Label("Title") String title,
                           @Label("Sender") Ipc2581Composer.Sender sender,
                           @Label("Copper") CopperNames copper,
                           @Label("Mask") FaceNames mask,
                           @Label("Paste") FaceNames paste,
                           @Label("Silkscreen") FaceNames silkscreen,
                           @Label("Dielectric") DielectricNames dielectric,
                           @Label("Drill") String drill,
                           @Label(value = "Package margin", unit = "mm") double packageMargin) {
    }

    private final Supplier<Settings>              settings;
    private final Supplier<PcbArtwork.Settings> artwork;
    private final Supplier<PcbLayers.Settings>  layers;
    private final StrokeFont            font;
    private final Clock                 clock;

    /**
     * @param artwork what the copper, the mask, the paste and the silkscreen are drawn with, as every format draws them
     * @param layers  how thick the copper and the mask are
     * @param clock   when the file is written, as its history says
     */
    /** The ways a pcb is made, read through the engine's tables. */
    private final CadStandards standards;

    public PcbIpc2581Export(Supplier<Settings> settings, Supplier<PcbArtwork.Settings> artwork, Supplier<PcbLayers.Settings> layers, StrokeFont font,
                              Clock clock, CadStandards standards) {
        this.standards = standards;
        this.settings = settings;
        this.artwork = artwork;
        this.layers = layers;
        this.font = font;
        this.clock = clock;
    }

    @Override
    public String id() {
        return settings.get().id();
    }

    @Override
    public String title() {
        return settings.get().title();
    }

    /** A factory makes the pcb from this file: written only from a pcb its checks pass. */
    @Override
    public ExportPurpose purpose() {
        return ExportPurpose.FABRICATION;
    }

    @Override
    public Set<DocumentKind> kinds() {
        return Set.of(PcbModule.PCB);
    }

    @Override
    public ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue) {
        PcbFabrication fabrication = PcbFabrication.of(document, catalogue);
        List<String> notes = new ArrayList<>(fabrication.notes());
        Map<String, Double> rules = standards.pcbRules().valuesOf(fabrication.pcb().rules());
        PcbArtwork drawn = PcbArtwork.of(fabrication, rules, artwork.get(), font);
        Ipc2581Composer composer = new Ipc2581Composer(fabrication.name(), fabrication.name(), settings.get().sender(),
                OffsetDateTime.now(clock));
        List<String> copper = fabrication.pcb().copperLayerIds();
        declareLayers(composer, copper, rules.get(THICKNESS));
        profile(composer, drawn, notes);
        parts(composer, fabrication, drawn.outlines());
        for (String layer : copper) {
            artworkOf(composer, copperName(layer, copper), drawn.copper(layer), drawn.outlines(), fabrication);
        }
        for (String side : List.of(PcbDrawing.TOP, PcbDrawing.BOTTOM)) {
            artworkOf(composer, settings.get().mask().of(side), drawn.maskOpenings(side), drawn.outlines(), null);
            artworkOf(composer, settings.get().paste().of(side), drawn.paste(side), drawn.outlines(), null);
            artworkOf(composer, settings.get().silkscreen().of(side), drawn.silkscreen(side), drawn.outlines(), null);
        }
        holes(composer, fabrication);
        return ExportedFiles.single(fabrication.name() + ".xml", composer.write(), notes);
    }

    /**
     * The layers from the top: the silkscreen, the paste and the mask of the top, each copper layer with the dielectric
     * under it but the last, the bottom's mask, paste and silkscreen, then the drill layer; and the stackup they make, the
     * dielectric as thick as the pcb less its copper and its mask.
     */
    private void declareLayers(Ipc2581Composer composer, List<String> copper, double thickness) {
        Ipc2581Composer.Side top = Ipc2581Composer.Side.TOP;
        Ipc2581Composer.Side bottom = Ipc2581Composer.Side.BOTTOM;
        composer.layer(new Ipc2581Composer.Layer(settings.get().silkscreen().top(), settings.get().silkscreen().function(), top))
                .layer(new Ipc2581Composer.Layer(settings.get().paste().top(), settings.get().paste().function(), top))
                .layer(new Ipc2581Composer.Layer(settings.get().mask().top(), settings.get().mask().function(), top));
        List<Ipc2581Composer.StackupLayer> stacked = new ArrayList<>();
        stacked.add(new Ipc2581Composer.StackupLayer(settings.get().mask().top(), layers.get().mask().thickness()));
        double copperThickness = layers.get().copper().thickness();
        double dielectric = (thickness - copper.size() * copperThickness) / Math.max(1, copper.size() - 1);
        for (int index = 0; index < copper.size(); index++) {
            String name = copperName(copper.get(index), copper);
            Ipc2581Composer.Side side = index == 0 ? top : index == copper.size() - 1 ? bottom : Ipc2581Composer.Side.INTERNAL;
            composer.layer(new Ipc2581Composer.Layer(name, settings.get().copper().function(), side));
            stacked.add(new Ipc2581Composer.StackupLayer(name, copperThickness));
            if (index < copper.size() - 1) {
                String between = settings.get().dielectric().between().formatted(index + 1);
                composer.layer(new Ipc2581Composer.Layer(between, settings.get().dielectric().function(), Ipc2581Composer.Side.INTERNAL));
                stacked.add(new Ipc2581Composer.StackupLayer(between, dielectric));
            }
        }
        stacked.add(new Ipc2581Composer.StackupLayer(settings.get().mask().bottom(), layers.get().mask().thickness()));
        composer.layer(new Ipc2581Composer.Layer(settings.get().mask().bottom(), settings.get().mask().function(), bottom))
                .layer(new Ipc2581Composer.Layer(settings.get().paste().bottom(), settings.get().paste().function(), bottom))
                .layer(new Ipc2581Composer.Layer(settings.get().silkscreen().bottom(), settings.get().silkscreen().function(), bottom))
                .drillLayer(settings.get().drill(), copperName(copper.getFirst(), copper), copperName(copper.getLast(), copper))
                .stackup(thickness + 2 * layers.get().mask().thickness(), stacked);
    }

    private String copperName(String layer, List<String> copper) {
        if (PcbDrawing.TOP.equals(layer) || PcbDrawing.BOTTOM.equals(layer)) {
            return settings.get().copper().of(layer);
        }
        return settings.get().copper().inner().formatted(copper.indexOf(layer));
    }

    /** The pcb's edge: the widest of its areas, with its cut-outs; any other area is said and left out. */
    private static void profile(Ipc2581Composer composer, PcbArtwork drawn, List<String> notes) {
        PcbProfile profile = PcbProfile.of(drawn);
        notes.addAll(profile.notes());
        composer.profile(new Ipc2581Polygon(points(profile.outline()), profile.cutouts().stream().map(PcbIpc2581Export::points).toList()));
    }

    /**
     * Each footprint once as a package, each part as a component on it — its part number its line's in the bill of
     * materials —, each part's pins on their nets, and the bill of materials itself.
     */
    private void parts(Ipc2581Composer composer, PcbFabrication fabrication, PadOutline outlines) {
        Map<String, List<Ipc2581Composer.PinReference>> nets = new TreeMap<>();
        List<BillOfMaterials.Line> listed = new ArrayList<>();
        for (BillOfMaterials.Line line : BillOfMaterials.of(fabrication).lines()) {
            if (line.footprint().isEmpty() || line.parts().getFirst().drawing().isEmpty()) {
                continue;
            }
            listed.add(line);
            String packageName = line.footprint().get();
            composer.packageOf(packageOf(packageName, line.parts().getFirst().drawing().get(), outlines));
            for (PcbFabrication.SeatedPart part : line.parts()) {
                PcbDrawing.Placement placement = part.placement();
                composer.component(new Ipc2581Composer.Component(part.printed(), packageName, partNumberOf(line),
                        settings.get().copper().of(placement.side()), part.throughHole(), turn(-placement.rotation()), placement.onBottom(),
                        upward(placement.x(), placement.y())));
                part.padNets().forEach((pad, net) -> nets.computeIfAbsent(net, ignored -> new ArrayList<>())
                        .add(new Ipc2581Composer.PinReference(part.printed(), pad)));
            }
        }
        for (BillOfMaterials.Line line : listed) {
            // In the order they are said, every time the pcb is written.
            Map<String, String> characteristics = new LinkedHashMap<>();
            characteristics.put("value", line.value());
            characteristics.put("footprint", line.footprint().get());
            composer.bomItem(new Ipc2581Composer.BomItem(partNumberOf(line), line.value(), line.padCount(), line.designators(),
                    characteristics));
        }
        nets.forEach((net, pins) -> pins.forEach(pin -> composer.netPin(net, pin)));
    }

    /** A line's part number: its component, value and footprint, so two values of one component are two lines. */
    private static String partNumberOf(BillOfMaterials.Line line) {
        return String.join("_", line.component(), line.value().replace(' ', '_'), line.footprint().orElse(""));
    }

    /** A footprint as a package: its pads as pins in its own frame, its outline the box round them, a margin off. */
    private Ipc2581Composer.Package packageOf(String name, FootprintDrawing footprint, PadOutline outlines) {
        PcbDrawing.Placement origin = new PcbDrawing.Placement(name, 0, 0, 0, PcbDrawing.TOP, Optional.empty());
        List<Ipc2581Composer.Pin> pins = new ArrayList<>();
        double left = Double.MAX_VALUE;
        double top = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        double bottom = -Double.MAX_VALUE;
        for (FootprintDrawing.Pad pad : footprint.pads()) {
            pins.add(new Ipc2581Composer.Pin(pad.number(), !pad.isSurface(), upward(pad.x(), pad.y()),
                    shapeOf(origin, pad, 0, outlines)));
            for (PcbDrawing.Point corner : outlines.placed(origin, pad, 0)) {
                left = Math.min(left, corner.x());
                right = Math.max(right, corner.x());
                top = Math.min(top, corner.y());
                bottom = Math.max(bottom, corner.y());
            }
        }
        double margin = settings.get().packageMargin();
        if (pins.isEmpty()) {
            left = top = -margin;
            right = bottom = margin;
        }
        List<Ipc2581Point> box = List.of(upward(left - margin, bottom + margin), upward(right + margin, bottom + margin),
                upward(right + margin, top - margin), upward(left - margin, top - margin));
        String pinOne = footprint.pads().isEmpty() ? "1" : footprint.pads().getFirst().number();
        return new Ipc2581Composer.Package(name, pinOne, Ipc2581Polygon.of(box), pins);
    }

    /**
     * What the artwork draws on a layer: pads flashed — a copper pad on its net as the pin it is —, discs flashed, strokes,
     * arcs and areas drawn together, each in the set of the net it carries — a track, a via, a zone on its own net.
     *
     * @param fabrication the parts whose pads' nets a copper layer names; null for a layer that carries no net
     */
    private void artworkOf(Ipc2581Composer composer, String layer, List<ArtworkFeature> features, PadOutline outlines,
                           PcbFabrication fabrication) {
        Map<String, List<Ipc2581Graphic>> graphics = new LinkedHashMap<>();
        for (ArtworkFeature feature : features) {
            switch (feature) {
                case ArtworkFeature.Pad pad -> {
                    PcbDrawing.Point centre = pad.placement().placed(pad.pad().x(), pad.pad().y());
                    Ipc2581Shape shape = shapeOf(pad.placement(), pad.pad(), pad.growth(), outlines);
                    if (fabrication == null) {
                        composer.flash(layer, null, shape, upward(centre.x(), centre.y()));
                    } else {
                        composer.pad(layer, pad.net(), new Ipc2581Composer.PlacedPad(shape, upward(centre.x(), centre.y()),
                                new Ipc2581Composer.PinReference(pad.placement().printed(), pad.pad().number())));
                    }
                }
                case ArtworkFeature.Disc disc -> composer.flash(layer, disc.net(), new Ipc2581Shape.Circle(disc.diameter()),
                        upward(disc.centre().x(), disc.centre().y()));
                case ArtworkFeature.Stroke stroke -> {
                    List<Ipc2581Point> points = stroke.points().stream().map(point -> upward(point.x(), point.y())).toList();
                    if (points.size() == 1) {
                        composer.flash(layer, stroke.net(), new Ipc2581Shape.Circle(stroke.width()), points.getFirst());
                    } else if (points.size() == 2) {
                        drawn(graphics, stroke.net()).add(new Ipc2581Graphic.Line(points.get(0), points.get(1), stroke.width()));
                    } else {
                        drawn(graphics, stroke.net()).add(new Ipc2581Graphic.Polyline(points, stroke.width()));
                    }
                }
                case ArtworkFeature.Arc arc -> drawn(graphics, arc.net()).add(arcOf(arc));
                case ArtworkFeature.Region region -> {
                    if (region.outline().size() >= 3) {
                        drawn(graphics, region.net()).add(new Ipc2581Graphic.Area(new Ipc2581Polygon(points(region.outline()),
                                region.holes().stream().map(PcbIpc2581Export::points).toList())));
                    }
                }
            }
        }
        graphics.forEach((net, drawnOnNet) -> composer.graphics(layer, net, drawnOnNet));
    }

    /** The graphics of a net drawn so far on a layer; those of no net under null. */
    private static List<Ipc2581Graphic> drawn(Map<String, List<Ipc2581Graphic>> graphics, String net) {
        return graphics.computeIfAbsent(net, ignored -> new ArrayList<>());
    }

    /** Every hole through the pcb on the drill layer: a round one by its diameter, a slot as an oval along its line. */
    private void holes(Ipc2581Composer composer, PcbFabrication fabrication) {
        int count = 0;
        for (PcbDrills.Drill drill : PcbDrills.of(fabrication.pcb(), fabrication.footprints())) {
            count += 1;
            Ipc2581Composer.Plating plating = drill.plated() ? Ipc2581Composer.Plating.PLATED : Ipc2581Composer.Plating.NONPLATED;
            Ipc2581Point from = upward(drill.from().x(), drill.from().y());
            Ipc2581Point to = upward(drill.to().x(), drill.to().y());
            if (drill.round()) {
                composer.hole(settings.get().drill(), null, "H" + count, drill.width(), plating, from);
                continue;
            }
            double length = Math.hypot(to.x() - from.x(), to.y() - from.y());
            double rotation = turn(Math.toDegrees(Math.atan2(to.y() - from.y(), to.x() - from.x())));
            composer.slot(settings.get().drill(), null, "S" + count, plating, new Ipc2581Point((from.x() + to.x()) / 2, (from.y() + to.y()) / 2),
                    length + drill.width(), drill.width(), rotation);
        }
    }

    /**
     * A pad as a standard shape where it is one turned a whole quarter — a circle, a rectangle, a rounded one, an oval —,
     * else as its outline round its centre.
     */
    private static Ipc2581Shape shapeOf(PcbDrawing.Placement placement, FootprintDrawing.Pad pad, double growth, PadOutline outlines) {
        Ipc2581Shape standard = PadStandardShape.of(placement, pad, growth).map(shape -> switch (shape) {
            case PadStandardShape.Circle circle -> (Ipc2581Shape) new Ipc2581Shape.Circle(circle.diameter());
            case PadStandardShape.Rectangle rectangle -> new Ipc2581Shape.RectCenter(rectangle.width(), rectangle.height());
            case PadStandardShape.RoundedRectangle rounded -> new Ipc2581Shape.RectRound(rounded.width(), rounded.height(), rounded.radius());
            case PadStandardShape.Oval oval -> new Ipc2581Shape.Oval(oval.width(), oval.height());
        }).orElse(null);
        if (standard != null) {
            return standard;
        }
        PcbDrawing.Point centre = placement.placed(pad.x(), pad.y());
        List<Ipc2581Point> outline = outlines.placed(placement, pad, growth).stream()
                .map(point -> new Ipc2581Point(point.x() - centre.x(), centre.y() - point.y())).toList();
        return new Ipc2581Shape.Contour(Ipc2581Polygon.of(withoutRepeatedEnd(outline)));
    }

    /** An arc by its centre and its turn, seen with Y upwards; three points in a line, a line. */
    private static Ipc2581Graphic arcOf(ArtworkFeature.Arc arc) {
        Ipc2581Point from = upward(arc.from().x(), arc.from().y());
        Ipc2581Point to = upward(arc.to().x(), arc.to().y());
        return arc.circle().<Ipc2581Graphic>map(circle -> new Ipc2581Graphic.Arc(from, to,
                upward(circle.centre().x(), circle.centre().y()), circle.clockwise(), arc.width()))
                .orElseGet(() -> new Ipc2581Graphic.Line(from, to, arc.width()));
    }

    private static List<Ipc2581Point> points(List<PcbDrawing.Point> drawn) {
        return withoutRepeatedEnd(drawn.stream().map(point -> upward(point.x(), point.y())).toList());
    }

    /** A ring's points without its last when it repeats its first: the file closes a ring itself. */
    private static List<Ipc2581Point> withoutRepeatedEnd(List<Ipc2581Point> ring) {
        if (ring.size() > 1 && ring.getFirst().equals(ring.getLast())) {
            return ring.subList(0, ring.size() - 1);
        }
        return ring;
    }

    /** A point of the drawing, whose Y grows downwards, in the file's frame, whose Y grows upwards. */
    private static Ipc2581Point upward(double x, double y) {
        return new Ipc2581Point(x, y == 0 ? 0 : -y);
    }

    /** Degrees brought into 0 … 360. */
    private static double turn(double degrees) {
        return ((degrees % 360) + 360) % 360;
    }
}
