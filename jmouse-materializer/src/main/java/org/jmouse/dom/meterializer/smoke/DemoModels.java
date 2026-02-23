package org.jmouse.dom.meterializer.smoke;

import org.jmouse.meterializer.ModelReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DemoModels {

    private DemoModels() {}

    /**
     * Universal demo model for smoke tests.
     *
     * Structure:
     *
     * {
     *   title,
     *   description,
     *   fields: [ ViewBlock ],
     *   submit: { ... }
     * }
     */
    public static Map<String, Object> bootstrapFormDemo() {

        Map<String, Object> root = new LinkedHashMap<>();

        root.put("title", "Add Resistor!");
        root.put("description", "Universal dynamic form.");

        // ---- dynamic blocks ----

        List<Map<String, Object>> fields = List.of(

                Map.of(
                        "elementType", "text",
                        "name", "element_name",
                        "label", "Name Of Component:",
                        "value", "R12"
                ),

                Map.of(
                        "elementType", "select",
                        "name", "vendor",
                        "label", "Vendor:",
                        "selected", "ti",
                        "options", List.of(
                                Map.of("key", "vishay", "label", "Vishay"),
                                Map.of("key", "ti", "label", "Texas Instruments")
                        )
                )
        );

        root.put("fields", fields);

        // ---- submit ----

        root.put("submit",
                 Map.of("caption", "Submit")
        );

        return root;
    }
}
