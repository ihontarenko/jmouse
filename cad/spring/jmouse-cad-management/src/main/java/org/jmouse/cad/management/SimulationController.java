package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.activity.CadActivity;
import org.jmouse.cad.store.simulation.CatalogueChecks;
import org.jmouse.cad.store.simulation.SimulationViews.ExpectationRequest;
import org.jmouse.cad.store.simulation.SimulationViews.Job;
import org.jmouse.cad.store.simulation.SimulationViews.SimulationAnswer;
import org.jmouse.cad.store.simulation.SimulationViews.SimulationRequest;
import org.jmouse.cad.store.simulation.SimulationViews.SimulationResult;
import org.jmouse.cad.store.simulation.SimulationViews.Verdict;
import org.jmouse.cad.store.simulation.Simulations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ⚡ Running circuits — {@code JMOUSE_CAD_API.md} §2.4. Every route decides inside its service: the owner a run is for
 * travels in the body, so the service asks {@code SIMULATION_RUN} where that owner is, and a job answers only the actor
 * who started it.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class SimulationController {

    private final Simulations     simulations;
    private final CadActivity     activity;
    private final CatalogueChecks checks;

    public SimulationController(Simulations simulations, CadActivity activity, CatalogueChecks checks) {
        this.simulations = simulations;
        this.activity = activity;
        this.checks = checks;
    }

    /** What the installation is doing now: its simulations and its live circuits. */
    @GetMapping("/activity")
    @PublicEndpoint("the service requires SYSTEM_READ for the installation")
    public CadActivity.Activity activity() {
        return activity.activity();
    }

    /** Starts measuring every expectation the published catalogue states — or answers the run already going. */
    @PostMapping("/expectations/catalogue")
    @PublicEndpoint("the service requires SIMULATION_RUN for the installation")
    public ResponseEntity<CatalogueChecks.CheckRun> checkCatalogue() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(checks.start());
    }

    /** The catalogue check as it stands: its progress, and what it has measured. */
    @GetMapping("/expectations/catalogue")
    @PublicEndpoint("the service requires SIMULATION_RUN for the installation")
    public CatalogueChecks.CheckRun catalogueCheck() {
        return checks.current();
    }

    /** A result at once — 200 —, or a job to fetch it from — 202. */
    @PostMapping("/simulations")
    @PublicEndpoint("the service requires SIMULATION_RUN where the circuit's owner is, and reads only what the reader may see")
    public ResponseEntity<SimulationAnswer> simulate(@RequestBody SimulationRequest request) {
        SimulationAnswer answer = simulations.simulate(request);
        HttpStatus status = answer instanceof Job ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(answer);
    }

    @GetMapping("/simulations/jobs/{jobId}")
    @PublicEndpoint("a job answers only the actor who started it; anybody else is told it does not exist")
    public Job job(@PathVariable String jobId) {
        return simulations.job(jobId);
    }

    @GetMapping("/simulations/jobs/{jobId}/result")
    @PublicEndpoint("a job answers only the actor who started it; anybody else is told it does not exist")
    public SimulationResult result(@PathVariable String jobId) {
        return simulations.result(jobId);
    }

    @PostMapping("/simulations/jobs/{jobId}/cancel")
    @PublicEndpoint("a job answers only the actor who started it; anybody else is told it does not exist")
    public Job cancel(@PathVariable String jobId) {
        return simulations.cancel(jobId);
    }

    @PostMapping("/expectations")
    @PublicEndpoint("the service requires SIMULATION_RUN where the document's owner is, and reads only what the reader may see")
    public List<Verdict> expectations(@RequestBody ExpectationRequest request) {
        return simulations.expectations(request);
    }
}
