package org.jmouse.cad.plugin.odb;

import org.jmouse.settings.Label;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExportPurpose;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.ArtworkFeature;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.pcb.PcbProfile;
import org.jmouse.cad.exchange.pcb.PadStandardShape;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.odb.OdbComponents;
import org.jmouse.odb.OdbEda;
import org.jmouse.odb.OdbFeatures;
import org.jmouse.odb.OdbJob;
import org.jmouse.odb.OdbLayer;
import org.jmouse.odb.OdbSymbol;
import org.jmouse.strokefont.StrokeFont;

import java.time.Clock;
import java.util.function.Supplier;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A pcb as an ODB++ job: its matrix from the top silkscreen to the bottom one with each copper layer between, the drill
 * layer last; its edge as the step's profile; and on each layer what the Gerber set draws there — the same
 * {@link PcbArtwork} —, a pad flashed with a standard symbol where it is one turned a whole quarter, else filled as its
 * outline. The job's frame is the drawing's with Y turned upwards; it is handed over as one tgz.
 */
public final class PcbOdbExport implements DocumentExport {

    /**
     * What a layer on each face is called: the mask, the paste, the silkscreen.
     *
     * @param top    how the top one is called
     * @param bottom how the bottom one is called
     */
    public record FaceNames(@Label("Top") String top,
                            @Label("Bottom") String bottom) {

        /** The one on a side: {@code top} or {@code bottom}. */
        String of(String side) {
            return PcbDrawing.TOP.equals(side) ? top : bottom;
        }
    }

    /**
     * What the copper layers are called: the two outer ones, and every one between by its number from the top.
     *
     * @param top    how the top one is called
     * @param bottom how the bottom one is called
     * @param inner  how an inner layer is called, by its number from the top: {@code inner%d}
     */
    public record CopperNames(@Label("Top") String top,
                              @Label("Bottom") String bottom,
                              @Label("Inner") String inner) {

        /** The outer one on a side: {@code top} or {@code bottom}. */
        String of(String side) {
            return new FaceNames(top, bottom).of(side);
        }
    }

    /**
     * The export's data ({@code settings/plugin/cad-odb/export}).
     *
     * @param id         its word on the exchange routes
     * @param title      what the export is called where formats are offered
     * @param origin     who the job is from and what wrote it
     * @param step       the job's one step: {@code pcb}
     * @param copper     how the copper layers are called
     * @param mask       how the solder mask layers are called
     * @param paste      how the paste layers are called
     * @param silkscreen how the silkscreen layers are called
     * @param drill      what the layer of the through holes is called
     */
    public record Settings(@Label("ID") String id,
                           @Label("Title") String title,
                           @Label("Origin") OdbJob.Origin origin,
                           @Label("Step") String step,
                           @Label("Copper") CopperNames copper,
                           @Label("Mask") FaceNames mask,
                           @Label("Paste") FaceNames paste,
                           @Label("Silkscreen") FaceNames silkscreen,
                           @Label("Drill") String drill) {
    }

    private final Supplier<Settings>              settings;
    private final Supplier<PcbArtwork.Settings> artwork;
    private final StrokeFont            font;
    private final Clock                 clock;

    /**
     * @param artwork what the copper, the mask, the paste and the silkscreen are drawn with, as every format draws them
     * @param clock   when the job is written, as its history says
     */
    /** The ways a pcb is made, read through the engine's tables. */
    private final CadStandards standards;

