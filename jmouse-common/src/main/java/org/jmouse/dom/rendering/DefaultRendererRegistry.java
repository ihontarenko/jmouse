package org.jmouse.dom.rendering;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeContext;
import org.jmouse.dom.Renderer;

import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Default {@link RendererRegistry} implementation.
 *
 * <p>Thread-safety: registrations are stored in a {@link CopyOnWriteArrayList}.
 * Sorting happens after each registration.</p>
 */
public final class DefaultRendererRegistry implements RendererRegistry {

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

        throw new IllegalStateException("No renderer matches node: nodeType=" + node.getNodeType() + ", tagName=" + node.getTagName());
    }
}
