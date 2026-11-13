package org.jmouse.cad.simulation;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.el.grammar.Nodes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * A category of the catalogue, as its own document says it: a {@code category} document is filed under its parent the
 * way every document is filed in its category — {@code meta { category passive }} —, and says where it stands among its
 * siblings with {@code order}. Nothing else holds a category: the tree is the documents.
 *
 * @param slug    the category's address, which every document filed in it names
 * @param title   what it is called
 * @param summary what it holds, or null
 * @param parent  the category it stands under, or null at the top
 * @param order   where it stands among its siblings; null where it says nothing, after those that do
 */
public record CategoryDocument(String slug, String title, String summary, String parent, Integer order) {

    /** Siblings by their order, those that give none after, then by title. */
    public static final Comparator<CategoryDocument> SIBLING_ORDER = Comparator
            .comparing(CategoryDocument::order, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(CategoryDocument::title);

    /** The category a document says it is; refuses a document of another kind, or an order that is no whole number. */
    public static CategoryDocument of(DocumentNode document) {
        if (!document.kind().equals(SimulationModule.CATEGORY)) {
            throw new IllegalArgumentException("'" + document.slug() + "' is a " + document.kind().word() + ", not a category");
        }
        Optional<MetaNode> meta = document.block(MetaNode.class);
        Integer order = meta.flatMap(found -> found.text(MetaNode.ORDER)).map(written -> orderOf(document.slug(), written))
                .orElse(null);
        return new CategoryDocument(document.slug(), document.title(), meta.flatMap(found -> found.text(MetaNode.SUMMARY)).orElse(null),
                meta.flatMap(found -> found.text(MetaNode.CATEGORY)).orElse(null), order);
    }

    /** The category as the tree of its document, which the language writes. */
    public DocumentNode document() {
        DocumentNode document = new DocumentNode(SimulationModule.CATEGORY, title, slug, null, 1);
        MetaNode meta = new MetaNode();
        if (summary != null && !summary.isBlank()) {
            meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(summary.strip())));
        }
        if (parent != null && !parent.isBlank()) {
            meta.addExpression(MetaLineNode.of(MetaNode.CATEGORY, Nodes.word(parent.strip())));
        }
        if (order != null) {
            meta.addExpression(MetaLineNode.of(MetaNode.ORDER, Nodes.number(order)));
        }
        document.addExpression(meta);
        return document;
    }

    /**
     * Categories as a tree is read: every parent before its children, siblings in their order. A category under one
     * that is not among them, or under itself through its parents, is refused by name.
     */
    public static List<CategoryDocument> parentsFirst(Collection<CategoryDocument> categories) {
        Map<String, CategoryDocument> bySlug = new LinkedHashMap<>();
        categories.forEach(category -> bySlug.put(category.slug(), category));
        List<String> refused = new ArrayList<>();
        for (CategoryDocument category : categories) {
            ringOrOrphan(category, bySlug::get).ifPresent(refused::add);
        }
        if (!refused.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", refused));
        }
        Map<String, List<CategoryDocument>> children = new LinkedHashMap<>();
        categories.forEach(category -> children.computeIfAbsent(category.parent(), ignored -> new ArrayList<>()).add(category));
        children.values().forEach(siblings -> siblings.sort(SIBLING_ORDER));
        List<CategoryDocument> ordered = new ArrayList<>();
        addUnder(null, children, ordered);
        return ordered;
    }

    /**
     * A category and every category under it, however deep: what a document filed "in" a category means to a reader looking
     * there. A category that is none of {@code categories} is itself alone.
     */
    public static Set<String> within(String code, Collection<CategoryDocument> categories) {
        Map<String, List<String>> children = new HashMap<>();
        categories.forEach(category -> children.computeIfAbsent(category.parent(), ignored -> new ArrayList<>()).add(category.slug()));
        Set<String> within = new LinkedHashSet<>();
        Deque<String> waiting = new ArrayDeque<>(List.of(code));
        while (!waiting.isEmpty()) {
            String next = waiting.pop();
            if (within.add(next)) {
                waiting.addAll(children.getOrDefault(next, List.of()));
            }
        }
        return within;
    }

    /**
     * What is wrong with where a category stands, if anything: under a category {@code categories} does not know, or
     * under itself through its parents.
     *
     * @param categories every category by its slug; null for one there is none of
     */
    public static Optional<String> ringOrOrphan(CategoryDocument category, Function<String, CategoryDocument> categories) {
        Set<String> seen = new HashSet<>(List.of(category.slug()));
        for (String above = category.parent(); above != null; ) {
            CategoryDocument parent = categories.apply(above);
            if (parent == null) {
                return Optional.of("the category '" + category.slug() + "' stands under '" + above + "', which is no category");
            }
            if (!seen.add(above)) {
                return Optional.of("the category '" + category.slug() + "' stands under itself");
            }
            above = parent.parent();
        }
        return Optional.empty();
    }

    private static void addUnder(String parent, Map<String, List<CategoryDocument>> children, List<CategoryDocument> ordered) {
        for (CategoryDocument child : children.getOrDefault(parent, List.of())) {
            ordered.add(child);
            addUnder(child.slug(), children, ordered);
        }
    }

    private static Integer orderOf(String slug, String written) {
        try {
            return Integer.valueOf(written.strip());
        } catch (NumberFormatException notWhole) {
            throw new IllegalArgumentException("The category '" + slug + "' gives its order as '" + written
                    + "', which is no whole number", notWhole);
        }
    }
}
