package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

public interface ErrorTargetSelector {
    Node resolve(Node control);
}