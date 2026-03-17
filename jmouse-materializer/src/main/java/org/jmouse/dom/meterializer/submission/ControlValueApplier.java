package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

public interface ControlValueApplier {

    boolean supports(Node node);

    void apply(Node node, Object value);

}