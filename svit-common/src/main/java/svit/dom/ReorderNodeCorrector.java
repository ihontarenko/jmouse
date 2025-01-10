package svit.dom;

public class ReorderNodeCorrector implements Corrector {

    @Override
    public void accept(Node node) {
        node.setDepth(node.hasParent() ? node.getParent().getDepth() + 1 : 0);
    }

}
