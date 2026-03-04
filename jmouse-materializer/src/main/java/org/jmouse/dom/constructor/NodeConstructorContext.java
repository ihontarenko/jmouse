package org.jmouse.dom.constructor;

import org.jmouse.core.scope.AbstractAttributesContext;
import org.jmouse.meterializer.SubmissionState;

public class NodeConstructorContext extends AbstractAttributesContext {

    public SubmissionState getDataProvider() {
        return this.requireAttribute(SubmissionState.class);
    }

    public void setDataProvider(SubmissionState dataProvider) {
        setAttribute(SubmissionState.class, dataProvider);
    }

    public NodeConstructorRegistry getRegistry() {
        return this.requireAttribute(NodeConstructorRegistry.class);
    }

    public void setRegistry(NodeConstructorRegistry registry) {
        this.setAttribute(NodeConstructorRegistry.class, registry);
    }

}
