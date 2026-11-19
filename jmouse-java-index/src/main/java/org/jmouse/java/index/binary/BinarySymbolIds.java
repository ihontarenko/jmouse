package org.jmouse.java.index.binary;

import org.jmouse.java.model.descriptor.JavaArrayDescriptor;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.java.parser.model.SymbolId;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Navigation identities remain distinct from executable/ABI symbol identities. */
final class BinarySymbolIds {
    private BinarySymbolIds() {}

    static SymbolId type(String qualifiedName) {
        String name =
                Arrays.stream(qualifiedName.split("\\.", -1))
                        .map(BinarySymbolIds::segment)
                        .collect(Collectors.joining(BinaryProjectionNames.NESTED_SEPARATOR));
        return new SymbolId(name.isBlank() ? BinaryProjectionNames.BINARY_ID_PREFIX + name : name);
    }

    static String segment(String name) {
        if (!name.isEmpty()
                && Character.isJavaIdentifierStart(name.codePointAt(0))
                && name.codePoints().allMatch(Character::isJavaIdentifierPart)) {
            return name;
        }
        return URLEncoder.encode(name, StandardCharsets.UTF_8);
    }

    static String descriptorName(JavaTypeDescriptor descriptor, BinaryProjectionContext context) {
        return switch (descriptor) {
            case JavaClassDescriptor type -> context.qualifiedName(type.internalName());
            case JavaArrayDescriptor array ->
                    descriptorName(array.element(), context)
                            + BinaryProjectionNames.ARRAY_SUFFIX.repeat(array.dimensions());
            case JavaType type -> type.displayName();
            default ->
                    throw new IllegalArgumentException(
                            "No navigation name projection for " + descriptor);
        };
    }
}
