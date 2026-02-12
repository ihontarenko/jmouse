package org.jmouse.crawler.runtime.core;

import org.jmouse.core.IdGenerator;
import org.jmouse.core.trace.TraceContext;
import org.jmouse.crawler.api.*;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

/**
 * Default implementation of {@link TaskFactory} responsible for creating
 * {@link ProcessingTask} instances. 🏭
 *
 * <p>{@code DefaultTaskFactory} centralizes all rules related to task creation,
 * ensuring consistent initialization of:</p>
 * <ul>
 *   <li>task identity ({@link TaskId})</li>
 *   <li>trace propagation ({@link TraceContext})</li>
 *   <li>scheduling timestamps</li>
 *   <li>depth and parent relationships</li>
 * </ul>
 *
 * <p>The factory is intentionally simple and deterministic:
 * all time-sensitive values are derived from the provided {@link Clock},
 * and all identifiers are generated via the supplied {@link IdGenerator}.</p>
 *
 * <h3>Task creation semantics</h3>
 * <ul>
 *   <li><strong>Seed tasks</strong> start a new crawl tree and a new trace</li>
 *   <li><strong>Child tasks</strong> inherit trace context and increment depth</li>
 * </ul>
 *
 * <p>This design ensures that task construction logic is not duplicated
 * across the crawler runtime and remains easy to evolve.</p>
 */
public final class DefaultTaskFactory implements TaskFactory {

    private final Clock                       clock;
    private final IdGenerator<String, String> generator;

    /**
     * Create a new task factory.
     *
     * @param clock      time source used to initialize {@code scheduledAt}
     * @param generator  identifier generator used to create {@link TaskId}s
     */
    public DefaultTaskFactory(Clock clock, IdGenerator<String, String> generator) {
        this.clock = clock;
        this.generator = generator;
    }

    /**
     * Create a seed (sourceRoot) task for the given URL.
     *
     * <p>A seed task:</p>
     * <ul>
     *   <li>has depth {@code 0}</li>
     *   <li>has no parent</li>
     *   <li>starts a new {@link TraceContext}</li>
     *   <li>is scheduled for immediate execution</li>
     * </ul>
     *
     * @param url     initial URL to crawl
     * @param hint    routing hint associated with the task
     * @param origin  origin metadata describing how the task was introduced
     * @return newly created seed {@link ProcessingTask}
     */
    @Override
    public ProcessingTask seed(URI url, RoutingHint hint, TaskOrigin origin) {
        Instant      now   = clock.instant();
        TraceContext trace = TraceContext.root();

        return new ProcessingTask(
                new TaskId(generator.generate()),
                trace,
                url,
                0,
                null,
                origin,
                0,
                now,
                0,
                hint
        );
    }

    /**
     * Create a child task derived from an existing parent task.
     *
     * <p>A child task:</p>
     * <ul>
     *   <li>inherits and extends the parent's {@link TraceContext}</li>
     *   <li>has depth {@code parent.depth() + 1}</li>
     *   <li>records the parent's URL as its {@code parent}</li>
     *   <li>inherits the parent's priority</li>
     *   <li>is scheduled for immediate execution</li>
     * </ul>
     *
     * @param parent parent task from which this task is derived
     * @param url    discovered child URL
     * @param hint   routing hint associated with the task
     * @param origin origin metadata describing how the task was discovered
     * @return newly created child {@link ProcessingTask}
     */
    @Override
    public ProcessingTask childOf(ProcessingTask parent, URI url, RoutingHint hint, TaskOrigin origin) {
        Instant      now   = clock.instant();
        TraceContext trace = parent.trace().child();

        return new ProcessingTask(
                new TaskId(generator.generate()),
                trace,
                url,
                parent.depth() + 1,
                parent.url(),
                origin,
                parent.priority(),
                now,
                0,
                hint
        );
    }
}
