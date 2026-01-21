package org.jmouse.crawler.runtime.state.persistence;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.Frontier;
import org.jmouse.crawler.api.InFlightBuffer;

public final class DefaultStateBootstrapper implements StateBootstrapper {

    private final Frontier       frontier;
    private final InFlightBuffer inFlight;

    public DefaultStateBootstrapper(Frontier frontier, InFlightBuffer inFlight) {
        this.frontier = Verify.nonNull(frontier, "frontier");
        this.inFlight = Verify.nonNull(inFlight, "inFlight");
    }

    @Override
    public void restore() {
        // In v1 (no decode yet), nothing to restore from disk.
        // Once codec decode is implemented, this will:
        // 1) load snapshots
        // 2) replay WAL
        // 3) requeue inflight
        requeueInFlight();
    }

    @Override
    public void checkpoint() {
        // Later: snapshot all + rotate WAL
    }

    private void requeueInFlight() {
        inFlight.drainAll().forEach(frontier::offer);
    }
}
