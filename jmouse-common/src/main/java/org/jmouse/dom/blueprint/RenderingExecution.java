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

    private final AccessorWrapper  accessorWrapper;
    private final ObjectAccessor   rootAccessor;
    private final RenderingRequest request;
    private final ValueNavigator   valueNavigator;

    private final Map<String, ObjectAccessor> variables = new LinkedHashMap<>();
    private final Map<String, Object>         diagnostics = new LinkedHashMap<>();

    public RenderingExecution(AccessorWrapper accessorWrapper, ObjectAccessor rootAccessor, RenderingRequest request) {
        this(accessorWrapper, rootAccessor, request, ValueNavigator.defaultNavigator());
    }

    public RenderingExecution(
            AccessorWrapper accessorWrapper,
            ObjectAccessor rootAccessor,
            RenderingRequest request,
            ValueNavigator valueNavigator
    ) {
        this.accessorWrapper = Verify.nonNull(accessorWrapper, "accessorWrapper");
        this.rootAccessor = Verify.nonNull(rootAccessor, "rootAccessor");
        this.request = Verify.nonNull(request, "request");
        this.valueNavigator = Verify.nonNull(valueNavigator, "valueNavigator");
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

    public Map<String, Object> diagnostics() {
        return diagnostics;
    }

    public ValueNavigator valueNavigator() {
        return valueNavigator;
    }
}
