package org.jmouse.el.rendering;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.List;
import java.util.Map;

public interface Template {

    void evaluate(Content content, EvaluationContext context);

    void evaluate(Content content, Map<String, Object> values);

    Content evaluate(EvaluationContext context);

    EvaluationContext createContext();

    TokenizableSource getSource();

    String getName();

    void setBlock(Block block);

    Block getBlock(String name);

    List<Block> getBlocks();

    void setParent(String parent, EvaluationContext context);

    void setParent(Template parent, EvaluationContext context);

}
