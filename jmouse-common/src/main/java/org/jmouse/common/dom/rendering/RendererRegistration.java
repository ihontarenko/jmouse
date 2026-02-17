package org.jmouse.common.dom.rendering;

import org.jmouse.core.Verify;
import org.jmouse.common.dom.Node;
import org.jmouse.common.dom.Renderer;

import java.util.function.Predicate;

/**
 * Single renderer registration entry.
 *
 * @param predicate match rule for nodes
 * @param order higher values win over lower values
 * @param renderer renderer to use when predicate matches
 */
public record RendererRegistration(Predicate<? super Node> predicate, int order, Renderer renderer) {

    public RendererRegistration {
        Verify.nonNull(predicate, "predicate");
        Verify.nonNull(renderer, "renderer");
    }

}
