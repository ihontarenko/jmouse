package org.jmouse.el.rendering;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.List;
import java.util.Map;

public interface Template {

    void renderBlock(String name, Content content, EvaluationContext context);

    void renderBlock(String name, Content content);

    Content renderBlock(String name);

    void render(Content content, EvaluationContext context);

    void render(Content content, Map<String, Object> values);

    Content render(Map<String, Object> values);

    Content render(EvaluationContext context);

    EvaluationContext createContext();

    TokenizableSource getSource();

    String getName();

    void setMacro(Macro macro);

    Macro getMacro(String name);

    void setBlock(Block block);

    Block getBlock(String name);

    Block getBlock(String name, EvaluationContext context);

    List<Block> getBlocks();

    void setParent(String parent, EvaluationContext context);

    void setParent(Template parent, EvaluationContext context);

}
