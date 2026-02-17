package org.jmouse.dom.building;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;

import java.lang.reflect.Type;
import java.util.function.UnaryOperator;

/**
 * High-level facade for building DOM node trees from arbitrary data.
 */
public final class DomNodeBuilder {

    private final NodeBuilderRegistry registry;

    public DomNodeBuilder(NodeBuilderRegistry registry) {
        this.registry = Verify.nonNull(registry, "registry");
    }

    public static DomNodeBuilder standard() {
        NodeBuilderRegistry registry = NodeBuilderRegistry.createDefault();
        StandardNodeBuilderModule.registerAll(registry);
        return new DomNodeBuilder(registry);
    }

    public Node build(Object value, Type declaredType) {
        return build(value, declaredType, UnaryOperator.identity());
    }

    public Node build(Object value, Type declaredType, UnaryOperator<NodeBuildRequest> requestCustomizer) {
        Verify.nonNull(declaredType, "declaredType");
        Verify.nonNull(requestCustomizer, "requestCustomizer");

        NodeBuildRequest request = requestCustomizer.apply(NodeBuildRequest.of(value, declaredType));

//        DefaultNodeBuildContext context = new DefaultNodeBuildContext(registry);

//        return context.build(request);
        return null;
    }
}
