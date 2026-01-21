package org.jmouse.crawler.route;

import java.net.URI;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.RoutingHint;
import org.jmouse.crawler.api.RunContext;

@FunctionalInterface
public interface ProcessingTaskMatcher extends Matcher<ProcessingTaskMatcher.Candidate> {
    record Candidate (RoutingHint hint, ProcessingTask task, RunContext context) {
        public URI url() {
            return task().url();
        }
    }
}
