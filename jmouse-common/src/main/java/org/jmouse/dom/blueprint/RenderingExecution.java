package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ValueNavigator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Execution context for a single pipeline run.
 *
 * <p>Holds the root accessor, request attributes, runtime variables (for repeat scopes),
 * and utilities required during materialization.</p>
 */
public final class RenderingExecution {

    private final AccessorWrapper             accessorWrapper;
    private final ObjectAccessor              rootAccessor;
    private final RenderingRequest            request;
    private final ValueNavigator              valueNavigator;
    private final BlueprintResolver           resolver;
    private final Map<String, ObjectAccessor> variables = new LinkedHashMap<>();

    public RenderingExecution(
            AccessorWrapper accessorWrapper,
            ObjectAccessor rootAccessor,
            RenderingRequest request,
            BlueprintResolver resolver
    ) {
        this(accessorWrapper, rootAccessor, request, ValueNavigator.defaultNavigator(), resolver);
    }

    public RenderingExecution(
            AccessorWrapper accessorWrapper,
            ObjectAccessor rootAccessor,
            RenderingRequest request,
            ValueNavigator valueNavigator,
            BlueprintResolver resolver
    ) {
        this.accessorWrapper = Verify.nonNull(accessorWrapper, "accessorWrapper");
        this.rootAccessor = Verify.nonNull(rootAccessor, "rootAccessor");
        this.request = Verify.nonNull(request, "request");
        this.valueNavigator = Verify.nonNull(valueNavigator, "valueNavigator");
        this.resolver = Verify.nonNull(resolver, "resolver");
    }

    public BlueprintResolver resolver() {
        return resolver;
    }

    public AccessorWrapper accessorWrapper() {
        return accessorWrapper;
    }

    public ObjectAccessor rootAccessor() {
        return rootAccessor;
    }

    public RenderingRequest request() {
        return request;
    }

    public Map<String, ObjectAccessor> variables() {
        return variables;
    }

    public ValueNavigator valueNavigator() {
        return valueNavigator;
    }
}
