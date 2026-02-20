package org.jmouse.xml;

import java.util.Map;

@FunctionalInterface
public interface NamespaceConfig {

    String getNamespaceFor(String prefix);

    static NamespaceConfig of(Map<String, String> mapping) {
        return mapping::get;
    }

}