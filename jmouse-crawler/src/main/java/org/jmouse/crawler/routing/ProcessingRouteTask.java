package org.jmouse.crawler.routing;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.crawler.runtime.ProcessingTask;
import org.jmouse.crawler.runtime.RunContext;

@FunctionalInterface
public interface ProcessingRouteTask extends Matcher<ProcessingRouteTask.Candidate> {
   // boolean test(ProcessingTask task, RunContext context);
    record Candidate (ProcessingTask task, RunContext context) {}
}
