package org.jmouse.cad.management;

import org.jmouse.cad.pcb.drawing.ViaSize;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.checks.PcbFinding;
import org.jmouse.cad.exchange.pcb.CopperRefresh;
import org.jmouse.cad.exchange.pcb.LayoutMoment;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.PathOutlines;
import org.jmouse.cad.pcb.geometry.NetLengths;
import org.jmouse.cad.pcb.routing.ViaRows;
import org.jmouse.cad.pcb.physics.CopperPaths;
import org.jmouse.cad.pcb.physics.TrackStacks;
import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import java.util.LinkedHashMap;
import org.jmouse.cad.exchange.sheet.SheetFinding;
import org.jmouse.cad.simulation.CircuitSheet;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.cad.exchange.OutputJob;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.cad.store.licence.DocumentLicences;
import org.jmouse.cad.store.exchange.DocumentExchange.Format;
import org.jmouse.cad.store.exchange.DocumentExchange.Imported;
import org.jmouse.cad.store.exchange.DocumentExchange.Read;
import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 🔁 Documents to and from other formats — {@code JMOUSE_CAD_API.md} §2.7. Which formats there are is the plugins'; the
 * routes are the same for every one, so an interface needs to know none of them by name.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class ExchangeController {

    private final DocumentExchange exchange;
    private final LanguageEditing  editing;

    public ExchangeController(DocumentExchange exchange, LanguageEditing editing) {
        this.exchange = exchange;
        this.editing = editing;
    }

    /**
     * Files the reader chose.
     *
     * @param files every file by its path, the one read first among them
     * @param main  the file read first
     * @param title what the made document is called, or null for the file's own word
     * @param owner   whose components a converted part may also name, as {@code TYPE:id}; null for the catalogue's alone
     * @param authors who holds the files, as the reader says — asked of a library's files, whose text does not say it
     * @param license the terms they are under, an SPDX expression, as the reader says
     */
    public record FilesRequest(Map<String, String> files, String main, String title, String owner, String authors, String license) {

        private ExchangeFiles exchangeFiles() {
            try {
                return new ExchangeFiles(files, main, title, new Provenance(null, blankToNull(authors), blankToNull(license)));
            } catch (IllegalArgumentException refused) {
                throw CadProblem.invalid(refused.getMessage());
            }
        }

        private static String blankToNull(String written) {
            return written == null || written.isBlank() ? null : written.strip();
        }
    }

    /** @param owner whose components the document's parts may also name, as {@code TYPE:id}; null for the catalogue's */
    public record ExportRequest(String source, String owner) {
    }

    /**
     * @param job     what the archive is called after
     * @param formats the formats it holds
     * @param owner   whose components the document's parts may also name, as {@code TYPE:id}; null for the catalogue's
     */
    public record ReleaseRequest(String source, String job, String owner) {
    }

    @GetMapping("/exchange")
    @PublicEndpoint("the formats this server converts; nothing stored is read")
    public List<Format> formats() {
        return exchange.formats();
    }

    @PostMapping("/exchange/{format}/read")
    @PublicEndpoint("files the caller sent, converted and handed back; components are read as the reader may see them")
    public Read read(@PathVariable String format, @RequestBody FilesRequest request) {
        return exchange.read(format, request.exchangeFiles(), WrittenOwners.optional(request.owner()));
    }

    @PostMapping(CadManagementRoutes.OWNER + "/exchange/{format}/import")
    @ResponseStatus(HttpStatus.CREATED)
    @PublicEndpoint("writes only where the reader may write the owner's documents, checked in the service")
    public Imported importInto(@OwnerAddress String owner, @PathVariable String format, @RequestBody FilesRequest request) {
        return exchange.importInto(OwnerAddressArgumentResolver.ownerOf(owner), format, request.exchangeFiles());
    }

    /** The output jobs ready to take, those whose formats all run here. */
    @GetMapping("/exchange/output-jobs")
    @PublicEndpoint("the sets of formats this server writes; nothing stored is read")
    public List<OutputJob> outputJobs() {
        return exchange.outputJobs();
    }

    /** A document written in several formats, as one archive. */
    @PostMapping("/exchange/release")
    @PublicEndpoint("a text the caller sent; components are read as the reader may see them")
    public ExportedFiles release(@RequestBody ReleaseRequest request) {
        return exchange.release(request.source(), request.job(), WrittenOwners.optional(request.owner()));
    }

    /** A text as it is written: a panel's as the one pcb it makes. */
    @PostMapping("/exchange/flattened")
    @PublicEndpoint("a text the caller sent; components are read as the reader may see them")
    public DocumentExchange.FlattenedText flattened(@RequestBody ExportRequest request) {
        return exchange.flattened(request.source(), WrittenOwners.optional(request.owner()));
    }

    /** The works of others a text carries, and what each one's licence answers for passing it on. */
    @PostMapping("/exchange/borrowed")
    @PublicEndpoint("a text the caller sent; components are read as the reader may see them")
    public List<DocumentLicences.BorrowedWork> borrowed(@RequestBody ExportRequest request) {
        return exchange.borrowed(request.source(), WrittenOwners.optional(request.owner()));
    }

    /**
     * A finding of a pcb's checks as the editor reads it: its severity's word, what it says, the keys it selects, and
     * where it is, {@code [x, y]}, or null.
     */
    public record FindingView(String severity, String message, List<String> keys, double[] at) {

        static FindingView of(PcbFinding finding) {
            return new FindingView(finding.severity().word(), finding.message(), finding.keys(),
                    finding.at().map(point -> new double[] {point.x(), point.y()}).orElse(null));
        }

        static FindingView of(SheetFinding finding) {
            return new FindingView(finding.severity().word(), finding.message(), finding.keys(),
                    finding.at().map(point -> new double[] {point.x(), point.y()}).orElse(null));
        }
    }

    /** What a pcb's checks find, its text as the editor holds it: the engine's one set of checks. */
    @PostMapping("/pcb/checks")
    @PublicEndpoint("a text the caller sent; the components and footprints it names are read as the reader may see them")
    public List<FindingView> checks(@RequestBody ExportRequest request) {
        return exchange.checks(request.source(), WrittenOwners.optional(request.owner())).stream().map(FindingView::of).toList();
    }

    /**
     * What the engine makes of a pcb is asked of: its text, and — from the pcb editor — the layout it holds, written into the
     * text first, so an edit is worked out in one call rather than written by one and worked out by another.
     *
     * @param drawing the layout as the editor holds it, or null to work out the text as it stands
     * @param owner   whose components and footprints the parts may also name, as {@code TYPE:id}; null for the catalogue's
     */
    /**
     * A pcb an editor holds, asked about: its text and layout, its owner, and — from an editor — the moment the layout is of
     * ({@code shown}, {@code kept}, {@code asked}) and when the reader has the copper poured ({@code on-change}, {@code on-release},
     * {@code on-demand}); none, the copper is poured.
     */
    public record PcbDerivedRequest(String source, PcbDrawing drawing, String owner, String moment, String copper) {

        boolean pours() {
            return moment == null && copper == null || CopperRefresh.of(copper).pours(LayoutMoment.of(moment));
        }
    }

    /** A piece of copper's net — none when it is on none yet — and the piece of joined copper it is in. */
    public record CopperNetView(String net, int group) {
    }

    /** A part's pad: whose, which, its net and its piece of joined copper. */
    public record PadNetView(String designator, String pad, String net, int group) {
    }

    /** A piece of copper drawn as a shape: the shape it is of, its net and its piece of joined copper. */
    public record DrawnNetView(int shape, String net, int group) {
    }

    /** Copper joining nets the circuit keeps apart: the nets, and the tracks, arcs and vias that do it. */
    public record ShortView(List<String> nets, List<Integer> tracks, List<Integer> arcs, List<Integer> vias) {
    }

    /** A connection not laid yet: its net, and the two points nearest each other of the pieces it is to join. */
    public record UnlaidView(String net, double[] from, double[] to) {
    }

    /** A drop a track is widened with: its layer, the track, and its outline as path data. */
    public record TeardropView(String layer, int track, String path) {
    }

    /**
     * The pcb's copper as the engine works it out, each list in the order of the pcb's own: the nets of pads, tracks, arcs,
     * drawn copper and vias, the shorts, what is not laid, each zone poured as path data — empty where nothing is left —
     * and the drops.
     */
    public record CopperView(List<PadNetView> pads, List<CopperNetView> tracks, List<CopperNetView> arcs, List<DrawnNetView> drawn,
                             List<CopperNetView> vias, List<ShortView> shorts, List<UnlaidView> unlaid, List<Map<String, String>> pours,
                             List<TeardropView> teardrops) {

        static CopperView of(PcbArtwork artwork) {
            CopperNets nets = artwork.nets();
            List<PadNetView> pads = new ArrayList<>();
            for (int index = 0; index < nets.pads().size(); index++) {
                CopperNets.PlacedPad pad = nets.pads().get(index);
                pads.add(new PadNetView(pad.designator(), pad.pad().number(), nets.padNets().get(index).orElse(null), nets.padGroup(index)));
            }
            List<CopperNetView> tracks = new ArrayList<>();
            for (int index = 0; index < nets.trackNets().size(); index++) {
                tracks.add(new CopperNetView(nets.trackNets().get(index).orElse(null), nets.trackGroup(index)));
            }
            List<CopperNetView> arcs = new ArrayList<>();
            for (int index = 0; index < nets.arcNets().size(); index++) {
                arcs.add(new CopperNetView(nets.arcNets().get(index).orElse(null), nets.arcGroup(index)));
            }
            List<DrawnNetView> drawn = new ArrayList<>();
            for (int index = 0; index < nets.drawn().size(); index++) {
                drawn.add(new DrawnNetView(nets.drawn().get(index).index(), nets.drawnNets().get(index).orElse(null), nets.drawnGroup(index)));
            }
            List<CopperNetView> vias = new ArrayList<>();
            for (int index = 0; index < nets.viaNets().size(); index++) {
                vias.add(new CopperNetView(nets.viaNets().get(index).orElse(null), nets.viaGroup(index)));
            }
            List<ShortView> shorts = nets.shorts().stream()
                    .map(joined -> new ShortView(joined.nets(), joined.tracks(), joined.arcs(), joined.vias())).toList();
            List<UnlaidView> unlaid = nets.unlaid().stream().map(connection -> new UnlaidView(connection.net(),
                    new double[] {connection.from().x(), connection.from().y()}, new double[] {connection.to().x(), connection.to().y()})).toList();
            // Each zone's pour as its rings by key: a change answered as what changed sends only the rings it reached.
            List<Map<String, String>> pours = artwork.pours().stream().map(pour -> pour.map(PathOutlines::ringPathsOf).orElse(Map.of())).toList();
            List<TeardropView> teardrops = artwork.drops().stream()
                    .map(drop -> new TeardropView(drop.layer(), drop.track(), PathOutlines.pathOf(drop.outline()))).toList();
            return new CopperView(pads, tracks, arcs, drawn, vias, shorts, unlaid, pours, teardrops);
        }
    }

    /** A side's mask and paste as made, each as path data drawn by the even-odd rule: everything that opens the mask, the windows that are no pad's, and the paste. */
    /** A side's mask and paste, each as its rings by key (see the pours). */
    public record MaskView(Map<String, String> openings, Map<String, String> windows, Map<String, String> paste) {

        static MaskView of(DocumentExchange.MaskAreas areas) {
            return new MaskView(PathOutlines.ringPathsOf(areas.openings()), PathOutlines.ringPathsOf(areas.windows()),
                    PathOutlines.ringPathsOf(areas.paste()));
        }
    }

    /** How long a net's copper runs: along its tracks, arcs and drawn lines; how many vias; down their barrels; the whole. */
    public record LengthView(double copper, int vias, double barrels, double total) {

        static LengthView of(NetLengths.Length length) {
            return new LengthView(length.copper(), length.vias(), length.barrels(), length.total());
        }
    }

    /** What the engine makes of a pcb: its copper, each outer side's mask and paste by side, what its checks find, and each net's length. */
    /** Whether an answer about a pcb's copper poured it, or the copper stands as it was — the reader pours on demand. */
    public enum CopperState {
        POURED, STANDING
    }

    /**
     * What the engine makes of a pcb: its copper, each outer side's mask and paste by side, what its checks find, and each net's
     * length — or, where the moment asked about is not poured, only that the copper stands as it was.
     */
    public record PcbDerivedView(CopperState state, CopperView copper, Map<String, MaskView> masks, List<FindingView> findings,
                                 Map<String, LengthView> lengths) {

        /** The copper not poured: the editor keeps what it has, out of date. */
        static PcbDerivedView standing() {
            return new PcbDerivedView(CopperState.STANDING, null, null, null, null);
        }

        static PcbDerivedView of(DocumentExchange.PcbDerived derived) {
            Map<String, MaskView> masks = new LinkedHashMap<>();
            derived.masks().forEach((side, areas) -> masks.put(side, MaskView.of(areas)));
            Map<String, LengthView> lengths = new java.util.TreeMap<>();
            derived.lengths().forEach((net, length) -> lengths.put(net, LengthView.of(length)));
            return new PcbDerivedView(CopperState.POURED, CopperView.of(derived.artwork()), masks, derived.findings().stream().map(FindingView::of).toList(),
                    lengths);
        }
    }

    /** What the engine makes of a pcb an editor holds: its copper to draw, its checks to show — the editor works out neither. */
    @PostMapping("/pcb/derived")
    @PublicEndpoint("a text the caller sent; the components and footprints it names are read as the reader may see them")
    public PcbDerivedView derived(@RequestBody PcbDerivedRequest request) {
        // Whether the moment asked about is poured is the reader's rule, kept here: the editor only tells the moment.
        if (!request.pours()) {
            return PcbDerivedView.standing();
        }
        String source = request.drawing() == null ? request.source()
                : editing.pcb(request.source(), request.drawing(), null, null).source();
        return PcbDerivedView.of(exchange.derived(source, WrittenOwners.optional(request.owner())));
    }

    /** A via's size, as a row of them is made with. */
    public record ViaSizeView(double drill, double diameter) {

        /** The size asked for; none asked, the one the net is laid with. */
        static Optional<ViaSize> of(ViaSizeView view) {
            return Optional.ofNullable(view).map(asked -> new ViaSize(asked.drill(), asked.diameter()));
        }
    }

    /** A zone stitched: the pcb as {@code /pcb/derived} takes it, the zone by its place, the grid's pitch and the vias' size — null for its net's. */
    public record StitchingRequest(String source, PcbDrawing drawing, String owner, int zone, double pitch, ViaSizeView via) {
    }

    /** A track fenced: the pcb, the track by its place, the net its vias are of, their pitch along it and their size. */
    public record FenceRequest(String source, PcbDrawing drawing, String owner, int track, String net, double pitch, ViaSizeView via) {
    }

    /** Vias made in a row, or why none can stand, in words: exactly one of them. */
    public record ViaRowView(List<Via> vias, String refusal) {

        static ViaRowView of(ViaRows.Placed placed) {
            return switch (placed) {
                case ViaRows.Placed.Vias made -> new ViaRowView(made.vias(), null);
                case ViaRows.Placed.Refused refused -> new ViaRowView(null, refused.refusal());
            };
        }
    }

    /** A zone stitched to its net's pours on the other layers by the engine: vias on the pcb's grid, clear of everything else. */
    @PostMapping("/pcb/stitching")
    @PublicEndpoint("a text the caller sent; the components and footprints it names are read as the reader may see them")
    public ViaRowView stitching(@RequestBody StitchingRequest request) {
        String source = request.drawing() == null ? request.source() : editing.pcb(request.source(), request.drawing(), null, null).source();
        return ViaRowView.of(exchange.stitching(source, WrittenOwners.optional(request.owner()), request.zone(), request.pitch(), ViaSizeView.of(request.via())));
    }

    /** A track fenced by a net's vias down each side of it, by the engine. */
    @PostMapping("/pcb/fence")
    @PublicEndpoint("a text the caller sent; the components and footprints it names are read as the reader may see them")
    public ViaRowView fence(@RequestBody FenceRequest request) {
        String source = request.drawing() == null ? request.source() : editing.pcb(request.source(), request.drawing(), null, null).source();
        return ViaRowView.of(exchange.fence(source, WrittenOwners.optional(request.owner()), request.track(), request.net(), request.pitch(),
                ViaSizeView.of(request.via())));
    }


    /** A circuit a new pcb is asked for, by its slug, and whose documents it may name. */
    public record NewPcbRequest(String circuit, String owner) {
    }

    /** A new pcb for a circuit, laid out by the engine from its sheet; no copper laid. */
    @PostMapping("/pcb/new")
    @PublicEndpoint("a circuit read as the reader may see it; nothing is stored")
    public PcbDrawing newPcb(@RequestBody NewPcbRequest request) {
        return exchange.newPcb(request.circuit(), WrittenOwners.optional(request.owner()));
    }

    /** One outer side as made, each as rings. */
    public record SolidSideView(List<List<double[]>> copper, List<List<double[]>> bare, List<List<double[]>> laminate,
                                List<List<double[]>> paste, List<List<double[]>> silkscreen) {
    }

    /** A part's body: its outline, and how high it stands — null where its footprint gives no height, when it is its outline alone. */
    public record SolidBodyView(String designator, String side, List<List<double[]>> rings, Double height) {
    }

    /** A pcb as made, for a 3D view: its thicknesses, the substrate, each outer side by side, and the parts' bodies. */
    public record SolidView(double thickness, double copperThickness, List<List<double[]>> substrate, Map<String, SolidSideView> sides,
                            List<SolidBodyView> bodies) {

        static SolidView of(DocumentExchange.MadePcb made) {
            double tolerance = made.tolerance();
            Map<String, SolidSideView> sides = new LinkedHashMap<>();
            made.solid().sides().forEach((side, areas) -> sides.put(side, new SolidSideView(ringsOf(areas.copper(), tolerance),
                    ringsOf(areas.bare(), tolerance), ringsOf(areas.laminate(), tolerance), ringsOf(areas.paste(), tolerance),
                    ringsOf(areas.silkscreen(), tolerance))));
            List<SolidBodyView> bodies = made.solid().bodies().stream().map(body -> new SolidBodyView(body.designator(), body.side(),
                    List.of(body.outline().stream().map(point -> new double[] {point.x(), point.y()}).toList()),
                    body.height().isPresent() ? body.height().getAsDouble() : null)).toList();
            return new SolidView(made.thickness(), made.copperThickness(), ringsOf(made.solid().substrate(), tolerance), sides, bodies);
        }

        /** An area as closed rings of {@code [x, y]}, outlines turning one way and holes the other, simplified for a view. */
        private static List<List<double[]>> ringsOf(Geometry area, double tolerance) {
            return PathOutlines.ringsOf(TopologyPreservingSimplifier.simplify(area, tolerance)).stream()
                    .map(ring -> ring.stream().map(point -> new double[] {point.x(), point.y()}).toList()).toList();
        }
    }

    /** A pcb as made — the made pcb its 3D files are raised from —, its text as it stands and its layout written into it first. */
    @PostMapping("/pcb/solid")
    @PublicEndpoint("a text the caller sent; the components and footprints it names are read as the reader may see them")
    public SolidView solid(@RequestBody PcbDerivedRequest request) {
        String source = request.drawing() == null ? request.source()
                : editing.pcb(request.source(), request.drawing(), null, null).source();
        return SolidView.of(exchange.made(source, WrittenOwners.optional(request.owner())));
    }

    /** A point a copper path is asked between: where, on which copper layer — null for any —, and how far off copper it may stand. */
    public record ProbeView(double x, double y, String layer, double reach) {

        CopperPaths.Probe probe() {
            return new CopperPaths.Probe(x, y, Optional.ofNullable(layer).filter(name -> !name.isBlank()), reach);
        }
    }

    /** A copper path asked of a pcb: its text and layout as `/pcb/derived` takes them, the two points, the current and the rise allowed. */
    public record CopperPathRequest(String source, PcbDrawing drawing, String owner, ProbeView from, ProbeView to, double current,
                                    double temperatureRise) {
    }

    /** One resistor of the copper: what it is of, where, and how much it carries within the rise. */
    public record BranchView(String thing, int index, String layer, double[] from, double[] to, double length, double width, double thickness,
                             double resistance, double capacity) {

        static BranchView of(CopperPaths.Branch branch) {
            return new BranchView(branch.thing().word(), branch.index(), branch.layer(), new double[] {branch.from().x(), branch.from().y()},
                    new double[] {branch.to().x(), branch.to().y()}, branch.length(), branch.width(), branch.thickness(), branch.resistance(),
                    branch.capacity());
        }
    }

    /** What the current does in one branch; the width or the number of vias that would carry it, where it is too much. */
    public record FlowView(BranchView branch, double current, double power, double rise, boolean overloaded, Double neededWidth, Integer neededVias) {

        static FlowView of(CopperPaths.Flow flow) {
            return new FlowView(BranchView.of(flow.branch()), flow.current(), flow.power(), flow.rise(), flow.overloaded(),
                    flow.neededWidth().orElse(null), flow.neededVias().orElse(null));
        }
    }

    /** The copper between two points: ohms, the drop and heat at the current, the branches most loaded first, the zones on the way. */
    public record PathView(double resistance, double current, double temperatureRise, double drop, double power, List<FlowView> flows,
                           FlowView weakest, List<Integer> pours) {
    }

    /** A copper path, or why there is none in words: exactly one of them. */
    public record CopperAnswerView(PathView path, String refusal) {

        static CopperAnswerView of(CopperPaths.Answer answer) {
            return switch (answer) {
                case CopperPaths.Found found -> {
                    CopperPaths.Path path = found.path();
                    yield new CopperAnswerView(new PathView(path.resistance(), path.current(), path.temperatureRise(), path.drop(), path.power(),
                            path.flows().stream().map(FlowView::of).toList(), path.weakest().map(FlowView::of).orElse(null), path.pours()), null);
                }
                case CopperPaths.Refused refused -> new CopperAnswerView(null, refused.refusal());
            };
        }
    }

    /** The copper between two points of a pcb: the engine's network of it, solved — the editor's copper probe reads it out. */
    @PostMapping("/pcb/copper-path")
    @PublicEndpoint("a text the caller sent; the components and footprints it names are read as the reader may see them")
    public CopperAnswerView copperPath(@RequestBody CopperPathRequest request) {
        String source = request.drawing() == null ? request.source()
                : editing.pcb(request.source(), request.drawing(), null, null).source();
        return CopperAnswerView.of(exchange.copperPath(source, WrittenOwners.optional(request.owner()), request.from().probe(), request.to().probe(),
                request.current(), request.temperatureRise()));
    }

    /**
     * What a pcb's stack is asked about: its text and layout, the layer and the width of a track, the reader's own height and
     * permittivity where they say them, the impedances asked about, and the watts spent on the pcb.
     */
    public record StackRequest(String source, PcbDrawing drawing, String owner, String layer, double width, Double height, Double permittivity,
                               List<Double> impedances, double power) {
    }

    /** A width giving an impedance; null where none searched does. */
    public record WidthView(double impedance, Double width) {
    }

    /** A pair's differential impedance. */
    public record PairView(String name, double impedance) {
    }

    /**
     * A layer's stack and what it means: why there is none in words, else how the track stands, the stack's measures, its
     * impedance and delay, the widths for each impedance asked about, the pairs; and the pcb's warmth, null without an outline.
     */
    public record StackView(String refusal, String line, Double height, Double thickness, Double permittivity, Double impedance, Double delay,
                            List<WidthView> widths, List<PairView> pairs, Double warmth) {

        static StackView of(DocumentExchange.StackAnswer answer) {
            Double warmth = answer.warmth().orElse(null);
            return switch (answer.standing()) {
                case TrackStacks.Unplaned unplaned -> new StackView(unplaned.refusal(), null, null, null, null, null, null, List.of(), List.of(), warmth);
                case TrackStacks.Stands stands -> {
                    TrackStacks.Impedance impedance = answer.impedance().orElseThrow();
                    TrackStacks.Stack stack = stands.stack();
                    yield new StackView(null, stack.line().word(), stack.height(), stack.thickness(), stack.permittivity(), impedance.impedance(),
                            impedance.delay(), answer.widths().entrySet().stream().map(entry -> new WidthView(entry.getKey(), entry.getValue().orElse(null))).toList(),
                            answer.pairs().entrySet().stream().map(entry -> new PairView(entry.getKey(), entry.getValue())).toList(), warmth);
                }
            };
        }
    }

    /** What a pcb's stack means on a layer: impedance, widths, pairs and warmth, by the engine's closed formulas. */
    @PostMapping("/pcb/stack")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public StackView stack(@RequestBody StackRequest request) {
        String source = request.drawing() == null ? request.source()
                : editing.pcb(request.source(), request.drawing(), null, null).source();
        return StackView.of(exchange.stack(source, request.layer(), request.width(), Optional.ofNullable(request.height()),
                Optional.ofNullable(request.permittivity()), request.impedances() == null ? List.of() : request.impedances(), request.power()));
    }

    /**
     * What a sheet's checks are asked of: the circuit's text, and — from a schematic editor — the sheet it drew, which is
     * written into the text first, so an edit is checked in one call rather than written by one and checked by another.
     *
     * @param circuit the sheet as drawn, or null to check the text as it stands
     * @param owner   whose components the parts may also name, as {@code TYPE:id}; null for the catalogue's alone
     */
    public record SheetChecksRequest(String source, CircuitSheet circuit, String owner) {
    }

    /** What a sheet's electrical checks find: the engine's one set of them, which the bench shows. */
    @PostMapping("/circuit/checks")
    @PublicEndpoint("a text the caller sent; the components it names are read as the reader may see them")
    public List<FindingView> sheetChecks(@RequestBody SheetChecksRequest request) {
        String source = request.circuit() == null ? request.source()
                : editing.circuit(request.source(), request.circuit(), null, null, null).source();
        return exchange.sheetChecks(source, WrittenOwners.optional(request.owner())).stream().map(FindingView::of).toList();
    }

    @PostMapping("/exchange/{format}/export")
    @PublicEndpoint("a text the caller sent; components are read as the reader may see them")
    public ExportedFiles export(@PathVariable String format, @RequestBody ExportRequest request) {
        return exchange.export(format, request.source(), WrittenOwners.optional(request.owner()));
    }
}
