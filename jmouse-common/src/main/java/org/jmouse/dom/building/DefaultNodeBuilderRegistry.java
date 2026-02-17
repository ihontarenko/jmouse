package org.jmouse.dom.building;

import org.jmouse.core.Verify;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Default {@link NodeBuilderRegistry} implementation.
 */
public final class DefaultNodeBuilderRegistry implements NodeBuilderRegistry {

    private static final Comparator<NodeBuilder> ORDERING =
            Comparator.comparingInt(NodeBuilder::order).reversed();

    private final List<NodeBuilder> builders = new ArrayList<>();

    @Override
    public NodeBuilderRegistry register(NodeBuilder builder) {
        Verify.nonNull(builder, "builder");
        builders.add(builder);
        builders.sort(ORDERING);
        return this;
    }

    @Override
    public NodeBuilder resolve(NodeBuildRequest request, NodeBuildContext context) {
        Verify.nonNull(request, "request");
        Verify.nonNull(context, "context");

        for (NodeBuilder builder : builders) {
            if (builder.supports(request, context)) {
                return builder;
            }
        }

        throw new IllegalStateException("No builder matches request. declaredType=%s, purpose=%s".formatted(
                request.declaredType(), request.purpose().orElse("<none>")
        ));
    }
}
