package svit.dom.builder;

import svit.context.AbstractAttributesContext;
import svit.dom.PostDataProvider;

public class NodeBuilderContext extends AbstractAttributesContext {

    public PostDataProvider getDataProvider() {
        return this.requireAttribute(PostDataProvider.class);
    }

    public void setDataProvider(PostDataProvider dataProvider) {
        setAttribute(PostDataProvider.class, dataProvider);
    }

    public NodeBuilderRegistry getRegistry() {
        return this.requireAttribute(NodeBuilderRegistry.class);
    }

    public void setRegistry(NodeBuilderRegistry registry) {
        this.setAttribute(NodeBuilderRegistry.class, registry);
    }

}
