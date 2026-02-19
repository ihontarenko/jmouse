package org.jmouse.template;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ValueNavigator;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RenderingExecution {

    private final AccessorWrapper             accessorWrapper;
    private final ObjectAccessor              rootAccessor;
    private final RenderingRequest            request;
    private final ValueNavigator              valueNavigator;
    private final NodeTemplateResolver        resolver;
    private final Map<String, ObjectAccessor> variables = new LinkedHashMap<>();

    public RenderingExecution(
            AccessorWrapper accessorWrapper,
            ObjectAccessor rootAccessor,
            RenderingRequest request,
            NodeTemplateResolver resolver
    ) {
        this(accessorWrapper, rootAccessor, request, ValueNavigator.defaultNavigator(), resolver);
    }

    public RenderingExecution(
            AccessorWrapper accessorWrapper,
            ObjectAccessor rootAccessor,
            RenderingRequest request,
            ValueNavigator valueNavigator,
            NodeTemplateResolver resolver
    ) {
        this.accessorWrapper = Verify.nonNull(accessorWrapper, "accessorWrapper");
        this.rootAccessor = Verify.nonNull(rootAccessor, "rootAccessor");
        this.request = Verify.nonNull(request, "request");
        this.valueNavigator = Verify.nonNull(valueNavigator, "valueNavigator");
        this.resolver = Verify.nonNull(resolver, "resolver");
    }

    public NodeTemplateResolver resolver() {
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
