package org.jmouse.el.rendering;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.List;

public interface Template {

    void evaluate(Content content, EvaluationContext context);

    EvaluationContext createContext();

    TokenizableSource getSource();

    String getName();

    void setBlock(Block block);

    Block getBlock(String name);

    List<Block> getBlocks();

    TemplateStack getStack();

    void setParent(String parent);

    default void setParent(Template parent) {
        getStack().inherit(parent);
    }

    default Template getParent() {
        return getStack().getParent();
    }

    default boolean hasParent() {
        return getParent() != null;
    }

}
