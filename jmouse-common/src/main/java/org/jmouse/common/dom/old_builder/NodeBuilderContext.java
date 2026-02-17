package org.jmouse.common.dom.old_builder;

import org.jmouse.core.scope.AbstractAttributesContext;
import org.jmouse.common.dom.PostDataProvider;

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
