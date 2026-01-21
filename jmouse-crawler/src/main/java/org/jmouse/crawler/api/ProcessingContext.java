package org.jmouse.crawler.api;

import java.net.URI;

/**
 * Mutable processing context associated with a single {@link ProcessingTask}. 🧠
 *
 * <p>{@code ProcessingContext} acts as the shared state holder during task execution.
 * It exposes access to:</p>
 * <ul>
 *   <li>the current task and run-level context</li>
 *   <li>fetch and parse results</li>
 *   <li>decision logging</li>
 *   <li>routing information</li>
 *   <li>URL discovery and enqueueing</li>
 * </ul>
 *
 * <p>The context is typically created per task and passed through
 * fetch → parse → route → enqueue stages.</p>
 */
public interface ProcessingContext {

    /**
     * Return the task currently being processed.
     */
    ProcessingTask task();

    /**
     * Return the run-level context shared across all tasks.
     */
    RunContext run();

    /**
     * Return the fetch result associated with this task, if available.
     *
     * @return fetch result, or {@code null} if fetching has not yet occurred
     */
    FetchResult fetchResult();

    /**
     * Set the fetch result produced by the {@link Fetcher}.
     *
     * @param result fetch result to associate with this context
     */
    void setFetchResult(FetchResult result);

    /**
     * Return the parsed document derived from the fetch result.
     *
     * @return parsed document, or {@code null} if parsing has not yet occurred
     */
    ParsedDocument document();

    /**
     * Set the parsed document produced from the fetch result.
     *
     * @param document parsed document
     */
    void setDocument(ParsedDocument document);

    /**
     * Return the decision log used to record accept/reject outcomes
     * during processing of this task.
     */
    DecisionLog decisions();

    /**
     * Return the identifier of the route selected for this task.
     *
     * <p>Routes typically represent processing pipelines or handlers.</p>
     *
     * @return route identifier, or {@code null} if not yet assigned
     */
    String routeId();

    /**
     * Set the identifier of the route selected for this task.
     *
     * @param routeId route identifier
     */
    void setRouteId(String routeId);

    /**
     * Enqueue a newly discovered URL using default routing hints.
     *
     * @param url discovered URL
     */
    void enqueue(URI url);

    /**
     * Enqueue a newly discovered URL with an explicit routing hint.
     *
     * @param url  discovered URL
     * @param hint routing hint influencing downstream processing
     */
    void enqueue(URI url, RoutingHint hint);

    /**
     * Obtain a utility object from the run-level utility registry.
     *
     * <p>This is a convenience method equivalent to:</p>
     * <pre>{@code
     * run().utilities().get(type)
     * }</pre>
     *
     * @param type utility class
     * @param <T>  utility type
     * @return utility instance, or {@code null} if not registered
     */
    default <T> T utility(Class<T> type) {
        return run().utilities().get(type);
    }
}
