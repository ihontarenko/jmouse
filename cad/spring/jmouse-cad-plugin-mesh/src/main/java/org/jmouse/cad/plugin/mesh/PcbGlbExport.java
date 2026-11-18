package org.jmouse.cad.plugin.mesh;

import org.jmouse.settings.Label;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.ArtworkAreas;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.pcb.PcbSolidLayers;
import org.jmouse.cad.exchange.pcb.PartBodies;
import org.jmouse.cad.exchange.pcb.PcbSolid;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.mesh.ModelPlacement;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.mesh.MeshScene;
import org.jmouse.mesh.Placement;
import org.jmouse.mesh.gltf.GlbWriter;
import org.jmouse.strokefont.StrokeFont;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A pcb as binary glTF, for a viewer, a renderer or a web page: the pcb a solid of its thickness with every hole
 * through it, the outer copper, the solder mask and the silkscreen over each face as thin solids, drawn from the same
 * {@link PcbArtwork} as the Gerber files, and each part its 3D model where it stands — each model written once, however
 * many parts show it — or, with none, its footprint's body ({@link PartBodies}). Each is a node named for what it is; the
 * whole is in metres with Y up, as glTF's standard says. A part with neither model nor body, or whose model is not read,
 * is left out and said so.
 */
public final class PcbGlbExport implements DocumentExport {

    /** The rule a pcb's thickness is in. */
    private static final String THICKNESS = "pcb-thickness";

    /** What the pcb's own solid is called after the pcb, as its layers are called after it and their side. */
    private static final String PCB = "pcb";

    /** The format the file is, among the mesh formats: its frame and unit. */
    private static final String FORMAT = "gltf-binary";

    /**
     * The export's data ({@code settings/plugin/cad-mesh/glb}).
     *
     * @param id            its word on the exchange routes: {@code glb}
     * @param title         what the export is called where formats are offered
     * @param areaSteps     how many straight steps a quarter of a round end, a disc, an arc or a drill is drawn with
     * @param areaTolerance how far a layer's outline may move when it is simplified
     */
    public record Settings(@Label("ID") String id,
                           @Label("Title") String title,
                           @Label("Area steps") int areaSteps,
                           @Label(value = "Area tolerance", unit = "mm") double areaTolerance) {
    }

    private final Supplier<Settings>                  settings;
    private final Supplier<PcbArtwork.Settings>     artwork;
    private final Supplier<PcbSolidLayers.Settings>               layers;
    private final StrokeFont                font;
    private final Supplier<ModelMeshService> meshes;
    private final Placement                 frame = MeshFormats.standard().byId(FORMAT).orElseThrow().frame();
    private final GlbWriter                 writer = new GlbWriter();

    /**
     * @param artwork what the copper, the mask and the silkscreen are drawn with, as every format draws them
     * @param layers  how thick the layers over the pcb's faces are and how each looks, as every 3D format models them
     * @param meshes  the models as meshes, asked for on each export: they stand on the catalogue, which stands on the plugins
     */
    /** The ways a pcb is made, read through the engine's tables. */
    private final CadStandards standards;

    public PcbGlbExport(Supplier<Settings> settings, Supplier<PcbArtwork.Settings> artwork, Supplier<PcbSolidLayers.Settings> layers, StrokeFont font,
                          Supplier<ModelMeshService> meshes, CadStandards standards) {
        this.standards = standards;
        this.settings = settings;
        this.artwork = artwork;
        this.layers = layers;
        this.font = font;
        this.meshes = meshes;
    }

    @Override
    public String id() {
        return settings.get().id();
    }

    @Override
    public String title() {
        return settings.get().title();
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
        double thickness = rules.get(THICKNESS);
        PcbArtwork drawn = PcbArtwork.of(fabrication, rules, artwork.get(), font);
        List<PcbDrills.Drill> drilled = PcbDrills.of(fabrication.pcb(), fabrication.footprints());
        PartBodies partBodies = new PartBodies(artwork.get().curveSteps());
        PcbSolid made = PcbSolid.of(drawn, new ArtworkAreas(drawn.outlines(), settings.get().areaSteps()), drilled, settings.get().areaSteps(), partBodies);
        Geometry pcb = made.substrate();
        if (pcb.isEmpty()) {
            throw new IllegalArgumentException("The PCB has no closed edge, so there is no PCB to make a solid of.");
        }
        List<Mesh> bodies = new ArrayList<>();
        List<MeshScene.Node> nodes = new ArrayList<>();
        raised(new PcbSolidLayers.RaisedArea(fabrication.name() + " " + PCB, new PcbSolidLayers(layers.get()).settings().core(), 0, thickness, pcb), bodies,
                nodes);
        new PcbSolidLayers(layers.get()).layersOf(made, drawn, thickness).forEach(layer ->
                raised(new PcbSolidLayers.RaisedArea(layer.name(), layer.finish(), layer.bottom(), layer.thickness(),
                        TopologyPreservingSimplifier.simplify(layer.area(), settings.get().areaTolerance())), bodies, nodes));
        Map<String, Optional<Integer>> models = new LinkedHashMap<>();
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            List<FootprintDrawing.Model> shown = part.drawing().map(ModelPlacement::shownOf).orElse(List.of());
            if (shown.isEmpty()) {
                partBodies.of(part).filter(PartBodies.PartBody::raised).ifPresentOrElse(body -> raised(new PcbSolidLayers(layers.get()).bodyOf(body, thickness), bodies, nodes),
                        () -> notes.add(part.designator() + " has neither a 3D model nor a body; left out."));
                continue;
            }
            double surface = part.placement().onBottom() ? 0 : thickness;
            for (FootprintDrawing.Model model : shown) {
                Optional<Integer> index = models.computeIfAbsent(model.reference(), reference -> modelOf(reference, bodies, notes, catalogue));
                if (index.isEmpty()) {
                    notes.add(part.designator() + " is left out: its model " + model.reference() + " is not read.");
                    continue;
                }
                nodes.add(new MeshScene.Node(part.designator(), index.get(), ModelPlacement.onBoard(part.placement(), model, surface)));
            }
        }
        byte[] glb = writer.write(new MeshScene(fabrication.name(), frame, bodies, nodes));
        return ExportedFiles.binary(fabrication.name() + ".glb", glb, notes);
    }

    /** An area raised into a body of its own, standing where it was drawn. */
    private static void raised(PcbSolidLayers.RaisedArea area, List<Mesh> bodies, List<MeshScene.Node> nodes) {
        MeshBuilder builder = new MeshBuilder();
        AreaPrisms.raise(builder, AreaPrisms.materialOf(area.name(), area.finish()), area.area(), area.bottom(), area.thickness());
        nodes.add(new MeshScene.Node(area.name(), bodies.size(), Placement.IDENTITY));
        bodies.add(builder.build());
    }

    /** The model read into the bodies once, by the index the parts name it with; empty, and said why, for one not read. */
    private Optional<Integer> modelOf(String reference, List<Mesh> bodies, List<String> notes, CatalogueDocuments documents) {
        try {
            bodies.add(meshes.get().mesh(reference, documents));
            return Optional.of(bodies.size() - 1);
        } catch (CadProblem refused) {
            notes.add("The model " + reference + " cannot be read: " + refused.getMessage());
            return Optional.empty();
        }
    }
}
