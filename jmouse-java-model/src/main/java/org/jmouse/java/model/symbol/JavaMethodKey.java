package org.jmouse.java.model.symbol;

import org.jmouse.java.model.descriptor.JavaMethodDescriptor;

import java.util.Objects;

/**
 * Declaration identity using the erased language signature. Constructor parameters supplied by the
 * JVM ABI are represented separately by {@link JavaExecutableAbi}.
 */
public record JavaMethodKey(JavaTypeKey owner, String name, JavaMethodDescriptor descriptor)
        implements JavaSymbolKey {
    public JavaMethodKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(descriptor, "descriptor");
        if (Objects.requireNonNull(name, "name").isEmpty()) {
            throw new IllegalArgumentException("A method requires a name");
        }
    }

    @Override
    public String displayName() {
        return owner.displayName() + "#" + name + descriptor.encoding();
    }
}
