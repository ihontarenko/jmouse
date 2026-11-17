package org.jmouse.settings;

import org.jmouse.core.parameters.RequestParametersJavaStructureConverter;
import org.jmouse.core.parameters.RequestParametersJavaStructureOptions;
import org.jmouse.core.parameters.RequestParametersPathParser;
import org.jmouse.core.parameters.RequestParametersPathToken;
import org.jmouse.core.parameters.RequestParametersPathWriter;
import org.jmouse.core.parameters.RequestParametersSpelling;
import org.jmouse.core.parameters.RequestParametersTreeOptions;
import org.jmouse.core.parameters.RequestParametersTreeOptions.Conflict;
import org.jmouse.core.parameters.RequestParametersTreeOptions.Emptiness;
import org.jmouse.core.parameters.RequestParametersTreeOptions.Root;
import org.jmouse.core.parameters.RequestParametersTreeParser;
import org.jmouse.core.parameters.support.RequestParametersTreeFlattener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🌳 A structured value unfolded into its parameters, one key a value, and folded back — jmouse-core's parameters tree held to
 * what a store keeps. A key is a path of its one grammar ({@link RequestParametersPathParser}, {@link RequestParametersPathWriter}):
 * a record's field after a dot ({@code circuit.gap}), a list's item by its index in brackets ({@code papers[0].width}), a map's
 * key that holds a dot in brackets ({@code looks[pcb.look]}) and one of digits alone after a dot ({@code multipliers.0}).
 *
 * <p>Held to a store ({@link #STORE}): a key has one spelling, two keys that disagree on a place are refused, a section may be a
 * list at its root ({@code [0].ending}), an empty record is one parameter of {@value #EMPTY_RECORD} and an empty list one of
 * {@value #EMPTY_LIST} — each giving way to what another key puts there —, and a null one with no value.</p>
 *
 * <p>The tree is plain Java — maps, lists, text, numbers, switches, nulls —, so the library reads no format: whoever declares a
 * section turns its source into such a tree, and reads a folded tree back into its record ({@link SectionReader}). Folded, every
 * value is text; the reader takes it as the type its record says.</p>
 */
public final class ParameterTree {

    /** An empty record's value. */
    public static final String EMPTY_RECORD = "{}";

    /** An empty list's value. */
    public static final String EMPTY_LIST = "[]";

    /** How a store's keys are read: one spelling, a disagreement refused, a list at the root, {@value #EMPTY_RECORD} and {@value #EMPTY_LIST}. */
    private static final RequestParametersTreeOptions STORE = new RequestParametersTreeOptions(
            RequestParametersSpelling.ONE, Conflict.REFUSE, Root.RECORD_OR_LIST, new Emptiness(EMPTY_RECORD, EMPTY_LIST));

    private static final RequestParametersPathParser             PATH_PARSER = new RequestParametersPathParser();
    private static final RequestParametersPathWriter             PATH_WRITER = new RequestParametersPathWriter();
    private static final RequestParametersTreeParser             TREE_PARSER = new RequestParametersTreeParser(STORE);
    private static final RequestParametersTreeFlattener          FLATTENER   = new RequestParametersTreeFlattener(STORE.emptiness());
    private static final RequestParametersJavaStructureConverter CONVERTER   =
            new RequestParametersJavaStructureConverter(RequestParametersJavaStructureOptions.defaults());

    private ParameterTree() {
    }

    /**
     * The parameters of a tree: each value under its key, in the tree's order.
     *
     * @throws IllegalArgumentException where the tree is not a record or a list, or a map's key holds a bracket or nothing
     */
    public static Map<String, String> unfold(Object tree) {
        Map<String, String> parameters = new LinkedHashMap<>();
        FLATTENER.flatten(CONVERTER.toTree(tree)).forEach((key, values) -> parameters.put(key, values.getFirst()));
        return parameters;
    }

    /**
     * The tree the parameters make: maps, lists in their indices' order, and every value as text — {@value #EMPTY_RECORD} and
     * {@value #EMPTY_LIST} an empty record and list, no value a null.
     *
     * @throws IllegalArgumentException where a key is not written as its path is, or two keys say one place is both a record and
     *                                  a list, or both a value and more
     */
    public static Object fold(Map<String, String> parameters) {
        Map<String, String[]> named = new LinkedHashMap<>();
        parameters.forEach((key, value) -> named.put(key, new String[]{value}));
        return CONVERTER.toJavaObject(TREE_PARSER.parse(named));
    }

    /**
     * A key written from its path.
     *
     * @throws IllegalArgumentException where the path is empty — a section's root is a record or a list —, or a name of it holds a
     *                                  bracket or nothing
     */
    public static String keyOf(List<RequestParametersPathToken> path) {
        return PATH_WRITER.write(path);
    }

    /**
     * A key's path: {@code papers[0].width} → papers, index 0, width; {@code looks[pcb.look]} → looks, pcb.look.
     *
     * @throws IllegalArgumentException where the key is not written as its path is — {@code a[b]} for {@code a.b}, an index after a
     *                                  dot, an append {@code []}, a bracket never closed, nothing at all
     */
    public static List<RequestParametersPathToken> pathOf(String key) {
        return PATH_PARSER.parse(key, RequestParametersSpelling.ONE);
    }
}
