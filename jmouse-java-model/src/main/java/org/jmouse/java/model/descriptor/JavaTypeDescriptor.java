package org.jmouse.java.model.descriptor;

/** An erased JVM type; constructing this model never parses source text. */
public interface JavaTypeDescriptor {
    String encoding();

    default int parameterSlots() {
        return 1;
    }
}
