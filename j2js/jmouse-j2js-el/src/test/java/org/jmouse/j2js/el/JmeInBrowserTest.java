package org.jmouse.j2js.el;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JmeInBrowserTest {

    @Test
    void anExpressionIsEvaluatedOverTheValuesThePageGives() {
        assertEquals("{\"value\":5}", JmeInBrowser.evaluate("1 + 2 * x", "{\"x\": 2}"));
    }

    @Test
    void aValueIsReadAsJsonGivesIt() {
        assertEquals("{\"value\":\"Ada\"}", JmeInBrowser.evaluate("person.name", "{\"person\": {\"name\": \"Ada\"}}"));
        assertEquals("{\"value\":3}", JmeInBrowser.evaluate("items[2]", "{\"items\": [1, 2, 3]}"));
    }

    @Test
    void noValuesIsAnEmptyText() {
        assertEquals("{\"value\":true}", JmeInBrowser.evaluate("2 > 1", ""));
    }

    @Test
    void whatCannotBeEvaluatedIsARefusalNotAnException() {
        String answer = JmeInBrowser.evaluate("1 +", "");

        assertTrue(answer.startsWith("{\"refusal\":"), answer);
    }

    @Test
    void valuesThatAreNoObjectAreRefused() {
        assertEquals("{\"refusal\":\"The values are a JSON object of names: {\\\"x\\\": 2}\"}", JmeInBrowser.evaluate("x", "[1]"));
    }
}
