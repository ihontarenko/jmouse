package org.jmouse.grabber.document;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 📄 A Jsoup document behind {@link PageDocument}.
 */
final class JsoupPageDocument implements PageDocument {

    private final Document document;
    private final URI      address;

    JsoupPageDocument(Document document, URI address) {
        this.document = document;
        this.address = address;
    }

    @Override
    public URI address() {
        return address;
    }

    @Override
    public Optional<PageElement> select(String selector) {
        return Optional.ofNullable(document.selectFirst(selector)).map(JsoupPageElement::new);
    }

    @Override
    public List<PageElement> selectAll(String selector) {
        return JsoupPageElement.wrap(document.select(selector));
    }

    @Override
    public PageElement root() {
        return new JsoupPageElement(document);
    }

    @Override
    public Optional<String> title() {
        String title = document.title();
        return title.isBlank() ? Optional.empty() : Optional.of(title);
    }

    @Override
    public String html() {
        return document.outerHtml();
    }

    @Override
    public List<URI> links(String selector) {
        Elements  matches = document.select(selector);
        Set<URI>  found   = new LinkedHashSet<>();

        for (Element element : matches) {
            JsoupPageElement.absolute(element).ifPresent(found::add);
        }

        return List.copyOf(found);
    }

    /**
     * 🔗 Absolute-address helpers shared by the document and the element.
     */
    static Optional<URI> toAbsolute(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String trimmed = value.trim();

        // A fragment-only or scheme-only anchor is not an address to visit. Jsoup resolves the first
        // to the page itself, which would queue every page again under a slightly different spelling.
        if (trimmed.startsWith("#")
                || trimmed.startsWith("javascript:")
                || trimmed.startsWith("mailto:")
                || trimmed.startsWith("tel:")
                || trimmed.startsWith("data:")) {
            return Optional.empty();
        }

        try {
            return Optional.of(new URI(trimmed));
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
    }

}
