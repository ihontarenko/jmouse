package org.jmouse.settings;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterTreeTest {

    private static Map<String, Object> record(Object... namesAndValues) {
        Map<String, Object> record = new LinkedHashMap<>();
        for (int position = 0; position < namesAndValues.length; position += 2) {
            record.put((String) namesAndValues[position], namesAndValues[position + 1]);
        }
        return record;
    }

    @Test
    void aRecordUnfoldsByItsWordsAListByItsIndices() {
        Map<String, Object> layout = record(
                "circuit", record("gap", 1.5, "passes", 400, "connector", record("designator", "J")),
                "copperStacks", List.of(1, 2, 4),
                "papers", List.of(record("name", "A4", "width", 297)));
        Map<String, String> parameters = ParameterTree.unfold(layout);
        assertEquals(List.of("circuit.gap", "circuit.passes", "circuit.connector.designator", "copperStacks.0", "copperStacks.1",
                             "copperStacks.2", "papers.0.name", "papers.0.width"), List.copyOf(parameters.keySet()));
        assertEquals("1.5", parameters.get("circuit.gap"));
        assertEquals("297", parameters.get("papers.0.width"));
    }

    @Test
    void aMapKeyThatIsNotOnePlainWordStandsInBrackets() {
        Map<String, String> parameters = ParameterTree.unfold(record("habits", record("pcb.look", "classic", "2024", "on")));
        assertEquals(Map.of("habits[pcb.look]", "classic", "habits[2024]", "on"), parameters);
        assertEquals(record("habits", record("pcb.look", "classic", "2024", "on")), ParameterTree.fold(parameters));
    }

    @Test
    void emptinessAndNothingAreRowsToo() {
        Map<String, String> parameters = ParameterTree.unfold(record("habits", record(), "jobs", List.of(), "note",
                Arrays.asList((Object) null).getFirst()));
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("habits", "{}");
        expected.put("jobs", "[]");
        expected.put("note", null);
        assertEquals(expected, parameters);
        Map<String, Object> folded = record("habits", record(), "jobs", new ArrayList<>(), "note", null);
        assertEquals(folded, ParameterTree.fold(parameters));
    }

    @Test
    void anEmptyRecordGivesWayToWhatAnotherKeyHoldsThere() {
        Map<String, String> before = new LinkedHashMap<>();
        before.put("habits", "{}");
        before.put("habits[pcb.look]", "classic");
        Map<String, String> after = new LinkedHashMap<>();
        after.put("habits[pcb.look]", "classic");
        after.put("habits", "{}");
        assertEquals(record("habits", record("pcb.look", "classic")), ParameterTree.fold(before));
        assertEquals(record("habits", record("pcb.look", "classic")), ParameterTree.fold(after));
    }

    @Test
    void foldingGivesTextInTheTreesShapeItsListsInIndexOrder() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("copperStacks.10", "8");
        parameters.put("copperStacks.2", "4");
        parameters.put("circuit.gap", "1.5");
        assertEquals(record("copperStacks", List.of("4", "8"), "circuit", record("gap", "1.5")), ParameterTree.fold(parameters));
    }

    @Test
    void aSectionThatIsAListUnfoldsFromItsIndices() {
        Map<String, String> parameters = ParameterTree.unfold(List.of(record("ending", "-F_Cu.gbr")));
        assertEquals(Map.of("0.ending", "-F_Cu.gbr"), parameters);
        assertEquals(List.of(record("ending", "-F_Cu.gbr")), ParameterTree.fold(parameters));
    }

    @Test
    void twoKeysThatDisagreeOnAPlaceAreRefused() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("circuit", "1");
        parameters.put("circuit.gap", "1.5");
        assertThrows(IllegalArgumentException.class, () -> ParameterTree.fold(parameters));
        Map<String, String> listAndRecord = new LinkedHashMap<>();
        listAndRecord.put("papers.0", "A4");
        listAndRecord.put("papers.name", "A4");
        assertThrows(IllegalArgumentException.class, () -> ParameterTree.fold(listAndRecord));
    }
}
