package org.jmouse.el.translate;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.node.StructuredNode;
import org.junit.jupiter.api.Test;

class NodeOperationRegistryTest {
    private static final class Leaf extends StructuredNode {
        private static final NodeKind<Leaf> KIND = new NodeKind<>("test.leaf", Leaf.class);

        private Leaf() {
            super(NodeSchema.of(KIND));
        }
    }

    @Test
    void extensionSnapshotsReplaceExactHandlersWithoutChangingAnExistingConsumer() {
        NodeOperationRegistry<String, String> registry = new NodeOperationRegistry<>("test.render");
        Leaf leaf = new Leaf();
        assertThrows(NodeOperationBindingException.class, () -> registry.apply(leaf, "source"));
        registry.bind(Leaf.KIND, "test.original", (node, context) -> context + ":original");
        NodeOperationRegistry<String, String> baseline = registry.snapshot();
        NodeOperationRegistry<String, String> extension = baseline.fork();
        extension.replace(
                Leaf.KIND,
                "test.original",
                "test.extension",
                (node, context) -> context + ":extended");
        assertEquals("source:original", baseline.apply(leaf, "source"));
        assertEquals("source:extended", extension.snapshot().apply(leaf, "source"));
        assertEquals("test.extension", extension.explain(Leaf.KIND).implementation());
        assertThrows(
                IllegalStateException.class,
                () ->
                        baseline.replace(
                                Leaf.KIND,
                                "test.original",
                                "test.other",
                                (node, context) -> context));
        assertThrows(
                IllegalArgumentException.class,
                () -> extension.bind(Leaf.KIND, "test.duplicate", (node, context) -> context));
    }
}
