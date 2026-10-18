package org.jmouse.java.index.binary;

import org.jmouse.java.parser.model.Modifier;
import org.objectweb.asm.Opcodes;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Context-specific access flags: the same JVM bit can mean different things on a field and a
 * method.
 */
final class JvmModifiers {

    private static final Map<Integer, Modifier> COMMON =
            Map.of(
                    Opcodes.ACC_PUBLIC, Modifier.PUBLIC,
                    Opcodes.ACC_PROTECTED, Modifier.PROTECTED,
                    Opcodes.ACC_PRIVATE, Modifier.PRIVATE,
                    Opcodes.ACC_STATIC, Modifier.STATIC,
                    Opcodes.ACC_FINAL, Modifier.FINAL);

    private JvmModifiers() {}

    static Set<Modifier> type(int access) {
        return read(access, Map.of(Opcodes.ACC_ABSTRACT, Modifier.ABSTRACT));
    }

    static Set<Modifier> field(int access) {
        return read(
                access,
                Map.of(
                        Opcodes.ACC_VOLATILE,
                        Modifier.VOLATILE,
                        Opcodes.ACC_TRANSIENT,
                        Modifier.TRANSIENT));
    }

    static Set<Modifier> method(int access) {
        return read(
                access,
                Map.of(
                        Opcodes.ACC_ABSTRACT, Modifier.ABSTRACT,
                        Opcodes.ACC_SYNCHRONIZED, Modifier.SYNCHRONIZED,
                        Opcodes.ACC_NATIVE, Modifier.NATIVE,
                        Opcodes.ACC_STRICT, Modifier.STRICTFP));
    }

    private static Set<Modifier> read(int access, Map<Integer, Modifier> specific) {
        Set<Modifier> result = EnumSet.noneOf(Modifier.class);
        Map<Integer, Modifier> flags = new LinkedHashMap<>(COMMON);
        flags.putAll(specific);
        flags.forEach(
                (flag, modifier) -> {
                    if ((access & flag) != 0) {
                        result.add(modifier);
                    }
                });
        return Set.copyOf(result);
    }
}
