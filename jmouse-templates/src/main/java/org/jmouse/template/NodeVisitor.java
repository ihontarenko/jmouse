package org.jmouse.template;

import org.jmouse.el.node.Node;
import org.jmouse.template.node.BlockNode;
import org.jmouse.template.node.MacroNode;
import org.jmouse.template.node.RawTextNode;

public interface NodeVisitor {

    void visit(Node embedNode);

    void visit(RawTextNode textNode);

    void visit(BlockNode blockNode);

    void visit(MacroNode macroNode);

}
