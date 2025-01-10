package svit.dom;

import svit.dom.node.TextNode;

public class NbspReplacerCorrector implements Corrector{

    @Override
    public void accept(Node node) {
       if (node instanceof TextNode textNode) {
           textNode.setText(textNode.getText().replaceAll("\s+", "&nbsp;"));
       }
    }

}
