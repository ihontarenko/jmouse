package org.jmouse.query.el.node;

import org.jmouse.query.el.SourceWriter;

/**
 * {@code view "name" on target { … }} — a named, stored query.
 *
 * <p>⚠️ <strong>The target is an opaque identifier and is resolved by nobody here.</strong> Whether
 * {@code on inventory} names a section, a purpose, or both is a question each product answers about
 * its own data, and a language that answered it would be a language that could only serve one product.
 * The parser reads the word and hands it on.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ViewNode extends QueryBlockNode {

    private static final String INDENT = "  ";

    private String title;
    private String target;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public String toSource() {
        String body = clausesToSource(INDENT);

        if (body.isEmpty()) {
            return "view %s on %s { }".formatted(SourceWriter.literal(title), SourceWriter.name(target));
        }

        return "view %s on %s {\n%s\n}".formatted(
                SourceWriter.literal(title), SourceWriter.name(target), body);
    }

    @Override
    public String toString() {
        return "view '%s' on %s".formatted(title, target);
    }
}
