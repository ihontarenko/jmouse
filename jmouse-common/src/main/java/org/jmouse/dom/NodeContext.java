package org.jmouse.dom;

import org.jmouse.core.SingletonSupplier;

import java.util.function.Supplier;

public record NodeContext(RendererFactory rendererFactory) {

    public static final CommentInfoCorrector  COMMENT_INFO_CORRECTOR;
    public static final NbspReplacerCorrector NBSP_REPLACER_CORRECTOR;
    public static final CorrectNodeDepth      CORRECT_NODE_DEPTH;
    public static final Supplier<NodeContext> NODE_CONTEXT_SUPPLIER;

    static {
        CORRECT_NODE_DEPTH = new CorrectNodeDepth();
        COMMENT_INFO_CORRECTOR = new CommentInfoCorrector();
        NBSP_REPLACER_CORRECTOR = new NbspReplacerCorrector();
        NODE_CONTEXT_SUPPLIER = SingletonSupplier.of(() -> new NodeContext(new RendererFactory()));
    }

    public static NodeContext defaults() {
        return NODE_CONTEXT_SUPPLIER.get();
    }

}
