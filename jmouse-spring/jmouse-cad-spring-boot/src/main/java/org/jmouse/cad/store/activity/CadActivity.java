package org.jmouse.cad.store.activity;

import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.simulation.SimulationJobs;
import org.jmouse.cad.store.simulation.SimulationViews.Job;
import org.jmouse.cad.store.simulation.SimulationViews.JobState;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

/**
 * What the installation is doing now: the simulations it runs or keeps the answers of, and how many circuits run live —
 * for whoever may see the installation's state ({@link CadCapability#SYSTEM_READ}).
 */
public class CadActivity {

    private final SimulationJobs               jobs;
    private final CadAuthority                 authority;
    private final ObjectProvider<LiveActivity> live;

    public CadActivity(SimulationJobs jobs, CadAuthority authority, ObjectProvider<LiveActivity> live) {
        this.jobs = jobs;
        this.authority = authority;
        this.live = live;
    }

    /**
     * @param queued       simulations waiting for a worker
     * @param running      simulations being computed
     * @param liveCircuits circuits running live
     * @param jobs         every simulation kept, newest first: the ones in flight, and the ended ones until they are forgotten
     */
    public record Activity(long queued, long running, int liveCircuits, List<Job> jobs) {
    }

    public Activity activity() {
        authority.require(CadCapability.SYSTEM_READ, CadPlace.installation());
        List<Job> kept = jobs.all();
        LiveActivity circuits = live.getIfAvailable();
        return new Activity(kept.stream().filter(job -> job.state() == JobState.QUEUED).count(),
                            kept.stream().filter(job -> job.state() == JobState.RUNNING).count(),
                            circuits == null ? 0 : circuits.running(), kept);
    }
}
