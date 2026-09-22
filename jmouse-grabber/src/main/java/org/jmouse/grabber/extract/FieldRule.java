package org.jmouse.grabber.extract;

import java.util.List;
import java.util.Optional;

import org.jmouse.grabber.document.PageElement;

/**
 * 🎣 How one field is pulled off a page.
 *
 * <p>A rule is built up rather than configured: {@code css("h1").text()} is a selector plus a way of
 * reading what it found, and {@code .as(BigDecimal.class)} and {@code .many()} are further steps on
 * the same chain. What comes out is whatever the last step produced — a string, a converted value, or
 * a list of either.</p>
 */
public interface FieldRule {

    /**
     * 🎣 Reads this field out of the given element.
     *
     * @return the value, or empty when the page does not have it. ⚠️ Empty rather than null, because
     *         a field that is genuinely absent and a field that is present and blank are different
     *         answers and a binder treats them differently.
     */
    Optional<Object> read(PageElement element, ExtractionSupport support);

    /**
     * 🎣 A rule that starts from a CSS selector.
     */
    static Selection css(String selector) {
        return new Selection(selector);
    }

    /**
     * 🎣 A rule reading the element the extraction is scoped to, without selecting anything.
     *
     * <p>For the field that lives on the block itself — an identifier in a {@code data-} attribute on
     * the card, rather than in something inside it.</p>
     */
    static Selection self() {
        return new Selection(null);
    }

    /**
     * 🧭 A selector, before it has been said what to read from it.
     */
    final class Selection {

        private final String selector;

        private Selection(String selector) {
            this.selector = selector;
        }

        /**
         * 🔤 The text of the match, whitespace collapsed.
         */
        public ValueRule text() {
            return new ValueRule(selector, Reading.TEXT, null, null, false);
        }

        /**
         * 🔤 The text of the match itself, ignoring its children — the price without the currency
         * symbol that lives in a nested span.
         */
        public ValueRule ownText() {
            return new ValueRule(selector, Reading.OWN_TEXT, null, null, false);
        }

        /**
         * 🏷️ An attribute of the match.
         */
        public ValueRule attribute(String name) {
            return new ValueRule(selector, Reading.ATTRIBUTE, name, null, false);
        }

        /**
         * 🔗 An attribute of the match, read as an absolute address.
         */
        public ValueRule link(String attributeName) {
            return new ValueRule(selector, Reading.LINK, attributeName, null, false);
        }

        /**
         * 🔗 The {@code href} of the match, absolute.
         */
        public ValueRule link() {
            return link("href");
        }

        /**
         * 📃 The inner HTML of the match, for the field that is markup rather than text.
         */
        public ValueRule html() {
            return new ValueRule(selector, Reading.HTML, null, null, false);
        }

        /**
         * ✅ Whether anything matched at all — a boolean field from the presence of an element.
         */
        public ValueRule present() {
            return new ValueRule(selector, Reading.PRESENT, null, null, false);
        }
    }

    /**
     * 🎣 A complete rule, which may still be converted or made repeating.
     */
    final class ValueRule implements FieldRule {

        private final String   selector;
        private final Reading  reading;
        private final String   attribute;
        private final Class<?> type;
        private final boolean  many;

        private ValueRule(String selector, Reading reading, String attribute, Class<?> type, boolean many) {
            this.selector = selector;
            this.reading = reading;
            this.attribute = attribute;
            this.type = type;
            this.many = many;
        }

        /**
         * 🔁 Converts what was read into this type.
         *
         * <p>⚠️ Through {@link org.jmouse.core.convert.Conversion} — the library's own — rather than
         * through parsing written here. A grabber that grew its own string-to-number handling has
         * re-implemented that service badly, and would then need its own date handling, and its own
         * enum handling, and so on.</p>
         */
        public ValueRule as(Class<?> targetType) {
            return new ValueRule(selector, reading, attribute, targetType, many);
        }

        /**
         * 🔢 Every match rather than the first, as a list.
         */
        public ValueRule many() {
            return new ValueRule(selector, reading, attribute, type, true);
        }

        @Override
        public Optional<Object> read(PageElement element, ExtractionSupport support) {
            if (reading == Reading.PRESENT) {
                return Optional.of(selector == null || element.has(selector));
            }

            if (many) {
                List<Object> values = matches(element).stream()
                        .map(match -> readOne(match, support))
                        .flatMap(Optional::stream)
                        .toList();

                return values.isEmpty() ? Optional.empty() : Optional.of(values);
            }

            return firstMatch(element).flatMap(match -> readOne(match, support));
        }

        private List<PageElement> matches(PageElement element) {
            return selector == null ? List.of(element) : element.selectAll(selector);
        }

        private Optional<PageElement> firstMatch(PageElement element) {
            return selector == null ? Optional.of(element) : element.select(selector);
        }

        private Optional<Object> readOne(PageElement match, ExtractionSupport support) {
            Optional<Object> raw = switch (reading) {
                case TEXT -> nonBlank(match.text());
                case OWN_TEXT -> nonBlank(match.ownText());
                case HTML -> nonBlank(match.html());
                case ATTRIBUTE -> match.attribute(attribute).map(value -> value);
                case LINK -> match.link(attribute).map(value -> value);
                case PRESENT -> Optional.of(Boolean.TRUE);
            };

            if (type == null) {
                return raw;
            }

            return raw.map(value -> support.convert(value, type));
        }

        private Optional<Object> nonBlank(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(value.trim());
        }
    }

    /**
     * 📖 What is read once a selector has matched.
     */
    enum Reading {
        TEXT, OWN_TEXT, HTML, ATTRIBUTE, LINK, PRESENT
    }

}
