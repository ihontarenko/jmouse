package org.jmouse.common.dom.rendering;

import org.jmouse.core.Verify;
import org.jmouse.common.dom.Node;
import org.jmouse.common.dom.NodeContext;
import org.jmouse.common.dom.Renderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Default {@link RendererRegistry} implementation.
 *
 * <p>Thread-safety: registrations are stored in a {@link CopyOnWriteArrayList}.
 * Resolution iterates a snapshot and therefore is safe for concurrent reads and registrations.</p>
 */
public final class DefaultRendererRegistry implements RendererRegistry {

    public static final int HIGHEST_PRECEDENCE = Integer.MAX_VALUE;
    public static final int LOWEST_PRECEDENCE  = Integer.MIN_VALUE;

    private static final Comparator<RendererRegistration> ORDERING =
            Comparator.comparingInt(RendererRegistration::order).reversed();

    private final CopyOnWriteArrayList<RendererRegistration> registrations = new CopyOnWriteArrayList<>();

    @Override
    public RendererRegistry register(Predicate<? super Node> predicate, int order, Renderer renderer) {
        Verify.nonNull(predicate, "predicate");
        Verify.nonNull(renderer, "renderer");

        registrations.add(new RendererRegistration(predicate, order, renderer));
        registrations.sort(ORDERING);

        return this;
    }

    @Override
    public Renderer resolve(Node node, NodeContext context) {
        Verify.nonNull(node, "node");
        Verify.nonNull(context, "context");

        for (RendererRegistration registration : registrations) {
            if (registration.predicate().test(node)) {
                return registration.renderer();
            }
        }

        throw new IllegalStateException("No renderer matches node: " + node.getNodeType() + " / " + node.getTagName());
    }

    /**
     * Returns a snapshot of current registrations (useful for diagnostics).
     *
     * @return immutable snapshot list
     */
    public List<RendererRegistration> snapshot() {
        return List.copyOf(new ArrayList<>(registrations));
    }

}
