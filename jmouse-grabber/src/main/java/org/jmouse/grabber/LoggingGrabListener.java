package org.jmouse.grabber;

import org.jmouse.grabber.fetch.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🪵 A line per event, at debug — except a failure, which is a warning.
 *
 * <p>The decision line is the one worth having: it names the route that claimed a page and what it
 * concluded, which is the question a run behaving oddly always turns out to be about.</p>
 */
final class LoggingGrabListener implements GrabListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.jmouse.grabber");

    @Override
    public void queued(Visit visit) {
        LOGGER.debug("queued {} at depth {}", visit.address(), visit.depth());
    }

    @Override
    public void fetched(Visit visit, PageResponse response) {
        LOGGER.debug("fetched {} -> {} in {}ms", visit.address(), response.status().getCode(),
                     response.elapsed().toMillis());
    }

    @Override
    public void decided(Visit visit, String route, Verdict verdict) {
        LOGGER.debug("{} on {} decided {}", route, visit.address(), describe(verdict));
    }

    @Override
    public void emitted(Visit visit, Object item) {
        LOGGER.debug("emitted from {}: {}", visit.address(), item);
    }

    @Override
    public void failed(Failure failure) {
        if (failure.exhausted()) {
            LOGGER.warn("gave up on {}: {}", failure.visit().address(), failure.message());
        } else {
            LOGGER.debug("retrying {}: {}", failure.visit().address(), failure.message());
        }
    }

    @Override
    public void finished(GrabRun run) {
        LOGGER.info("run finished: {} fetched, {} emitted, {} failed, {} skipped in {}s",
                    run.fetched(), run.emitted(), run.failed(), run.skipped(), run.elapsed().toSeconds());
    }

    private String describe(Verdict verdict) {
        return switch (verdict) {
            case Verdict.Followed followed -> "followed " + followed.visits().size();
            case Verdict.Leaf ignored -> "leaf";
            case Verdict.Skipped skipped -> "skipped (" + skipped.reason() + ")";
            case Verdict.Retry retry -> "retry after " + retry.after().toMillis() + "ms";
            case Verdict.Stopped stopped -> "stopped (" + stopped.reason() + ")";
        };
    }

}
