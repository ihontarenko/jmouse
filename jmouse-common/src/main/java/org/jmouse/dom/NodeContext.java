package org.jmouse.dom;

import org.jmouse.core.SingletonSupplier;

import java.util.function.Supplier;

public record NodeContext(RendererFactory rendererFactory) {

    public static final ReorderNodeCorrector  REORDER_NODE_CORRECTOR;
    public static final CommentInfoCorrector  COMMENT_INFO_CORRECTOR;
    public static final NbspReplacerCorrector NBSP_REPLACER_CORRECTOR;
    public static final Supplier<NodeContext> NODE_CONTEXT_SUPPLIER;

    static {
        REORDER_NODE_CORRECTOR = new ReorderNodeCorrector();
        COMMENT_INFO_CORRECTOR = new CommentInfoCorrector();
        NBSP_REPLACER_CORRECTOR = new NbspReplacerCorrector();
        NODE_CONTEXT_SUPPLIER = SingletonSupplier.of(() -> new NodeContext(new RendererFactory()));
    }

    public static NodeContext defaults() {
        return NODE_CONTEXT_SUPPLIER.get();
    }

}
