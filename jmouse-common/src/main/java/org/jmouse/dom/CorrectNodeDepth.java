package org.jmouse.dom;

public class CorrectNodeDepth implements Corrector {

    @Override
    public void accept(Node node) {
        node.setDepth(node.hasParent() ? node.getParent().getDepth() + 1 : 0);
    }

}
