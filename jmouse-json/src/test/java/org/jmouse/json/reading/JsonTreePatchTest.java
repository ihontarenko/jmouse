package org.jmouse.json.reading;

import org.jmouse.json.Json;
import org.jmouse.json.JsonMappingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A patch applied to a tree in place: the places it names changed, the rest the very nodes they were. */
class JsonTreePatchTest {

    private static final String DOCUMENT = "{\"name\": \"video\", \"lines\": [{\"points\": [1, 2]}, {\"points\": [3, 4]}]}";

    @Test
    @SuppressWarnings("unchecked")
    void appliesEachOperationInPlace() {
        Map<String, Object> tree = (Map<String, Object>) Json.read(DOCUMENT);
        Object untouched = ((List<Object>) tree.get("lines")).get(0);
        List<?> patch = (List<?>) Json.read("""
                [{"op": "replace", "path": "/lines/1/points/0", "value": 5},
                 {"op": "add", "path": "/lines/1/points/-", "value": 6},
                 {"op": "remove", "path": "/name"},
                 {"op": "copy", "from": "/lines/0", "path": "/first"},
                 {"op": "test", "path": "/lines/1/points/2", "value": 6}]""");

        Object now = JsonTreePatch.applied(tree, patch);

        assertSame(tree, now);
        assertEquals(Json.read("{\"lines\": [{\"points\": [1, 2]}, {\"points\": [5, 4, 6]}], \"first\": {\"points\": [1, 2]}}"), now);
        assertSame(untouched, ((List<Object>) tree.get("lines")).get(0));
    }

    @Test
    void readsWhatAPatchChanged() {
        List<?> patch = (List<?>) Json.read("[{\"op\": \"replace\", \"path\": \"/lines/1/points/0\", \"value\": 5}, {\"op\": \"remove\", \"path\": \"/name\"}]");
        ChangedPaths changed = JsonTreePatch.changedBy(patch);
        assertTrue(changed.at("lines").at("1").at("points").at("0").isWhole());
        assertTrue(changed.isWhole());
    }

    @Test
    void refusesAPatchThatDoesNotFit() {
        List<?> patch = (List<?>) Json.read("[{\"op\": \"replace\", \"path\": \"/lines/7\", \"value\": 1}]");
        assertThrows(JsonMappingException.class, () -> JsonTreePatch.applied(Json.read(DOCUMENT), patch));
    }
}
