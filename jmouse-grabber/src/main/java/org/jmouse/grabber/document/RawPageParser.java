package org.jmouse.grabber.document;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.jmouse.core.MediaType;
import org.jmouse.grabber.fetch.PageResponse;

/**
 * 📦 Claims named media types and parses nothing.
 *
 * <p>For the route that reads the body itself — a JSON API, a file to write to disk. Everything about
 * the response is still reachable through the {@link org.jmouse.grabber.PageContext}; only the
 * document is empty, because there is nothing to build one from.</p>
 */
final class RawPageParser implements PageParser {

    private final List<MediaType> claimed;

    RawPageParser(List<MediaType> claimed) {
        if (claimed.isEmpty()) {
            throw new IllegalArgumentException("A raw parser must name the media types it claims");
        }

        this.claimed = claimed;
    }

    @Override
    public boolean supports(MediaType contentType) {
        if (contentType == null) {
            return false;
        }

        return claimed.stream().anyMatch(type -> type.includes(contentType));
    }

    @Override
    public PageDocument parse(PageResponse response) {
        return new RawPageDocument(response);
    }

    /**
     * 📄 A document over a body nothing parsed.
     *
     * <p>Every selection answers empty rather than throwing: a handler asking a raw page for a
     * selector has made a mistake, and an empty answer is the same one it would get from a page that
     * genuinely lacked the element. Throwing would turn a wrong route into a dead run.</p>
     */
    private record RawPageDocument(PageResponse response) implements PageDocument {

        @Override
        public URI address() {
            return response.address();
        }

        @Override
        public Optional<PageElement> select(String selector) {
            return Optional.empty();
        }

        @Override
        public List<PageElement> selectAll(String selector) {
            return List.of();
        }

        @Override
        public PageElement root() {
            throw new UnsupportedOperationException(
                    "A raw page has no elements — read it with rawText() or bytes()");
        }

        @Override
        public Optional<String> title() {
            return Optional.empty();
        }

        @Override
        public String html() {
            return response.text();
        }

        @Override
        public List<URI> links(String selector) {
            return List.of();
        }
    }

}
