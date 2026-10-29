package org.jmouse.cad.management;

import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.simulation.AnalysisEngines;
import org.jmouse.cad.store.simulation.AnalysisEngines.Engine;
import org.jmouse.cad.store.simulation.AnalysisEngines.EngineRun;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ⚙️ The engines beside the engine's own — {@code JMOUSE_CAD_API.md} §2.4. Which there are is the plugins'.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class EngineController {

    private final AnalysisEngines engines;

    public EngineController(AnalysisEngines engines) {
        this.engines = engines;
    }

    /**
     * @param parameters its parameters at other values than the document's; may be null
     * @param samples    how many even steps it is printed in; null for this server's own
     * @param owner      whose components its parts may also name, and where it is run, as {@code TYPE:id}
     */
    public record TransientRequest(String source, Map<String, Double> parameters, Integer samples, String owner) {
    }

    @GetMapping("/simulations/engines")
    @PublicEndpoint("which engines this server runs; nothing stored is read")
    public List<Engine> engines() {
        return engines.engines();
    }

    @PostMapping("/simulations/engines/{engine}/transient")
    @PublicEndpoint("runs only where the reader may run circuits, checked in the service")
    public EngineRun runTransient(@PathVariable String engine, @RequestBody TransientRequest request) {
        return engines.runTransient(engine, request.source(), request.parameters(), request.samples(),
                                    WrittenOwners.optional(request.owner()));
    }
}
