package org.jmouse.meterializer;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.meterializer.hooks.RenderingHookChain;
import org.jmouse.meterializer.hooks.RenderingStage;

import java.util.function.UnaryOperator;

import static org.jmouse.core.Verify.nonNull;

/**
 * Base rendering pipeline implementation combining:
 * template resolution, execution context creation, materialization, and hooks. 🧬
 *
 * <p>
 * This pipeline orchestrates the high-level rendering flow:
 * </p>
 *
 * <pre>{@code
 * render(key, model)
 *   → build RenderingRequest
 *   → wrap model into ObjectAccessor
 *   → create RenderingExecution
 *   → resolve NodeTemplate by key
 *   → materialize template into result (T)
 *   → notify hooks across stages
 * }</pre>
 *
 * <h3>Core components</h3>
 * <ul>
 *     <li>{@link NodeTemplateResolver} — resolves templates by key</li>
 *     <li>{@link TemplateMaterializer} — materializes {@link NodeTemplate} into {@code T}</li>
 *     <li>{@link AccessorWrapper} — wraps the input model for path navigation</li>
 *     <li>{@link RenderingHookChain} — stage hooks (before/after + failure)</li>
 * </ul>
 *
 * <h3>Hooks</h3>
 * <p>
 * Hooks are invoked around key stages to allow instrumentation, debugging,
 * transformation, or cross-cutting concerns (metrics, tracing, logging).
 * Failures are reported via {@link RenderingHookChain#onFailure(Throwable, RenderingStage, RenderingExecution)}.
 * </p>
 *
 * <p>
 * Thread-safety depends on provided collaborators (resolver/materializer/accessorWrapper/hooks).
 * The pipeline itself holds only final references and creates per-invocation
 * {@link RenderingRequest} and {@link RenderingExecution}.
 * </p>
 *
 * @param <T> final rendering result type (e.g. DOM node, HTML string)
 * @param <R> self type of the concrete pipeline (used by builders for fluent APIs)
 */
abstract public class AbstractRenderingPipeline<T, R extends AbstractRenderingPipeline<T, R>> implements Rendering<T> {

    private final NodeTemplateResolver    resolver;
    private final TemplateMaterializer<T> materializer;
    private final AccessorWrapper         accessorWrapper;
    private final RenderingHookChain<T>   hookChain;

    /**
     * Creates a pipeline from a {@link PipelineBuilder}.
     *
     * @param builder pipeline builder with required collaborators
     */
    protected AbstractRenderingPipeline(PipelineBuilder<T, R> builder) {
        this.resolver = builder.templateResolver();
        this.materializer = builder.materializer();
        this.accessorWrapper = builder.accessorWrapper();
        this.hookChain = new RenderingHookChain<>(builder.hooks());
    }

    /**
     * Renders a template by key using the provided model.
     *
     * <p>Equivalent to calling {@link #render(String, Object, UnaryOperator)} with identity customizer.</p>
     *
     * @param templateReference template identifier
     * @param data model/root object
     * @return materialized result
     */
    public T render(String templateReference, Object data) {
        return render(templateReference, data, request -> request);
    }

    /**
     * Renders a template by key using the provided model and request customization.
     *
     * <p>
     * This method:
     * </p>
     * <ol>
     *     <li>builds {@link RenderingRequest} via {@code requestCustomizer}</li>
     *     <li>wraps {@code data} with {@link AccessorWrapper}</li>
     *     <li>creates {@link RenderingExecution}</li>
     *     <li>invokes hooks around resolution/materialization</li>
     * </ol>
     *
     * <p>
     * Any thrown exception is rethrown after notifying {@link RenderingHookChain#onFailure}.
     * Exceptions thrown from failure hooks are ignored.
     * </p>
     *
     * @param templateReference template identifier
     * @param data model/root object (may be {@code null} depending on {@link AccessorWrapper})
     * @param requestCustomizer request customization callback
     * @return materialized result
     */
    public T render(String templateReference, Object data, UnaryOperator<RenderingRequest> requestCustomizer) {
        nonNull(templateReference, "templateReference");
        nonNull(requestCustomizer, "requestCustomizer");

        RenderingRequest request      = requestCustomizer.apply(new RenderingRequest());
        ObjectAccessor   rootAccessor = accessorWrapper.wrap(data);

        RenderingExecution execution = new RenderingExecution(
                accessorWrapper,
                rootAccessor,
                request,
                resolver
        );

        try {
            hookChain.beforeTemplateResolve(templateReference, data, request, execution);

            NodeTemplate template = resolver.resolve(templateReference, execution);

            hookChain.afterTemplateResolve(templateReference, template, execution);
            hookChain.beforeMaterialize(template, execution);

            T node = materializer.materialize(template, execution);

            hookChain.afterMaterialize(node, execution);

            return node;
        } catch (Throwable exception) {
            try {
                hookChain.onFailure(exception, guessStage(exception), execution);
            } catch (Throwable ignored) {}
            throw exception;
        }
    }

    /**
     * Attempts to guess rendering stage for an exception. 🧭
     *
     * <p>
     * The default implementation returns {@link RenderingStage#BEFORE_TEMPLATE_RESOLVE}.
     * Subclasses may override or extend this logic to provide more accurate stage detection.
     * </p>
     *
     * @param exception thrown exception
     * @return best-effort stage value
     */
    private RenderingStage guessStage(Throwable exception) {
        return RenderingStage.BEFORE_TEMPLATE_RESOLVE;
    }

}