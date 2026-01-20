package org.jmouse.crawler.routing;

import java.net.URI;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.crawler.runtime.ProcessingTask;
import org.jmouse.crawler.runtime.RoutingHint;
import org.jmouse.crawler.runtime.RunContext;

@FunctionalInterface
public interface ProcessingRouteTask extends Matcher<ProcessingRouteTask.Candidate> {
    record Candidate (RoutingHint hint, ProcessingTask task, RunContext context) {
        public URI url() {
            return task().url();
        }
    }
}
