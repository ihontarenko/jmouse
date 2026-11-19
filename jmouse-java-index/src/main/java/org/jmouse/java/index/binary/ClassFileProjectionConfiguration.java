package org.jmouse.java.index.binary;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.java.bytecode.node.JavaMemberNode;

import java.util.Objects;

public record ClassFileProjectionConfiguration(
        SourceLayout layout,
        BinaryProjectionLimits limits,
        Matcher<? super JavaMemberNode> members,
        BinaryNamingStrategy naming,
        NodeOperationRegistry<BinaryTypeProjectionContext, BinaryProjectionOutcome> types) {
    public ClassFileProjectionConfiguration {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(members, "members");
        Objects.requireNonNull(naming, "naming");
        types = Objects.requireNonNull(types, "types").snapshot();
    }

    public static ClassFileProjectionConfiguration standard() {
        return new ClassFileProjectionConfiguration(
                SourceLayout.WORDS,
                BinaryProjectionLimits.standard(),
                Matcher.constant(true),
                BinaryNamingStrategy.metadata(),
                BinaryTypeProjections.standard());
    }

    public ClassFileProjectionConfiguration withMembers(Matcher<? super JavaMemberNode> matcher) {
        return new ClassFileProjectionConfiguration(layout, limits, matcher, naming, types);
    }

    public ClassFileProjectionConfiguration withTypes(
            NodeOperationRegistry<BinaryTypeProjectionContext, BinaryProjectionOutcome>
                    operations) {
        return new ClassFileProjectionConfiguration(layout, limits, members, naming, operations);
    }

    public ClassFileProjectionConfiguration withLimits(BinaryProjectionLimits replacement) {
        return new ClassFileProjectionConfiguration(layout, replacement, members, naming, types);
    }

    public ClassFileProjectionConfiguration withNaming(BinaryNamingStrategy strategy) {
        return new ClassFileProjectionConfiguration(layout, limits, members, strategy, types);
    }
}
