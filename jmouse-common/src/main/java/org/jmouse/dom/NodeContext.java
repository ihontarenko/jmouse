package org.jmouse.dom;

import org.jmouse.core.Verify;
import org.jmouse.dom.rendering.RendererRegistry;

/**
 * Rendering context that holds shared output and renderer resolution.
 */
public record NodeContext(StringBuilder output, RendererRegistry registry) {

    public NodeContext(StringBuilder output, RendererRegistry registry) {
        this.output = Verify.nonNull(output, "output");
        this.registry = Verify.nonNull(registry, "registry");
    }

}
