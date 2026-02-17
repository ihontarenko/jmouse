package org.jmouse.dom.rendering;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.Renderer;

import java.util.function.Predicate;

/**
 * Single registry entry that binds a predicate to a renderer with an order.
 *
 * @param predicate matching rule
 * @param order higher value wins
 * @param renderer renderer instance
 */
public record RendererRegistration(Predicate<? super Node> predicate, int order, Renderer renderer) {

    public RendererRegistration {
        Verify.nonNull(predicate, "predicate");
        Verify.nonNull(renderer, "renderer");
    }
}
