package org.jmouse.el.node;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructuredNodeTest {

    private static final class Branch extends StructuredNode {
        private static final ChildRole<Branch> HEAD = ChildRole.required("head", Branch.class);
        private static final ChildRole<Branch> TAIL = ChildRole.list("tail", Branch.class);
        private static final NodeSchema<Branch> SCHEMA = NodeSchema.of(
                new NodeKind<>("test.branch", Branch.class), HEAD, TAIL);
        private final String name;

        Branch(String name) {
            this(name, SCHEMA);
        }

        Branch(String name, NodeSchema<Branch> schema) {
            super(schema);
            this.name = name;
        }

        static Branch leaf(String name) {
            return new Branch(name, NodeSchema.of(new NodeKind<>("test.leaf", Branch.class)));
        }
    }

    @Test
    void rolesAreTheOnlyStructuralStorageAndReplacementDetachesTheOldChild() {
        Branch parent = new Branch("parent");
        Branch first = Branch.leaf("first");
        Branch second = Branch.leaf("second");
        parent.setChild(Branch.HEAD, first);
        var genericView = parent.getChildren();
        parent.setChild(Branch.HEAD, second);

        assertNull(first.getParent());
        assertSame(parent, second.getParent());
        assertSame(second, parent.child(Branch.HEAD));
        assertEquals(List.of(second), genericView);
        assertThrows(UnsupportedOperationException.class, () -> genericView.clear());
        assertThrows(UnsupportedOperationException.class, () -> second.setParent(null));
        assertThrows(IllegalArgumentException.class, () -> parent.addChild(Branch.TAIL, second));
        assertThrows(IllegalArgumentException.class, () -> second.addChild(Branch.TAIL, parent));
    }

    @Test
    void freezingValidatesBeforePublishingAnyFrozenNode() {
        Branch parent = new Branch("parent");
        Branch child = new Branch("child");
        parent.setChild(Branch.HEAD, child);

        assertThrows(IllegalStateException.class, parent::freeze);
        assertEquals(NodeLifecycle.BUILDING, parent.lifecycle());
        assertEquals(NodeLifecycle.BUILDING, child.lifecycle());
        assertThrows(IllegalStateException.class, child::freeze);

        child.setChild(Branch.HEAD, Branch.leaf("leaf"));
        parent.freeze();
        assertEquals(NodeLifecycle.FROZEN, child.lifecycle());
        assertThrows(IllegalStateException.class, () -> parent.setChild(Branch.HEAD, null));
        assertThrows(IllegalStateException.class, () -> child.setTrailingTrivia(Trivia.comment("# changed")));
    }

    @Test
    void traversalIsOrderedAndSupportsSkippingAndBalancedLeaveEvents() {
        Branch root = new Branch("root");
        root.setChild(Branch.HEAD, Branch.leaf("head"));
        Branch skipped = new Branch("skipped");
        skipped.setChild(Branch.HEAD, Branch.leaf("hidden"));
        root.addChild(Branch.TAIL, skipped);
        root.freeze();
        List<String> events = new ArrayList<>();
        root.accept(new Visitor() {
            @Override
            public TraversalAction enter(Node node) {
                String name = ((Branch) node).name;
                events.add("enter:" + name);
                return name.equals("skipped") ? TraversalAction.SKIP_CHILDREN : TraversalAction.CONTINUE;
            }

            @Override
            public void leave(Node node) {
                events.add("leave:" + ((Branch) node).name);
            }
        });
        assertEquals(List.of("enter:root", "enter:head", "leave:head", "enter:skipped",
                "leave:skipped", "leave:root"), events);
    }

    @Test
    void deepTreesFreezeAndVisitWithoutUsingTheJavaCallStack() {
        Branch root = Branch.leaf("bottom");
        for (int depth = 0; depth < 10_000; depth++) {
            Branch parent = new Branch("parent");
            parent.setChild(Branch.HEAD, root);
            root = parent;
        }
        root.freeze();
        int[] visited = {0};
        root.accept(new Visitor() {
            @Override
            public void visit(Node node) {
                visited[0]++;
            }
        });
        assertEquals(10_001, visited[0]);
    }
}
