package org.jmouse.grabber.document;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * 📄 A page, parsed.
 *
 * <p>The module hands out this rather than a {@code org.jsoup.nodes.Document} on purpose: a seam here
 * is what lets an XML feed, a JSON payload or a browser-rendered page arrive through the same handler
 * without the handler knowing which.</p>
 */
public interface PageDocument {

    /**
     * 🌐 The address this document was read from — the FINAL one, after redirects. Every link on the
     * page is resolved against it.
     */
    URI address();

    Optional<PageElement> select(String selector);

    List<PageElement> selectAll(String selector);

    /**
     * 🌳 The document as one element, for the extraction that wants a root to work from.
     */
    PageElement root();

    /**
     * 🏷️ The page's title, or empty when it has none.
     */
    Optional<String> title();

    String html();

    /**
     * 🔗 Every address this selector's matches point at, absolute and deduplicated in page order.
     *
     * <p>The one call a following handler makes, and the reason it is here rather than in each
     * handler: resolution, the {@code href} versus {@code src} question, and dropping the anchors
     * that are not addresses at all are three mistakes nobody should make twice.</p>
     */
    List<URI> links(String selector);

    /**
     * 🔗 Every link on the page — {@code a[href]}.
     */
    default List<URI> links() {
        return links("a[href]");
    }

    /**
     * 🔤 The text of the first match of this selector.
     */
    default Optional<String> text(String selector) {
        return select(selector).map(PageElement::text).filter(text -> !text.isBlank());
    }

    /**
     * 🔤 The text of every match of this selector.
     */
    default List<String> texts(String selector) {
        return selectAll(selector).stream().map(PageElement::text).filter(text -> !text.isBlank()).toList();
    }

    /**
     * 🏷️ An attribute of the first match of this selector.
     */
    default Optional<String> attribute(String selector, String name) {
        return select(selector).flatMap(element -> element.attribute(name));
    }

    default boolean has(String selector) {
        return select(selector).isPresent();
    }

}
