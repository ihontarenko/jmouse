package org.jmouse.java.analysis;

import java.util.List;

public record JavaScopeLayer(List<JavaVisibleBinding> bindings) {
    public JavaScopeLayer {
        bindings = List.copyOf(bindings);
    }
}
