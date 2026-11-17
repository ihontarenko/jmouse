package org.jmouse.j2js.el;

import org.jmouse.j2js.Export;
import org.jmouse.json.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🌐 What a page may ask of jME. An answer is JSON: {@code {"value": …}} — the expression's value — or {@code {"refusal": "…"}}
 * — why there is none, in the language's own words. A refusal is an answer, not an exception: the page is never left to catch
 * a Java one.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmeInBrowser {

    private JmeInBrowser() {
    }

    /**
     * The value of {@code expression}, its names given by {@code values} — a JSON object, {@code {"x": 2, "items": [1, 2]}};
     * empty text for none.
     */
    @Export
    public static String evaluate(String expression, String values) {
        Map<String, Object> answer = new LinkedHashMap<>();

        try {
            answer.put("value", PageEvaluation.evaluate(expression, valuesOf(values)));
        } catch (RuntimeException refusal) {
            answer.put("refusal", refusal.getMessage() != null ? refusal.getMessage() : refusal.getClass().getSimpleName());
        }

        return Json.write(answer);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> valuesOf(String values) {
        if (values == null || values.isBlank()) {
            return Map.of();
        }
        if (Json.read(values) instanceof Map<?, ?> named) {
            return (Map<String, Object>) named;
        }
        throw new IllegalArgumentException("The values are a JSON object of names: {\"x\": 2}");
    }
}
