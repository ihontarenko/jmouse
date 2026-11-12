package org.jmouse.cad.plugin.step;

import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.ArtworkAreas;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.pcb.PcbLayers;
import org.jmouse.cad.exchange.pcb.PartBodies;
import org.jmouse.cad.exchange.pcb.PcbSolid;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.mesh.ModelFile;
import org.jmouse.cad.mesh.ModelMaterials;
import org.jmouse.cad.mesh.ModelShapes;
import org.jmouse.cad.mesh.ModelPlacement;
import org.jmouse.cad.plugin.mesh.ModelDocuments;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.core.process.PipedProgram;
import org.jmouse.core.process.PipedProgramException;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.mesh.Placement;
import org.jmouse.svg.path.node.PathContours;
import org.jmouse.svg.path.node.PathSegment;
import org.jmouse.svg.path.node.PathSegment.PathPoint;
import org.jmouse.strokefont.StrokeFont;
import org.jmouse.svg.path.parser.PathParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.json.JsonMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A pcb as a STEP assembly, for a mechanical CAD: the pcb a solid of its thickness, its edge kept exact — lines,
 * arcs, curves — and every hole drilled through it; each part an instance of its model's own STEP file where it stands,
 * the models exact and each written once; with its {@link Variant#layers() layers}, the outer copper, the solder mask and
 * the silkscreen over each face as thin solids, drawn from the same {@link PcbArtwork} as the Gerber files. A part
 * whose footprint shows no model is its footprint's body ({@link PartBodies}), as a 3D view shows it. OpenCascade builds
 * it, run as a program; nothing of it is in the server. A part with neither, or whose model has no STEP file, is left out
 * and said so.
 */
public final class PcbStepExport implements DocumentExport {

    /** STEP's word for a model's file and among the mesh formats. */
    private static final String FORMAT = "step";

    /** The rule a pcb's thickness is in. */
    private static final String THICKNESS = "pcb-thickness";

    /** What OpenCascade prints before a word for the person who exported: the rest of what it prints is its own. */
    private static final String NOTE = "note: ";

    /** How many of a failed build's last lines are said with the failure. */
    private static final int SAID_LINES = 4;

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final PathParser PATHS = new PathParser();

    /**
     * The plugin's data ({@code settings/plugin/step/export}).
     *
     * @param ellipseSteps how many straight steps follow an arc of an ellipse, which a pcb's edge has no exact form for
     * @param areaSteps    how many straight steps a quarter of a round end, a disc or an arc of the copper, the mask and the
     *                     silkscreen is drawn with: each is a face of the file, so fewer than Gerber's curves
     * @param areaTolerance how far a layer's outline may move when it is simplified, in millimetres
     * @param exports      the exports the plugin brings, each a format of its own
     */
    public record Settings(int ellipseSteps, int areaSteps, double areaTolerance, List<Variant> exports) {
    }

    /**
     * One export of the plugin.
     *
     * @param id     its word on the exchange routes: {@code step}
     * @param title  what the export is called where formats are offered
     * @param layers whether the outer copper, the solder mask and the silkscreen are written over the pcb's faces
     */
    public record Variant(String id, String title, boolean layers) {
    }

    /** What OpenCascade is handed: millimetres, Z up, the pcb's Y turned upwards, its bottom face at 0. */
    record Request(String name, double thickness, double[] colour, List<Contour> outline, List<Hole> drills,
                   List<StepModel> models, List<PlacedPart> parts, List<RaisedSolid> solids) {
    }

    /**
     * An area raised into a solid — a thin layer over a face, a part's body —: each polygon a list of rings — its outline,
     * then its holes —, each ring its points, not closed by repeating the first.
     */
    record RaisedSolid(String name, double[] colour, double bottom, double thickness, List<List<List<double[]>>> polygons) {
    }

    /** One closed run of the edge: each segment a {@code line}, an {@code arc} (from, through, to) or a {@code bezier}. */
    record Contour(List<Map<String, List<double[]>>> segments) {
    }

    record Hole(double width, double[] from, double[] to) {
    }

    /**
     * A model as OpenCascade takes it: its own STEP file, or the shapes it is drawn from.
     *
     * @param step   the model's STEP file, in base64; null for a model drawn from shapes
     * @param shapes the shapes it is drawn from, each made an exact solid; empty for a model of files
     */
    record StepModel(String name, String step, List<StepShape> shapes) {
    }

    /**
     * One shape of a drawn model, as OpenCascade makes it an exact solid — set by {@code placement}, a 3 × 4 matrix, row
     * by row, and of its material's {@code colour}. Written with its {@code kind}: the script makes each kind its own way.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({@JsonSubTypes.Type(value = StepBox.class, name = "box"), @JsonSubTypes.Type(value = StepCylinder.class, name = "cylinder"),
            @JsonSubTypes.Type(value = StepPrism.class, name = "prism"), @JsonSubTypes.Type(value = StepRevolved.class, name = "revolved")})
    sealed interface StepShape permits StepBox, StepCylinder, StepPrism, StepRevolved {
        double[] placement();

        double[] colour();
    }

    /** A box of {@code size}, centred on its own origin. */
    record StepBox(double[] size, double[] placement, double[] colour) implements StepShape {
    }

    /** A cylinder of {@code radius} and {@code length} along Z, centred on its own origin. */
    record StepCylinder(double radius, double length, double[] placement, double[] colour) implements StepShape {
    }

    /** An {@code outline} of [x, y] raised by {@code height} from its base. */
    record StepPrism(List<double[]> outline, double height, double[] placement, double[] colour) implements StepShape {
    }

    /**
     * A turned body: its {@code profile} of [radius, z] turned about Z {@code from} one angle {@code to} another, in degrees —
     * the region between the profile and the axis, a solid, as the model's mesh caps a whole turn at the axis.
     */
    record StepRevolved(List<double[]> profile, double from, double to, double[] placement, double[] colour) implements StepShape {
    }

    /** @param placement a 3 × 4 matrix, row by row */
    record PlacedPart(String name, int model, double[] placement) {
    }

    private final PipedProgram             program;
    private final Supplier<ModelDocuments> models;
    private final Settings                 settings;
    private final Variant                  variant;
    private final PcbArtwork.Settings    artwork;
    private final PcbLayers              layers;
    private final StrokeFont               font;
    private final MeshFormats              formats = MeshFormats.standard();

    /**
     * @param models  the model documents, asked for on each export: they stand on the catalogue, which stands on the plugins
     * @param artwork what the copper, the mask and the silkscreen are drawn with, as every format draws them
     * @param layers  how thick the layers over the pcb's faces are and how each looks, as every 3D format models them
     */
    public PcbStepExport(PipedProgram program, Supplier<ModelDocuments> models, Settings settings, Variant variant,
                           PcbArtwork.Settings artwork, PcbLayers.Settings layers, StrokeFont font) {
        this.program = program;
        this.models = models;
        this.settings = settings;
        this.variant = variant;
        this.artwork = artwork;
        this.layers = new PcbLayers(layers);
        this.font = font;
    }

    @Override
    public String id() {
        return variant.id();
    }

    @Override
    public boolean available() {
        return program.available();
    }

    @Override
    public String title() {
        return variant.title();
    }

    @Override
    public Set<DocumentKind> kinds() {
        return Set.of(PcbModule.PCB);
    }

    @Override
    public ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue) {
        PcbFabrication fabrication = PcbFabrication.of(document, catalogue);
        PcbDrawing pcb = fabrication.pcb();
        List<String> notes = new ArrayList<>(fabrication.notes());
        Map<String, Double> rules = CadStandards.pcbRules().valuesOf(pcb.rules());
        double thickness = rules.get(THICKNESS);
        List<Contour> outline = outlineOf(pcb.outline(), notes);
        if (outline.isEmpty()) {
            throw new IllegalArgumentException("The PCB has no closed edge, so there is no PCB to make a solid of.");
        }
        List<PcbDrills.Drill> drilled = PcbDrills.of(pcb, fabrication.footprints());
        List<Hole> drills = drilled.stream().map(drill -> new Hole(drill.width(), upward(drill.from()), upward(drill.to()))).toList();
        List<StepModel> stepModels = new ArrayList<>();
        Map<String, Optional<Integer>> modelIndexes = new LinkedHashMap<>();
        Map<String, ModelFile> modelFiles = new LinkedHashMap<>();
        List<PlacedPart> parts = new ArrayList<>();
        List<RaisedSolid> solids = new ArrayList<>();
        PartBodies bodies = new PartBodies(artwork.curveSteps());
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            List<FootprintDrawing.Model> shown = part.drawing().map(ModelPlacement::shownOf).orElse(List.of());
            if (shown.isEmpty()) {
                bodies.of(part).filter(PartBodies.PartBody::raised).ifPresentOrElse(body -> solids.add(solidOf(layers.bodyOf(body, thickness), false)),
                        () -> notes.add(part.designator() + " has neither a 3D model nor a body; left out."));
                continue;
            }
            double surface = part.placement().onBottom() ? 0 : thickness;
            for (FootprintDrawing.Model model : shown) {
                Optional<Integer> index = modelIndexes.computeIfAbsent(model.reference(),
                        reference -> stepModelOf(reference, stepModels, modelFiles, notes, catalogue));
                if (index.isEmpty()) {
                    notes.add(part.designator() + " is left out: its model " + model.reference() + " cannot be written.");
                    continue;
                }
                ModelFile file = modelFiles.get(model.reference());
                Placement onBoard = ModelPlacement.onBoard(part.placement(), model, surface);
                // A drawn model is in millimetres with Z up already; a file is in its format's frame.
                Placement placement = file == null ? onBoard : onBoard.then(ModelPlacement.ofFile(file, formats.byId(FORMAT).orElseThrow()));
                parts.add(new PlacedPart(part.designator(), index.get(), placement.matrix()));
            }
        }
        if (variant.layers()) {
            PcbArtwork drawn = PcbArtwork.of(fabrication, rules, artwork, font);
            PcbSolid made = PcbSolid.of(drawn, new ArtworkAreas(drawn.outlines(), settings.areaSteps()), drilled, settings.areaSteps(), bodies);
            layers.layersOf(made, drawn, thickness)
                    .forEach(layer -> solids.add(solidOf(layer, true)));
        }
        String pcbColour = (variant.layers() ? layers.settings().core() : layers.settings().bare()).colour();
        Request request = new Request(fabrication.name(), thickness, colourOf(pcbColour), outline, drills, stepModels, parts,
                solids);
        PipedProgram.Ran ran;
        try {
            ran = program.run(JSON.writeValueAsBytes(request));
        } catch (PipedProgramException failed) {
            throw new IllegalStateException(failed.getMessage(), failed);
        }
        ran.errors().lines().filter(line -> line.startsWith(NOTE)).map(line -> line.substring(NOTE.length())).forEach(notes::add);
        if (ran.exitCode() != 0 || ran.output().length == 0) {
            throw new IllegalStateException(program.name() + " did not make the PCB (exit " + ran.exitCode() + "): "
                    + PipedProgram.lastLines(ran.errors(), SAID_LINES));
        }
        return ExportedFiles.single(fabrication.name() + ".step", ran.outputText(), notes);
    }

    /**
     * An area raised into a solid as OpenCascade takes it, in the model's frame; a layer of the artwork is simplified
     * first, as far as {@code areaTolerance}, so it is not written with a face for every step of every curve.
     */
    private RaisedSolid solidOf(PcbLayers.RaisedArea raised, boolean simplified) {
        Geometry area = simplified ? TopologyPreservingSimplifier.simplify(raised.area(), settings.areaTolerance()) : raised.area();
        return new RaisedSolid(raised.name(), colourOf(raised.finish().colour()), raised.bottom(), raised.thickness(),
                polygonsOf(area));
    }

    /** An area as OpenCascade takes it: each polygon its outline and holes, in the model's frame, Y upwards. */
    private static List<List<List<double[]>>> polygonsOf(Geometry area) {
        List<List<List<double[]>>> polygons = new ArrayList<>();
        for (int index = 0; index < area.getNumGeometries(); index++) {
            Geometry piece = area.getGeometryN(index);
            if (piece instanceof GeometryCollection nested && piece != area) {
                polygons.addAll(polygonsOf(nested));
                continue;
            }
            // What an operation leaves besides areas — a line where two met edge to edge — is no solid.
            if (!(piece instanceof Polygon polygon) || polygon.isEmpty()) {
                continue;
            }
            List<List<double[]>> rings = new ArrayList<>(List.of(ringOf(polygon.getExteriorRing())));
            for (int hole = 0; hole < polygon.getNumInteriorRing(); hole++) {
                rings.add(ringOf(polygon.getInteriorRingN(hole)));
            }
            polygons.add(rings);
        }
        return polygons;
    }

    /** A closed ring's points, upwards, its repeated last point dropped. */
    private static List<double[]> ringOf(LineString ring) {
        Coordinate[] points = ring.getCoordinates();
        List<double[]> upward = new ArrayList<>();
        for (int index = 0; index < points.length - 1; index++) {
            upward.add(new double[] {points[index].x, -points[index].y});
        }
        return upward;
    }

    /**
     * The model's STEP file read into the request once, by the index the parts name it with; empty, and said why, for a
     * model the reader may not see or that holds no STEP file.
     */
    private Optional<Integer> stepModelOf(String reference, List<StepModel> stepModels, Map<String, ModelFile> modelFiles,
                                          List<String> notes, CatalogueDocuments documents) {
        ModelDocuments.OpenedModel opened;
        try {
            opened = models.get().open(reference, documents);
        } catch (CadProblem refused) {
            notes.add("The model " + reference + " cannot be read: " + refused.getMessage());
            return Optional.empty();
        }
        List<ModelShapes.Shape> shapes = ModelShapes.of(opened.document());
        if (!shapes.isEmpty()) {
            stepModels.add(new StepModel(reference, null, shapes.stream().map(PcbStepExport::stepShapeOf).toList()));
            return Optional.of(stepModels.size() - 1);
        }
        List<ModelFile> files = ModelFile.of(opened.document());
        Optional<ModelFile> step = files.stream().filter(file -> FORMAT.equals(file.format())).findFirst();
        if (step.isEmpty()) {
            notes.add("The model " + reference + " holds no STEP file"
                    + (files.isEmpty() ? "" : ", only " + files.stream().map(ModelFile::format).distinct().toList()) + ".");
            return Optional.empty();
        }
        try (InputStream input = opened.files().open(step.get().name())) {
            stepModels.add(new StepModel(reference, Base64.getEncoder().encodeToString(input.readAllBytes()), List.of()));
        } catch (FileNotFoundException missing) {
            notes.add("The model " + reference + " names its STEP file " + step.get().name() + " but does not hold it.");
            return Optional.empty();
        } catch (IOException unread) {
            throw new UncheckedIOException("The model " + reference + "'s file " + step.get().name() + " could not be read", unread);
        }
        modelFiles.put(reference, step.get());
        return Optional.of(stepModels.size() - 1);
    }

    /** A drawn model's shape as OpenCascade makes it: its kind, its dimensions, its placement and its material's colour. */
    private static StepShape stepShapeOf(ModelShapes.Shape shape) {
        double[] colour = ModelMaterials.standard().colour(shape.material()).orElseThrow().stream().mapToDouble(Double::doubleValue).toArray();
        double[] placement = ModelShapes.placementOf(shape).matrix();
        return switch (shape) {
            case ModelShapes.Box box -> new StepBox(box.size().stream().mapToDouble(Double::doubleValue).toArray(), placement, colour);
            case ModelShapes.Cylinder cylinder -> new StepCylinder(cylinder.radius(), cylinder.length(), placement, colour);
            case ModelShapes.Prism prism -> new StepPrism(prism.outline(), prism.height(), placement, colour);
            case ModelShapes.Revolved revolved -> new StepRevolved(revolved.profile(), revolved.from(), revolved.to(), placement, colour);
        };
    }

    /** The edge's closed runs, each segment exact where OpenCascade has its form; a run left open is said and left out. */
    private List<Contour> outlineOf(String edge, List<String> notes) {
        if (edge == null || edge.isBlank()) {
            return List.of();
        }
        List<Contour> contours = new ArrayList<>();
        for (PathContours.PathContour run : PathContours.of(PATHS.parse(edge))) {
            if (!run.closed()) {
                notes.add("A run of the PCB's edge from " + said(run.start()) + " is not closed; it is left out of the solid.");
                continue;
            }
            List<Map<String, List<double[]>>> segments = new ArrayList<>();
            for (PathSegment segment : run.segments()) {
                segments.addAll(segmentsOf(segment));
            }
            contours.add(new Contour(segments));
        }
        return contours;
    }

    private List<Map<String, List<double[]>>> segmentsOf(PathSegment segment) {
        return switch (segment) {
            case PathSegment.Line line -> line.from().equals(line.to()) ? List.of()
                    : List.of(Map.of("line", List.of(upward(line.from()), upward(line.to()))));
            case PathSegment.Quadratic curve -> List.of(Map.of("bezier",
                    List.of(upward(curve.from()), upward(curve.handle()), upward(curve.to()))));
            case PathSegment.Cubic curve -> List.of(Map.of("bezier",
                    List.of(upward(curve.from()), upward(curve.first()), upward(curve.second()), upward(curve.to()))));
            case PathSegment.EllipticalArc arc when arc.circular() -> List.of(Map.of("arc",
                    List.of(upward(arc.from()), upward(arc.at(0.5)), upward(arc.to()))));
            case PathSegment.EllipticalArc arc -> {
                List<Map<String, List<double[]>>> steps = new ArrayList<>();
                PathPoint from = arc.from();
                for (int step = 1; step <= settings.ellipseSteps(); step++) {
                    PathPoint to = step == settings.ellipseSteps() ? arc.to() : arc.at((double) step / settings.ellipseSteps());
                    steps.add(Map.of("line", List.of(upward(from), upward(to))));
                    from = to;
                }
                yield steps;
            }
        };
    }

    /** A point of the drawing, whose Y grows downwards, in the model's frame, whose Y grows upwards. */
    private static double[] upward(PathPoint point) {
        return new double[] {point.x(), -point.y()};
    }

    private static double[] upward(PcbDrawing.Point point) {
        return new double[] {point.x(), -point.y()};
    }

    private static String said(PathPoint point) {
        return "(" + point.x() + ", " + point.y() + ")";
    }

    /** {@code #rrggbb} as three shares of 1, in sRGB. */
    private static double[] colourOf(String written) {
        int value = Integer.parseInt(written.substring(1), 16);
        return new double[] {(value >> 16 & 0xff) / 255.0, (value >> 8 & 0xff) / 255.0, (value & 0xff) / 255.0};
    }
}
