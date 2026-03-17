package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

public interface FieldKeyResolver {

    String resolveValueKey(Node node);

    String resolveErrorKey(Node node);

}