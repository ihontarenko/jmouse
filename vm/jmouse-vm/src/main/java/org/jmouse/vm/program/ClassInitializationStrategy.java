package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaTypeKey;

@FunctionalInterface
public interface ClassInitializationStrategy {

    ClassInitializationPlan plan(JavaTypeKey type);
}
