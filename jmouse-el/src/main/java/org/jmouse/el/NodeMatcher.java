package org.jmouse.el;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.TypeMatchers;
import org.jmouse.el.node.Node;

import java.util.Arrays;

public class NodeMatcher {

    @SafeVarargs
    public static Matcher<Node> types(Class<? extends Node>... types) {
        return new NodeTypeMatcher(types);
    }

    record NodeTypeMatcher(Class<? extends Node>... types) implements Matcher<Node> {
        @SafeVarargs
        public NodeTypeMatcher {
        }

        @Override
        public boolean matches(Node item) {
            boolean           matches = false;
            Matcher<Class<?>> matcher = TypeMatchers.isSupertype(item.getClass());

            for (Class<? extends Node> type : types) {
                matches |= matcher.matches(type);
            }

            return matches;
        }

        @Override
        public String toString() {
            return "NODE_TYPES: " + Arrays.toString(Arrays.stream(types).map(Class::getSimpleName).toArray());
        }
    }

}
