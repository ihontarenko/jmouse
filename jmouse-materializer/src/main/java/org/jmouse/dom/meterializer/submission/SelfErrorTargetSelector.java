package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

public final class SelfErrorTargetSelector implements ErrorTargetSelector {

    @Override
    public Node resolve(Node control) {
        return control;
    }

}