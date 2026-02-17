package org.jmouse.dom;

import org.jmouse.dom.node.CommentNode;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;

public class Example {

    public static void main(String[] args) {
        RendererFactory factory = new RendererFactory();
        NodeContext     context = new NodeContext(factory);

        ElementNode html = new ElementNode(TagName.HTML);
        ElementNode body = new ElementNode(TagName.BODY);
        ElementNode div  = new ElementNode(TagName.DIV);
        ElementNode p    = new ElementNode(TagName.P);

        div.addAttribute("class", "container");

        CommentNode comment = new CommentNode("This is a comment");

        TextNode text = new TextNode("Hello, World!");

        div.append(p);
        p.append(text);
        body.append(div);
        body.append(comment);
        html.append(body);

        html.execute(new CommentInfoCorrector());

        // after finish build html-tree reorder depth
        html.execute(node -> node.setDepth(node.hasParent() ? node.getParent().getDepth() + 1 : 0));

        html.execute(System.out::println);

        String output = html.interpret(context);
        System.out.println(output);
    }

}
