package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.computation.ComputationRequest;
import org.jmouse.parallax.store.computation.ComputationViews.ComputationAnswer;
import org.jmouse.parallax.store.computation.ComputationViews.JobView;
import org.jmouse.parallax.store.computation.ComputationViews.Queued;
import org.jmouse.parallax.store.computation.ComputationViews.Ready;
import org.jmouse.parallax.store.computation.Computing;
import org.jmouse.parallax.store.computation.Jobs;
import org.jmouse.parallax.store.support.ParallaxPage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🧮 Computing a representation — inline, queued or as an animation's frames — and the jobs a queued one becomes.
 *
 * <p>Most routes here decide inside {@code Computing} and {@code Jobs}, per object and per job, and are marked
 * {@link PublicEndpoint}; an owner's job list is the one a product gates by rule. The installation-wide job list
 * lives with the rest of the machinery's administration, in {@link ParallaxAdministrationController}.</p>
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX)
public class ComputationController {

    private static final int  MAXIMUM_FRAMES             = 64;
    private static final long FRAMES_BUDGET_MILLISECONDS = 4000;

    private final Computing computing;
    private final Jobs      jobs;

    public ComputationController(Computing computing, Jobs jobs) {
        this.computing = computing;
        this.jobs      = jobs;
    }

    /** An animation's frames: the same computation at each value of one parameter. */
    public record FramesRequest(ComputationRequest computation, String parameter, List<Double> values) {
    }

    /** One frame: the value it was computed at and its dataset. */
    public record Frame(double value, Ready ready) {
    }

    /**
     * The frames answered, in order. {@code complete} is false when the time budget ran out or a frame was too heavy
     * to compute inline — the client asks again from the first frame missing, and the cache makes the second loop free.
     */
    public record FramesAnswer(List<Frame> frames, boolean complete) {
    }

    /** 200 with the dataset when it is ready (cached or computed now), 202 with the job when it was queued. */
    @PostMapping("/computations")
    @PublicEndpoint("Computing.authorize decides: computing at the owner, or a published catalogue object inline")
    public ResponseEntity<ComputationAnswer> compute(@RequestBody ComputationRequest request) {
        ComputationAnswer answer = computing.compute(request);

        return switch (answer) {
            case Ready ready -> ResponseEntity.ok(ready);
            case Queued queued -> ResponseEntity.status(HttpStatus.ACCEPTED).body(queued);
        };
    }

    /**
     * Frames of an animation in one request: an animation is computed ahead and played back, not computed while it
     * plays. Each frame is authorized and cached exactly as a single computation is.
     */
    @PostMapping("/computations/frames")
    @PublicEndpoint("Computing.authorize decides for every frame, as for one computation")
    public FramesAnswer frames(@RequestBody FramesRequest request) {
        if (request.computation() == null || request.parameter() == null || request.parameter().isBlank()) {
            throw ParallaxProblem.invalid("Frames need a computation and the parameter they run over.");
        }
        if (request.values() == null || request.values().isEmpty() || request.values().size() > MAXIMUM_FRAMES) {
            throw ParallaxProblem.invalid("Ask for 1 to " + MAXIMUM_FRAMES + " frames at once.");
        }

        long        deadline = System.currentTimeMillis() + FRAMES_BUDGET_MILLISECONDS;
        List<Frame> frames   = new ArrayList<>();

        for (Double value : request.values()) {
            if (value == null || !Double.isFinite(value) || System.currentTimeMillis() > deadline) {
                return new FramesAnswer(frames, false);
            }

            Optional<Ready> ready = computing.computeInline(request.computation().withValue(request.parameter(), value));

            if (ready.isEmpty()) {
                return new FramesAnswer(frames, false);
            }

            frames.add(new Frame(value, ready.get()));
        }

        return new FramesAnswer(frames, true);
    }

    @PostMapping("/computations/estimate")
    @PublicEndpoint("estimating reads only what the caller may already read; nothing is computed or stored")
    public Map<String, Object> estimate(@RequestBody ComputationRequest request) {
        return computing.estimate(request);
    }

    /** The jobs queued in one realm, newest first. */
    @GetMapping(ParallaxManagementRoutes.OWNER + "/jobs")
    public ParallaxPage<JobView> ownedJobs(@OwnerAddress String owner, @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        return jobs.inRealm(OwnerAddressArgumentResolver.ownerOf(owner), page, size);
    }

    @GetMapping("/jobs/{jobId}")
    @PublicEndpoint("Jobs decides per job: reading jobs where it was queued, or administering jobs; hidden answers not found")
    public JobView job(@PathVariable String jobId) {
        return jobs.get(jobId);
    }

    @GetMapping("/jobs/{jobId}/dataset")
    @PublicEndpoint("Jobs decides per job, as for reading it")
    public Ready dataset(@PathVariable String jobId) {
        return jobs.dataset(jobId);
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @PublicEndpoint("Jobs decides: cancelling jobs where it was queued, or administering jobs")
    public JobView cancel(@PathVariable String jobId) {
        return jobs.cancel(jobId);
    }

    @PostMapping("/jobs/{jobId}/retry")
    @PublicEndpoint("Jobs decides: cancelling jobs where it was queued, or administering jobs")
    public JobView retry(@PathVariable String jobId) {
        return jobs.retry(jobId);
    }
}
