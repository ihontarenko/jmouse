package org.jmouse.crawler.runtime.core;

import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.RoutingHint;
import org.jmouse.crawler.api.TaskOrigin;

import java.net.URI;

public interface TaskFactory {
    ProcessingTask seed(URI url, RoutingHint hint, TaskOrigin origin);
    ProcessingTask childOf(ProcessingTask parent, URI url, RoutingHint hint, TaskOrigin origin);
}
