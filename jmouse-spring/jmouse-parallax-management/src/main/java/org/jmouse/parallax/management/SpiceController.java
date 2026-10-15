package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.spice.SpiceConverter;
import org.jmouse.parallax.spice.SpiceIncludes;
import org.jmouse.parallax.spice.SpiceModelCarriers;
import org.jmouse.parallax.spice.SpiceRunException;
import org.jmouse.parallax.spice.SpiceSimulator;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.spice.SpiceParseException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * SPICE in and out: a netlist as a jMX document, and a jMX circuit or component as a netlist. Nothing is stored here —
 * an editor saves what comes back through the catalogue —, and what a part names is read from the components the
 * reader may see, as an editor's checks read them.
 *
 * <p>What has no counterpart on the other side is named in {@code notes}, by line, never dropped in silence. A text
 * that does not read — SPICE or jMX — is refused as {@code invalid-document} with its {@code line} and
 * {@code column}.</p>
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX + "/spice")
public class SpiceController {

    /** A text is at most this long, as a document is. */
    private static final int MAXIMUM_LENGTH = 1_000_000;

    private final ComponentModels    components;
    private final SpiceModelCarriers carriers;
    private final ParallaxAuthority  authority;
    /** The second engine; empty when the server has no SPICE configured. */
    private final Optional<SpiceSimulator> simulator;

    public SpiceController(ComponentModels components, SpiceModelCarriers carriers, ParallaxAuthority authority,
                           Optional<SpiceSimulator> simulator) {
        this.components = components;
        this.carriers = carriers;
        this.authority = authority;
        this.simulator = simulator;
    }

    /**
     * @param name     the document's title, or null for the netlist's own
     * @param includes the files its {@code .include} and {@code .lib} lines name, by the path they write — texts the
     *                 reader chose from their own files; may be null. The server reads no file and no address itself.
     */
    public record ImportRequest(String text, String name, Map<String, String> includes) {
    }

    /** @param kind {@code COMPONENT} or {@code CIRCUIT} */
    public record ImportResponse(String source, String kind, List<String> notes) {
    }

    public record ExportRequest(String source) {
    }

    public record ExportResponse(String text, List<String> notes) {
    }

    @PostMapping("/import")
    @PublicEndpoint("converting a text the caller sent; components are read as the reader may see them")
    public ImportResponse importNetlist(@RequestBody ImportRequest request) {
        String text = requireText(request == null ? null : request.text(), "A netlist is a SPICE text.");
        SpiceConverter.SpiceImport imported = converted(() -> converter().importNetlist(text, request.name(),
                SpiceIncludes.of(request.includes())));
        return new ImportResponse(imported.source(), imported.kind().name(), imported.notes());
    }

    @PostMapping("/export")
    @PublicEndpoint("converting a text the caller sent; components are read as the reader may see them")
    public ExportResponse exportDocument(@RequestBody ExportRequest request) {
        String source = requireText(request == null ? null : request.source(), "A document is a jMX text: a circuit or "
                + "a component.");
        SpiceConverter.SpiceExport exported = converted(() -> converter().exportDocument(source));
        return new ExportResponse(exported.text(), exported.notes());
    }

    /**
     * @param source     a jMX circuit
     * @param parameters its parameters at other values than the document's — where the bench's sliders stand; may be null
     */
    public record TransientRequest(String source, Map<String, Double> parameters) {
    }

    /** @param deck the SPICE deck that ran, its control block included */
    public record TransientResponse(Dataset.TimeSeries series, String deck, List<String> notes) {
    }

    /**
     * A circuit's transient computed by SPICE — the second engine —, to set beside the engine's own. Each run starts a
     * process, so it is for a signed-in reader; a server without SPICE configured refuses with the reason.
     */
    @PostMapping("/transient")
    @PublicEndpoint("a signed-in reader only, checked here: each run starts a SPICE process")
    public TransientResponse simulateTransient(@RequestBody TransientRequest request) {
        authority.requireActor();
        SpiceSimulator spice = simulator.orElseThrow(() -> ParallaxProblem.refused("SPICE is not configured on this "
                + "server: jmouse.parallax.spice.command names the program that runs it."));
        String source = requireText(request == null ? null : request.source(), "A circuit is a jMX text.");
        Map<String, Double> parameters = request.parameters() == null ? Map.of() : request.parameters();
        SpiceConverter.SpiceTransient run = converted(() -> {
            try {
                return converter().simulateTransient(source, parameters, spice);
            } catch (SpiceRunException failed) {
                throw ParallaxProblem.invalid("spice-failed", failed.getMessage());
            }
        });
        return new TransientResponse(run.series(), run.deck(), run.notes());
    }

    private SpiceConverter converter() {
        return new SpiceConverter(components.forEditing(), carriers);
    }

    /** The conversion, its refusals as the problems the other routes answer with. */
    private static <T> T converted(Supplier<T> conversion) {
        try {
            return conversion.get();
        } catch (SpiceParseException refused) {
            throw ParallaxProblem.invalidDocument(refused.getDetail(), refused.getPosition().line(),
                    refused.getPosition().column());
        } catch (ExperienceParseException refused) {
            throw ParallaxProblem.invalidDocument(refused.getDetail(), refused.getLine(), refused.getColumn());
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw ParallaxProblem.invalid("invalid-conversion", refused.getMessage());
        }
    }

    private static String requireText(String text, String what) {
        if (text == null || text.isBlank()) {
            throw ParallaxProblem.invalid(what);
        }
        if (text.length() > MAXIMUM_LENGTH) {
            throw ParallaxProblem.invalid("A text is at most a million characters.");
        }
        return text;
    }
}
