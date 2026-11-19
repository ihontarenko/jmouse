package org.jmouse.java.parser.projection;

import org.jmouse.el.translate.Capability;

/** Public identities of the source-model views. */
public final class JavaProjectionCapabilities {
    public static final Capability DECLARATION_METADATA =
            new Capability("java.declaration.metadata");
    public static final Capability EXECUTABLE_METADATA = new Capability("java.executable.metadata");

    private JavaProjectionCapabilities() {}
}
