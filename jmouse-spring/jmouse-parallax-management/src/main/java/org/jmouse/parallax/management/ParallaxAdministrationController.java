package org.jmouse.parallax.management;

import org.jmouse.parallax.jpa.computation.ComputationJob;
import org.jmouse.parallax.live.LiveActivity;
import org.jmouse.parallax.store.computation.CatalogueWarmup;
import org.jmouse.parallax.store.computation.ComputationViews;
import org.jmouse.parallax.store.computation.ComputationViews.CacheStatistics;
import org.jmouse.parallax.store.computation.ComputationViews.JobView;
import org.jmouse.parallax.store.computation.Jobs;
import org.jmouse.parallax.store.evaluation.Evaluations;
import org.jmouse.parallax.store.evaluation.Evaluations.CheckView;
import org.jmouse.parallax.store.evaluation.Evaluations.RunReport;
import org.jmouse.parallax.store.evaluation.Evaluations.RunView;
import org.jmouse.parallax.store.statistics.ParallaxStatistics;
import org.jmouse.parallax.store.support.ParallaxPage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🛠️ The machinery's administration: every job of the installation, the dataset cache, the catalogue warm-up, what
 * the server is busy with, the statistics a system screen shows, and the model-consistency evaluation runs.
 *
 * <p>⚠️ Every route here spans the whole installation, so a product gates them at its widest scope and with its
 * administrative permissions — see {@link ParallaxAccessRules}.</p>
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX + ParallaxManagementRoutes.ADMINISTRATION)
public class ParallaxAdministrationController {

    private final Jobs               jobs;
    private final CatalogueWarmup    warmup;
    private final Evaluations        evaluations;
    private final ParallaxStatistics statistics;
    private final LiveActivity       liveActivity;

    /**
     * @param liveActivity the circuits running live; an installation without the live module reports none
     */
    public ParallaxAdministrationController(Jobs jobs, CatalogueWarmup warmup, Evaluations evaluations,
                                            ParallaxStatistics statistics, LiveActivity liveActivity) {
        this.jobs         = jobs;
        this.warmup       = warmup;
        this.evaluations  = evaluations;
        this.statistics   = statistics;
        this.liveActivity = liveActivity;
    }

    public record EvaluationRequest(String objectId) {
    }

    @GetMapping("/jobs")
    public ParallaxPage<JobView> allJobs(@RequestParam(required = false) ComputationJob.Status status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        return jobs.all(status, page, size);
    }

    /** The dataset cache and the queue, counted. */
    @GetMapping("/computation")
    public CacheStatistics cache() {
        return jobs.statistics();
    }

    /** What the server is doing now: the warm-up, the queue and the circuits running live. */
    @GetMapping("/computation/activity")
    public ComputationViews.Activity activity() {
        CacheStatistics counted = jobs.statistics();

        return new ComputationViews.Activity(warmup.status(), counted.queued(), counted.running(), counted.failed(),
                liveActivity.running());
    }

    @GetMapping("/computation/warmup")
    public CatalogueWarmup.WarmupStatus warmupStatus() {
        return warmup.status();
    }

    @PostMapping("/computation/warmup")
    public CatalogueWarmup.WarmupStatus startWarmup() {
        return warmup.start();
    }

    /** The workers, the queue, the cache and what the catalogue and the owners hold. */
    @GetMapping("/statistics")
    public ParallaxStatistics.Statistics statistics() {
        return statistics.current();
    }

    @GetMapping("/evaluation/checks")
    public List<CheckView> checks() {
        return evaluations.checks();
    }

    @GetMapping("/evaluation/runs")
    public List<RunView> runs(@RequestParam(defaultValue = "20") int size) {
        return evaluations.runs(size);
    }

    @GetMapping("/evaluation/runs/{runId}")
    public RunReport report(@PathVariable String runId) {
        return evaluations.report(runId);
    }

    @PostMapping("/evaluation/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public RunReport run(@RequestBody EvaluationRequest request) {
        return evaluations.run(request.objectId());
    }
}
