package org.jmouse.java.model.symbol;

import org.jmouse.java.model.descriptor.JavaTypeDescriptor;
import org.jmouse.java.model.type.JavaVoidType;

import java.util.Objects;

public record JavaFieldKey(JavaTypeKey owner, String name, JavaTypeDescriptor descriptor)
        implements JavaSymbolKey {
    public JavaFieldKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(descriptor, "descriptor");
        if (name.isEmpty() || descriptor == JavaVoidType.VOID) {
            throw new IllegalArgumentException("A field requires a name and a non-void descriptor");
        }
    }

    @Override
    public String displayName() {
        return owner.displayName() + "#" + name + ":" + descriptor.encoding();
    }
}
