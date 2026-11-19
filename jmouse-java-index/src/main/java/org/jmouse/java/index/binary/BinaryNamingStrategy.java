package org.jmouse.java.index.binary;

import org.jmouse.java.bytecode.node.attribute.JavaInnerClassNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** A binary name is not split at dollar signs: nesting comes from InnerClasses metadata. */
@FunctionalInterface
public interface BinaryNamingStrategy {
    String qualifiedName(String internalName, BinaryProjectionContext context);

    static BinaryNamingStrategy metadata() {
        return (internalName, context) -> {
            Deque<String> segments = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            String current = internalName;
            long characters = 0;
            while (true) {
                context.step();
                if (!visited.add(current)) {
                    throw new IllegalArgumentException(
                            "Cyclic InnerClasses naming metadata: " + current);
                }
                JavaInnerClassNode entry = context.innerClass(current).orElse(null);
                if (entry == null || entry.outerClassIndex() == 0 || entry.innerNameIndex() == 0) {
                    break;
                }
                String name = context.classFile().pool().utf8(entry.innerNameIndex());
                characters += name.length() + BinaryProjectionNames.NESTED_SEPARATOR.length();
                context.requireNameCapacity(characters);
                segments.addFirst(name);
                current = context.classFile().pool().className(entry.outerClassIndex());
            }
            context.requireNameCapacity(characters + current.length());
            StringBuilder name = new StringBuilder(current.replace('/', '.'));
            for (String segment : segments) {
                name.append(BinaryProjectionNames.NESTED_SEPARATOR).append(segment);
            }
            return name.toString();
        };
    }
}
