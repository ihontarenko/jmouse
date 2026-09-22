package org.jmouse.grabber.document;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 🧩 A Jsoup element behind {@link PageElement}.
 */
final class JsoupPageElement implements PageElement {

    /**
     * 🔗 The attributes an element's address lives in, in the order they are tried.
     *
     * <p>{@code href} for links, {@code src} for images and frames — asking for "the address of this
     * element" without naming the attribute is what a handler actually wants, and getting it wrong is
     * a silent empty result.</p>
     */
    private static final List<String> ADDRESS_ATTRIBUTES = List.of("href", "src", "data-src", "content");

    private final Element element;

    JsoupPageElement(Element element) {
        this.element = element;
    }

    static List<PageElement> wrap(Elements elements) {
        List<PageElement> wrapped = new ArrayList<>(elements.size());

        for (Element element : elements) {
            wrapped.add(new JsoupPageElement(element));
        }

        return List.copyOf(wrapped);
    }

    /**
     * 🔗 This element's own address, from whichever of the usual attributes it carries.
     */
    static Optional<URI> absolute(Element element) {
        for (String attribute : ADDRESS_ATTRIBUTES) {
            if (!element.hasAttr(attribute)) {
                continue;
            }

            Optional<URI> address = resolve(element, attribute);

            if (address.isPresent()) {
                return address;
            }
        }

        return Optional.empty();
    }

    private static Optional<URI> resolve(Element element, String attributeName) {
        // The raw value decides whether this is an address at all; absUrl decides what it points to.
        if (JsoupPageDocument.toAbsolute(element.attr(attributeName)).isEmpty()) {
            return Optional.empty();
        }

        return JsoupPageDocument.toAbsolute(element.absUrl(attributeName));
    }

    @Override
    public Optional<PageElement> select(String selector) {
        return Optional.ofNullable(element.selectFirst(selector)).map(JsoupPageElement::new);
    }

    @Override
    public List<PageElement> selectAll(String selector) {
        return wrap(element.select(selector));
    }

    @Override
    public String text() {
        return element.text();
    }

    @Override
    public String ownText() {
        return element.ownText();
    }

    @Override
    public String html() {
        return element.html();
    }

    @Override
    public Optional<String> attribute(String name) {
        if (!element.hasAttr(name)) {
            return Optional.empty();
        }

        String value = element.attr(name);

        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    @Override
    public Optional<URI> link(String attributeName) {
        return resolve(element, attributeName);
    }

    @Override
    public String tag() {
        return element.tagName().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean has(String selector) {
        return element.selectFirst(selector) != null;
    }

    @Override
    public String toString() {
        return "<" + tag() + "> " + text();
    }

}
