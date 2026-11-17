package org.jmouse.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonSyntaxTest {

    @Test
    void everyKindOfValueIsReadAsJmeWouldHoldIt() {
        Object read = Json.read("""
                {
                  "x": 2, "y": -0.5, "big": 12345678901,
                  "name": "R\\u00e9sistor \\"1\\" \\\\ \\/ \\n",
                  "on": true, "off": false, "none": null,
                  "list": [1, [2], {}, []]
                }
                """);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("x", 2);
        expected.put("y", -0.5);
        expected.put("big", 12345678901L);
        expected.put("name", "Résistor \"1\" \\ / \n");
        expected.put("on", true);
        expected.put("off", false);
        expected.put("none", null);
        expected.put("list", List.of(1, List.of(2), Map.of(), List.of()));

        assertEquals(expected, read);
    }

    @Test
    void aNumberIsReadByTheEnginesRule() {
        assertEquals(List.of(0, -1, 2.5, 1.0E10, new BigDecimal("0.1000000000000000000000001")),
                     Json.read("[0, -1, 2.5, 1e10, 0.1000000000000000000000001]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"x\": 1,}", "[1,]", "[1 2]", "{x: 1}", "{'x': 1}", "01", "- 1", "+1", ".5", "1.", "none", "undefined",
            "\"a\\qb\"", "\"\\u12\"", "[1] 2", "", "{\"x\" 1}", "[1, 2", "\"tab\there\"", "1L", "0x10"})
    void whatIsNotJsonIsRefused(String text) {
        assertThrows(JsonSyntaxException.class, () -> Json.read(text), text);
    }

    @Test
    void aRefusalSaysWhereAndWhat() {
        JsonSyntaxException refused = assertThrows(JsonSyntaxException.class, () -> Json.read("{\"x\": 1,}"));

        assertEquals("Not JSON: a member's name in double quotes expected, '}' found at line 1, character 9", refused.getMessage());
    }

    @Test
    void aValueIsWrittenCompactAsJavaScriptWouldWriteIt() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("whole", 5.0);
        value.put("half", 2.5);
        value.put("nan", Double.NaN);
        value.put("text", "a\"b\nc\u0001");
        value.put("array", new int[]{1, 2});
        value.put("list", Arrays.asList('c', null, false));
        value.put("empty", Map.of());

        assertEquals("{\"whole\":5,\"half\":2.5,\"nan\":null,\"text\":\"a\\\"b\\nc\\u0001\",\"array\":[1,2],\"list\":[\"c\",null,false],\"empty\":{}}",
                     Json.write(value));
    }

    @Test
    void anIndentedTextIsAMemberALine() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("x", 1);
        value.put("items", List.of(1, Map.of("deep", true)));
        value.put("none", List.of());

        assertEquals("""
                {
                    "x": 1,
                    "items": [
                        1,
                        {
                            "deep": true
                        }
                    ],
                    "none": []
                }""", Json.write(value, JsonLayout.INDENTED));
    }

    @Test
    void whatIsWrittenIsReadBackBothWays() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("numbers", List.of(1, 2.25, -3, 12345678901L));
        value.put("nested", Map.of("deep", List.of("x", "\\\"")));

        assertEquals(value, Json.read(Json.write(value)));
        assertEquals(value, Json.read(Json.write(value, JsonLayout.INDENTED)));
    }
}
