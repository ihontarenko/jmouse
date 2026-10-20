package org.jmouse.datamap.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;

/** {@code map "Title" as slug { … }} — the document; its statements are the map's fields and entries. */
public class DataMapDocumentNode extends ExpressionsNode {

    private String title;
    private String slug;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "map \"%s\" as %s { … }".formatted(title, slug);
    }
}
