package org.jmouse.common.dom;

public class NodeContext {

    public static final ReorderNodeCorrector  REORDER_NODE_CORRECTOR;
    public static final CommentInfoCorrector  COMMENT_INFO_CORRECTOR;
    public static final NbspReplacerCorrector NBSP_REPLACER_CORRECTOR;

    static {
        REORDER_NODE_CORRECTOR = new ReorderNodeCorrector();
        COMMENT_INFO_CORRECTOR = new CommentInfoCorrector();
        NBSP_REPLACER_CORRECTOR = new NbspReplacerCorrector();
    }

    private final RendererFactory rendererFactory;

    public NodeContext(RendererFactory rendererFactory) {
        this.rendererFactory = rendererFactory;
    }

    public RendererFactory getRendererFactory() {
        return rendererFactory;
    }

}