    public PcbOdbExport(Supplier<Settings> settings, Supplier<PcbArtwork.Settings> artwork, StrokeFont font, Clock clock, CadStandards standards) {
        this.standards = standards;
        this.settings = settings;
        this.artwork = artwork;
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

    /** A factory makes the pcb from these files: written only from a pcb its checks pass. */
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
        OdbJob job = new OdbJob(fabrication.name(), settings.get().step(), settings.get().origin(), OffsetDateTime.now(clock));
        PcbProfile profile = PcbProfile.of(drawn);
        notes.addAll(profile.notes());
        job.profile(new OdbFeatures().surface(contour(profile.outline()), profile.cutouts().stream().map(PcbOdbExport::contour).toList(), true));
        PadOutline outlines = drawn.outlines();
        String top = PcbDrawing.TOP;
        String bottom = PcbDrawing.BOTTOM;
        List<String> copper = fabrication.pcb().copperLayerIds();
        List<String> linked = new ArrayList<>(copper.stream().map(layer -> copperName(layer, copper)).toList());
        linked.add(settings.get().drill());
        OdbEda eda = new OdbEda(linked);
        PcbOdbConnectivity connectivity = new PcbOdbConnectivity(fabrication, eda, notes);
        job.layer(OdbLayer.of(OdbComponents.Side.TOP.layer(), OdbLayer.Type.COMPONENT), new OdbFeatures());
        job.layer(OdbLayer.of(settings.get().silkscreen().of(top), OdbLayer.Type.SILK_SCREEN), featuresOf(drawn.silkscreen(top), outlines))
                .layer(OdbLayer.of(settings.get().paste().of(top), OdbLayer.Type.SOLDER_PASTE), featuresOf(drawn.paste(top), outlines))
                .layer(OdbLayer.of(settings.get().mask().of(top), OdbLayer.Type.SOLDER_MASK), featuresOf(drawn.maskOpenings(top), outlines));
        for (String layer : copper) {
            String name = copperName(layer, copper);
            job.layer(OdbLayer.of(name, OdbLayer.Type.SIGNAL), featuresOf(drawn.copper(layer), outlines,
                    (feature, first, end) -> connectivity.copper(feature, name, first, end)));
        }
        job.layer(OdbLayer.of(settings.get().mask().of(bottom), OdbLayer.Type.SOLDER_MASK), featuresOf(drawn.maskOpenings(bottom), outlines))
                .layer(OdbLayer.of(settings.get().paste().of(bottom), OdbLayer.Type.SOLDER_PASTE), featuresOf(drawn.paste(bottom), outlines))
                .layer(OdbLayer.of(settings.get().silkscreen().of(bottom), OdbLayer.Type.SILK_SCREEN), featuresOf(drawn.silkscreen(bottom), outlines))
                .layer(OdbLayer.drill(settings.get().drill(), copperName(copper.getFirst(), copper), copperName(copper.getLast(), copper)),
                        holes(fabrication, connectivity))
                .layer(OdbLayer.of(OdbComponents.Side.BOTTOM.layer(), OdbLayer.Type.COMPONENT), new OdbFeatures());
        for (OdbComponents.Side side : OdbComponents.Side.values()) {
            connectivity.components(side).ifPresent(job::components);
        }
        job.eda(eda);
        return ExportedFiles.binary(fabrication.name() + "-odb.tgz", job.archive(), notes);
    }

    private String copperName(String layer, List<String> copper) {
        if (PcbDrawing.TOP.equals(layer) || PcbDrawing.BOTTOM.equals(layer)) {
            return settings.get().copper().of(layer);
        }
        return settings.get().copper().inner().formatted(copper.indexOf(layer));
    }

    /**
     * What the artwork draws on a layer, as features: a pad flashed with its standard symbol, else filled as its outline;
     * a disc flashed round; a stroke's runs as lines with a round symbol; an arc about its circle, or a line when straight;
     * an area as a surface with its holes.
     */
    private static OdbFeatures featuresOf(List<ArtworkFeature> drawn, PadOutline outlines) {
        return featuresOf(drawn, outlines, (feature, first, end) -> {
        });
    }

    /** What a drawn thing became: the numbers of the features, from {@code first} up to but not {@code end}. */
    private interface Drawn {
        void features(ArtworkFeature feature, int first, int end);
    }

