package org.jmouse.dom;

import java.util.function.Consumer;

public interface Corrector extends Consumer<Node> {
    default void correct(Node node) {
        accept(node);
    }
}
