package org.jmouse.el.renderable;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.lexer.TokenizableSource;
import org.jmouse.el.core.node.Node;

public interface Template {

    EvaluationContext newContext();

    TokenizableSource getSource();

    Node getRoot();

    String getName();

    void setMacro(Macro macro);

    Macro getMacro(String name);

    void setBlock(Block block);

    Block getBlock(String name);

    Block getBlock(String name, EvaluationContext context);

    Template getParent(EvaluationContext context);

    void setParent(String parent, EvaluationContext context);

    void setParent(Template parent, EvaluationContext context);

    TemplateRegistry getRegistry();

}