    private static OdbFeatures featuresOf(List<ArtworkFeature> drawn, PadOutline outlines, Drawn told) {
        OdbFeatures features = new OdbFeatures();
        for (ArtworkFeature feature : drawn) {
            int first = features.count();
            switch (feature) {
                case ArtworkFeature.Pad pad -> {
                    PcbDrawing.Point centre = pad.placement().placed(pad.pad().x(), pad.pad().y());
                    PadStandardShape.of(pad.placement(), pad.pad(), pad.growth()).ifPresentOrElse(
                            shape -> features.pad(upward(centre), symbolOf(shape), 0, true),
                            () -> features.surface(contour(outlines.placed(pad.placement(), pad.pad(), pad.growth())), List.of(), true));
                }
                case ArtworkFeature.Disc disc -> features.pad(upward(disc.centre()), new OdbSymbol.Round(disc.diameter()), 0, true);
                case ArtworkFeature.Stroke stroke -> {
                    OdbSymbol pen = new OdbSymbol.Round(stroke.width());
                    List<PcbDrawing.Point> points = stroke.points();
                    if (points.size() == 1) {
                        features.pad(upward(points.getFirst()), pen, 0, true);
                    }
                    for (int index = 1; index < points.size(); index++) {
                        features.line(upward(points.get(index - 1)), upward(points.get(index)), pen, true);
                    }
                }
                case ArtworkFeature.Arc arc -> {
                    OdbSymbol pen = new OdbSymbol.Round(arc.width());
                    arc.circle().ifPresentOrElse(
                            circle -> features.arc(upward(arc.from()), upward(arc.to()), upward(circle.centre()), pen, circle.clockwise(), true),
                            () -> features.line(upward(arc.from()), upward(arc.to()), pen, true));
                }
                case ArtworkFeature.Region region -> {
                    if (region.outline().size() >= 3) {
                        features.surface(contour(region.outline()), region.holes().stream().map(PcbOdbExport::contour).toList(), true);
                    }
                }
            }
            told.features(feature, first, features.count());
        }
        return features;
    }

    /** Every hole through the pcb: a round one flashed as large as its drill, a slot a line along it as wide. */
    private OdbFeatures holes(PcbFabrication fabrication, PcbOdbConnectivity connectivity) {
        OdbFeatures features = new OdbFeatures();
        for (PcbDrills.Drill drill : PcbDrills.of(fabrication.pcb(), fabrication.footprints())) {
            OdbSymbol bit = new OdbSymbol.Round(drill.width());
            connectivity.hole(drill.from(), settings.get().drill(), features.count());
            if (drill.round()) {
                features.pad(upward(drill.from()), bit, 0, true);
            } else {
                features.line(upward(drill.from()), upward(drill.to()), bit, true);
            }
        }
        return features;
    }

    private static OdbSymbol symbolOf(PadStandardShape shape) {
        return switch (shape) {
            case PadStandardShape.Circle circle -> new OdbSymbol.Round(circle.diameter());
            case PadStandardShape.Rectangle rectangle -> new OdbSymbol.Rectangle(rectangle.width(), rectangle.height());
            case PadStandardShape.RoundedRectangle rounded -> new OdbSymbol.RoundedRectangle(rounded.width(), rounded.height(), rounded.radius());
            case PadStandardShape.Oval oval -> new OdbSymbol.Oval(oval.width(), oval.height());
        };
    }

    /** A ring of the drawing as a contour, its closing point not repeated. */
    private static OdbFeatures.Contour contour(List<PcbDrawing.Point> ring) {
        List<PcbDrawing.Point> open = ring.size() > 1 && ring.getFirst().equals(ring.getLast()) ? ring.subList(0, ring.size() - 1) : ring;
        return new OdbFeatures.Contour(open.stream().map(PcbOdbExport::upward).toList());
    }

    /** A point of the drawing, whose Y grows downwards, in the job's frame, whose Y grows upwards. */
    private static OdbFeatures.Point upward(PcbDrawing.Point point) {
        return new OdbFeatures.Point(point.x(), point.y() == 0 ? 0 : -point.y());
    }
}
