package org.jmouse.grabber.document;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * 🧩 One thing on a page.
 *
 * <p>The same selection methods as {@link PageDocument}, scoped to this element's subtree — which is
 * what makes "for each product card, read its own title and its own price" the obvious thing to
 * write rather than an exercise in index arithmetic.</p>
 */
public interface PageElement {

    Optional<PageElement> select(String selector);

    List<PageElement> selectAll(String selector);

    /**
     * 🔤 All the text in this element and everything under it, whitespace collapsed.
     */
    String text();

    /**
     * 🔤 The text of this element alone, ignoring its children.
     *
     * <p>Worth having: a price element whose currency symbol is a child {@code span} reads very
     * differently through this than through {@link #text()}.</p>
     */
    String ownText();

    String html();

    /**
     * 🏷️ An attribute, or empty when it is absent or blank.
     */
    Optional<String> attribute(String name);

    /**
     * 🔗 An attribute read as an absolute address.
     *
     * <p>⚠️ <b>This is why the seam exists.</b> Resolution happens here, once, against the response's
     * final address — a handler that has to remember to resolve is a handler that forgets on one page
     * in ten, and the pages it then queues 404 for reasons that look like the site's fault.</p>
     */
    Optional<URI> link(String attributeName);

    /**
     * 🔗 The {@code href} of this element, absolute.
     */
    default Optional<URI> link() {
        return link("href");
    }

    /**
     * 🔤 The text of the first match of this selector inside this element.
     */
    default Optional<String> text(String selector) {
        return select(selector).map(PageElement::text).filter(text -> !text.isBlank());
    }

    /**
     * 🔤 The text of every match of this selector inside this element.
     */
    default List<String> texts(String selector) {
        return selectAll(selector).stream().map(PageElement::text).filter(text -> !text.isBlank()).toList();
    }

    /**
     * 🏷️ An attribute of the first match of this selector inside this element.
     */
    default Optional<String> attribute(String selector, String name) {
        return select(selector).flatMap(element -> element.attribute(name));
    }

    /**
     * 🧬 The element's tag name, lower-cased.
     */
    String tag();

    boolean has(String selector);

}
