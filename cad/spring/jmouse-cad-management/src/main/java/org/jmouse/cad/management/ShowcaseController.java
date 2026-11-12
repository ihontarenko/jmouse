package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.packages.pcb.PcbRules;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentDetail;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.PartLibrary.ComponentView;
import org.jmouse.cad.store.library.PartLibrary.DrawnDocument;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.cad.store.showcase.FeaturedDesigns;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.cad.store.simulation.SimulationViews.SimulationResult;
import org.jmouse.cad.store.simulation.Simulations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🔬 The catalogue's featured designs as the engine draws and computes them: each circuit with its document, the
 * components its parts are, its own response ({@link Simulations#preview}) and — where the catalogue lays it out — its
 * pcb with the footprints its parts sit on. Nothing but what a featured, published design shows ({@link FeaturedDesigns})
 * is answered, a run at a fixed number of samples, so a host may open it to whoever it likes.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/showcase")
public class ShowcaseController {

    /** How many circuits the showcase offers. */
    private static final int SHOWN = 24;

    /** How finely a preview is computed: enough for a trace a few hundred pixels wide. */
    private static final int PREVIEW_SAMPLES = 400;

    /** The mark every sheet draws its ground with. */
    private static final String GROUND_SYMBOL = "ground-symbol";

    private final CadCatalogue    catalogue;
    private final PartLibrary     library;
    private final Simulations     simulations;
    private final FeaturedDesigns  featured;
    private final DocumentExchange exchange;

    public ShowcaseController(CadCatalogue catalogue, PartLibrary library, Simulations simulations, FeaturedDesigns featured,
                              DocumentExchange exchange) {
        this.catalogue = catalogue;
        this.library = library;
        this.simulations = simulations;
        this.featured = featured;
        this.exchange = exchange;
    }

    /**
     * A featured circuit as the showcase draws it.
     *
     * @param components each component its parts are, by slug
     * @param symbols    the drawings its marks are drawn with
     * @param result     its response; null when its text does not run
     * @param board      its pcb; null where the catalogue lays it out on none
     */
    public record FeaturedCircuit(DocumentDetail detail, Map<String, ComponentView> components, List<DrawnDocument> symbols,
                                  SimulationResult result, FeaturedBoard board) {
    }

    /**
     * A featured circuit's pcb as the showcase draws it, flat and in 3D: its 3D models are read through the showcase's own
     * route, {@code /plugins/cad-mesh/showcase/{circuit}/models/{model}/mesh}.
     *
     * @param pcb        the pcb's document
     * @param footprints the footprints its parts sit on
     * @param rules      the ways a pcb is made, which its zones are poured by
     */
    public record FeaturedBoard(DocumentDetail pcb, List<DrawnDocument> footprints, PcbRules rules) {
    }

    @GetMapping
    @PublicEndpoint("lists only published, featured catalogue circuits")
    public List<DocumentSummary> circuits() {
        return catalogue.browse(null, null, "circuit", null, true, false, 0, SHOWN).rows();
    }

    @GetMapping("/{slug}")
    @PublicEndpoint("answers only a published, featured catalogue circuit, computed at a fixed number of samples")
    public FeaturedCircuit circuit(@PathVariable String slug) {
        SimulationResult result = simulations.preview(slug, PREVIEW_SAMPLES);
        DocumentDetail detail = catalogue.detail(slug);
        if (!(detail.model() instanceof Circuit circuit)) {
            throw CadProblem.notFound("The featured circuit");
        }
        Map<String, ComponentView> components = new LinkedHashMap<>();
        for (String component : circuit.parts().stream().map(Part::component).distinct().toList()) {
            components.put(component, library.component(component, null));
        }
        List<DrawnDocument> symbols = library.shelf(SchematicModule.SYMBOL.word(), null).stream()
                .filter(symbol -> symbol.slug().equals(GROUND_SYMBOL)).toList();
        FeaturedBoard board = featured.pcbOf(slug)
                .map(pcb -> new FeaturedBoard(catalogue.detail(pcb.getSlug()), featured.footprints(slug), library.pcbRules()))
                .orElse(null);
        return new FeaturedCircuit(detail, components, symbols, result, board);
    }

    /** What the engine makes of a featured design's pcb — its copper and masks to draw, its checks —, as it is stored. */
    @GetMapping("/{slug}/pcb/derived")
    @PublicEndpoint("answers only the stored pcb of a published, featured catalogue circuit")
    public ExchangeController.PcbDerivedView pcbDerived(@PathVariable String slug) {
        return ExchangeController.PcbDerivedView.of(exchange.derived(featuredPcbSource(slug), null));
    }

    /** A featured design's pcb as made, for its 3D view, as it is stored. */
    @GetMapping("/{slug}/pcb/solid")
    @PublicEndpoint("answers only the stored pcb of a published, featured catalogue circuit")
    public ExchangeController.SolidView pcbSolid(@PathVariable String slug) {
        return ExchangeController.SolidView.of(exchange.made(featuredPcbSource(slug), null));
    }

    /** The stored text of a featured design's pcb; refused where the design lays out none. */
    private String featuredPcbSource(String slug) {
        return featured.pcbOf(slug).map(pcb -> catalogue.detail(pcb.getSlug()).source())
                .orElseThrow(() -> CadProblem.notFound("The featured circuit's pcb"));
    }
}
