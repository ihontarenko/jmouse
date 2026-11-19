package org.jmouse.java.index.binary;

import org.jmouse.java.bytecode.vocabulary.JvmClassAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmFieldAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmMethodAccessFlag;
import org.jmouse.java.parser.model.Modifier;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

final class JvmModifiers {
    private JvmModifiers() {}

    static Set<Modifier> type(int access) {
        return read(
                access,
                Map.of(
                        JvmClassAccessFlag.PUBLIC.code(),
                        Modifier.PUBLIC,
                        JvmClassAccessFlag.FINAL.code(),
                        Modifier.FINAL,
                        JvmClassAccessFlag.ABSTRACT.code(),
                        Modifier.ABSTRACT));
    }

    static Set<Modifier> field(int access) {
        return read(
                access,
                Map.of(
                        JvmFieldAccessFlag.PUBLIC.code(), Modifier.PUBLIC,
                        JvmFieldAccessFlag.PRIVATE.code(), Modifier.PRIVATE,
                        JvmFieldAccessFlag.PROTECTED.code(), Modifier.PROTECTED,
                        JvmFieldAccessFlag.STATIC.code(), Modifier.STATIC,
                        JvmFieldAccessFlag.FINAL.code(), Modifier.FINAL,
                        JvmFieldAccessFlag.VOLATILE.code(), Modifier.VOLATILE,
                        JvmFieldAccessFlag.TRANSIENT.code(), Modifier.TRANSIENT));
    }

    static Set<Modifier> method(int access) {
        return read(
                access,
                Map.of(
                        JvmMethodAccessFlag.PUBLIC.code(), Modifier.PUBLIC,
                        JvmMethodAccessFlag.PRIVATE.code(), Modifier.PRIVATE,
                        JvmMethodAccessFlag.PROTECTED.code(), Modifier.PROTECTED,
                        JvmMethodAccessFlag.STATIC.code(), Modifier.STATIC,
                        JvmMethodAccessFlag.FINAL.code(), Modifier.FINAL,
                        JvmMethodAccessFlag.ABSTRACT.code(), Modifier.ABSTRACT,
                        JvmMethodAccessFlag.SYNCHRONIZED.code(), Modifier.SYNCHRONIZED,
                        JvmMethodAccessFlag.NATIVE.code(), Modifier.NATIVE,
                        JvmMethodAccessFlag.STRICT.code(), Modifier.STRICTFP));
    }

    private static Set<Modifier> read(int access, Map<Integer, Modifier> flags) {
        Set<Modifier> result = EnumSet.noneOf(Modifier.class);
        flags.forEach(
                (flag, modifier) -> {
                    if ((access & flag) != 0) {
                        result.add(modifier);
                    }
                });
        return Set.copyOf(result);
    }
}
