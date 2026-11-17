package org.jmouse.core.parameters;

import org.jmouse.core.parameters.RequestParametersTreeOptions.Conflict;
import org.jmouse.core.parameters.RequestParametersTreeOptions.Emptiness;
import org.jmouse.core.parameters.RequestParametersTreeOptions.Root;
import org.jmouse.core.parameters.support.RequestParametersTreeFlattener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestParametersTreeTest {

    /** How a store reads its keys: one spelling, a disagreement refused, a list at the root, {} and [] for emptiness. */
    private static final RequestParametersTreeOptions STORE = new RequestParametersTreeOptions(
            RequestParametersSpelling.ONE, Conflict.REFUSE, Root.RECORD_OR_LIST, new Emptiness("{}", "[]"));

    private final RequestParametersJavaStructureConverter converter =
            new RequestParametersJavaStructureConverter(RequestParametersJavaStructureOptions.defaults());

    private static Map<String, String[]> named(String... namesAndValues) {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        for (int position = 0; position < namesAndValues.length; position += 2) {
            parameters.put(namesAndValues[position], new String[]{namesAndValues[position + 1]});
        }
        return parameters;
    }

    private Object folded(RequestParametersTreeOptions options, Map<String, String[]> parameters) {
        return converter.toJavaObject(new RequestParametersTreeParser(options).parse(parameters));
    }

    @Test
    void aRequestIsReadAsItWasTheLastNameStanding() {
        Map<String, String[]> parameters = named("filter", "all", "filter[admin]", "yes", "[0]", "nameless");
        assertEquals(Map.of("filter", Map.of("admin", "yes")), folded(RequestParametersTreeOptions.defaults(), parameters));
    }

    @Test
    void aStoreRefusesTwoKeysThatDisagreeOnAPlace() {
        assertThrows(IllegalArgumentException.class, () -> folded(STORE, named("circuit", "1", "circuit.gap", "1.5")));
        assertThrows(IllegalArgumentException.class, () -> folded(STORE, named("papers[0]", "A4", "papers.name", "A4")));
        assertThrows(IllegalArgumentException.class, () -> folded(STORE, named("circuit[gap]", "1.5")));
        assertThrows(IllegalArgumentException.class, () -> folded(STORE, named("groups[]", "admin")));
    }

    @Test
    void anEmptyPlaceGivesWayToWhatAnotherKeyPutsThere() {
        assertEquals(Map.of("looks", Map.of("look", "classic")), folded(STORE, named("looks", "{}", "looks.look", "classic")));
        assertEquals(Map.of("looks", Map.of("look", "classic")), folded(STORE, named("looks.look", "classic", "looks", "{}")));
        assertEquals(Map.of("jobs", List.of(), "looks", Map.of()), folded(STORE, named("jobs", "[]", "looks", "{}")));
    }

    @Test
    void aStoreMayHoldAListAtItsRootItsItemsInIndexOrder() {
        assertEquals(List.of(Map.of("ending", "-F_Cu.gbr"), Map.of("ending", "-B_Cu.gbr")),
                     folded(STORE, named("[1].ending", "-B_Cu.gbr", "[0].ending", "-F_Cu.gbr")));
    }

    @Test
    void aValueThatIsNullIsKept() {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("note", new String[]{null});
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("note", null);
        assertEquals(expected, folded(STORE, parameters));
    }

    @Test
    void plainJavaUnfoldsIntoKeysThatFoldBackIntoIt() {
        Map<String, Object> java = new LinkedHashMap<>();
        java.put("circuit", Map.of("gap", 1.5));
        java.put("papers", List.of(Map.of("width", 297)));
        java.put("multipliers", Map.of("0", "black"));
        java.put("looks", Map.of());
        java.put("note", null);
        Map<String, List<String>> flattened = new RequestParametersTreeFlattener(STORE.emptiness()).flatten(converter.toTree(java));

        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("circuit.gap", List.of("1.5"));
        expected.put("papers[0].width", List.of("297"));
        expected.put("multipliers.0", List.of("black"));
        expected.put("looks", List.of("{}"));
        expected.put("note", new ArrayList<>(Arrays.asList((String) null)));
        assertEquals(expected, flattened);

        Map<String, String[]> named = new LinkedHashMap<>();
        flattened.forEach((name, values) -> named.put(name, values.toArray(String[]::new)));
        Map<String, Object> folded = new LinkedHashMap<>();
        folded.put("circuit", Map.of("gap", "1.5"));
        folded.put("papers", List.of(Map.of("width", "297")));
        folded.put("multipliers", Map.of("0", "black"));
        folded.put("looks", Map.of());
        folded.put("note", null);
        assertEquals(folded, folded(STORE, named));
    }

    @Test
    void aTreeHoldsAValueOnlyUnderAName() {
        assertThrows(IllegalArgumentException.class, () -> converter.toTree("alone"));
    }
}
