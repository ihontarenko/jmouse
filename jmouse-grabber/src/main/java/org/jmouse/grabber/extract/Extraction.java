package org.jmouse.grabber.extract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jmouse.grabber.document.PageDocument;
import org.jmouse.grabber.document.PageElement;

/**
 * 🧪 Which fields to pull off a page, and what they become.
 *
 * <pre>{@code
 * static final Extraction<Product> PRODUCT = Extraction.into(Product.class)
 *         .field("title",  css("h1").text())
 *         .field("price",  css(".price").text().as(BigDecimal.class))
 *         .field("sku",    css("[data-sku]").attribute("data-sku"))
 *         .field("images", css(".gallery img").link("src").many());
 * }</pre>
 *
 * <p>⚠️ <b>An extraction produces a map and stops there.</b> The map becomes the caller's own record
 * through {@link org.jmouse.core.binding.Bind} — which is exactly how this library binds its own
 * configuration — and the per-field coercion is {@link org.jmouse.core.convert.Conversion}. That
 * split is the whole design: the grabber decides what text is where, and the library decides what
 * type it becomes.</p>
 *
 * <p>A repeating block gets {@link #each(String)}: the extraction then runs once per match of that
 * selector, scoped to it, which is what turns a listing page into a list of objects in one call.</p>
 *
 * @param <T> what one extraction produces
 */
public final class Extraction<T> {

    private final Class<T>              type;
    private final String                blockSelector;
    private final Map<String, FieldRule> fields;
    private final ExtractionSupport     support;

    private Extraction(
            Class<T> type, String blockSelector, Map<String, FieldRule> fields, ExtractionSupport support) {
        this.type = type;
        this.blockSelector = blockSelector;
        this.fields = fields;
        this.support = support;
    }

    /**
     * 🧪 An extraction producing the caller's own type — a record, usually.
     */
    public static <T> Extraction<T> into(Class<T> type) {
        return new Extraction<>(type, null, new LinkedHashMap<>(), ExtractionSupport.standard());
    }

    /**
     * 🧪 An extraction producing a plain map, for the caller who does not want a type for it.
     */
    @SuppressWarnings("unchecked")
    public static Extraction<Map<String, Object>> intoMap() {
        return new Extraction<>(
                (Class<Map<String, Object>>) (Class<?>) Map.class,
                null, new LinkedHashMap<>(), ExtractionSupport.standard());
    }

    /**
     * ➕ One field, by name, read by this rule.
     *
     * <p>⚠️ For a record, the name must be a component name — or one named by {@code @BindName} on
     * the component. A name that matches nothing binds to nothing, silently, which is the first
     * thing that will confuse somebody.</p>
     */
    public Extraction<T> field(String name, FieldRule rule) {
        Map<String, FieldRule> extended = new LinkedHashMap<>(fields);

        extended.put(name, rule);

        return new Extraction<>(type, blockSelector, extended, support);
    }

    /**
     * 🔁 Runs once per match of this selector, scoped to it — the listing-page case.
     */
    public Extraction<T> each(String selector) {
        return new Extraction<>(type, selector, fields, support);
    }

    /**
     * 🧰 Uses a different conversion set — for the run that needs a converter the library does not
     * ship, registered on its own {@code Conversion}.
     */
    public Extraction<T> using(ExtractionSupport other) {
        return new Extraction<>(type, blockSelector, fields, other);
    }

    /**
     * 🧪 One result from the whole document, or from its first matching block.
     */
    public Optional<T> from(PageDocument document) {
        if (blockSelector == null) {
            return fromElement(document.root());
        }

        return document.select(blockSelector).flatMap(this::fromElement);
    }

    /**
     * 🧪 One result per matching block, or a single result when there is no block selector.
     */
    public List<T> allFrom(PageDocument document) {
        if (blockSelector == null) {
            return fromElement(document.root()).map(List::of).orElseGet(List::of);
        }

        List<T> results = new ArrayList<>();

        for (PageElement block : document.selectAll(blockSelector)) {
            fromElement(block).ifPresent(results::add);
        }

        return List.copyOf(results);
    }

    /**
     * 🧪 One result out of this element.
     *
     * <p>An element from which every field came back empty answers empty rather than an object of
     * nulls: a block that matched the selector and carried none of the fields is a block the selector
     * was wrong about, and an empty object hides that.</p>
     */
    public Optional<T> fromElement(PageElement element) {
        Map<String, Object> values = new LinkedHashMap<>();

        fields.forEach((name, rule) -> rule.read(element, support).ifPresent(value -> values.put(name, value)));

        if (values.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(support.bind(values, type));
    }

    /**
     * 🗺️ The raw map, before binding — for the caller who wants to see what was found.
     */
    public Map<String, Object> valuesFrom(PageElement element) {
        Map<String, Object> values = new LinkedHashMap<>();

        fields.forEach((name, rule) -> rule.read(element, support).ifPresent(value -> values.put(name, value)));

        return values;
    }

    public Class<T> type() {
        return type;
    }

    public Optional<String> blockSelector() {
        return Optional.ofNullable(blockSelector);
    }

    public List<String> fieldNames() {
        return List.copyOf(fields.keySet());
    }

}
