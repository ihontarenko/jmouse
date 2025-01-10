package svit.dom;

import svit.dom.node.CommentNode;

public class CommentInfoCorrector implements Corrector{

    @Override
    public void accept(Node node) {
        node.insertBefore(new CommentNode("OPEN: [%s]".formatted(node)));
        node.insertAfter(new CommentNode("CLOSE: [%s]".formatted(node)));
    }

}
