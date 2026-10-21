package org.jmouse.sheet.node;

import org.jmouse.sheet.SheetSource;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.Optional;

/** {@code id ["title"] [source …] { … }} — a block; its statements are its fields and the entries inside it. */
public class EntryNode extends ExpressionsNode {

    private String id;
    private String title;
    private SheetSource source;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Optional<SheetSource> getSource() {
        return Optional.ofNullable(source);
    }

    public void setSource(SheetSource source) {
        this.source = source;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "%s { … }".formatted(id);
    }
}
