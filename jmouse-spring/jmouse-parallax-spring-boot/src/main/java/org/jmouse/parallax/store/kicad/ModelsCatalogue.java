package org.jmouse.parallax.store.kicad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.parallax.kicad.KiCadCatalogue;
import org.jmouse.parallax.store.catalogue.ComponentModels;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The store's answers to what a KiCad import asks of a component: the model an editor reads ({@link ComponentModels})
 * — its pins with their package numbers, its default symbol's drawing and which pin each of that symbol's ports
 * stands for —, as the reader may see it.
 */
final class ModelsCatalogue implements KiCadCatalogue {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ComponentModels components;
    private final Map<String, Optional<ComponentModels.ModelView>> described = new LinkedHashMap<>();

    ModelsCatalogue(ComponentModels components) {
        this.components = components;
    }

    @Override
    public Optional<PartDrawing> drawingOf(String component) {
        return model(component).filter(model -> model.symbolReference() != null && model.symbolDrawing() != null)
                .flatMap(model -> {
                    Map<String, double[]> pins = new LinkedHashMap<>();
                    try {
                        for (JsonNode pin : JSON.readTree(model.symbolDrawing()).path("pins")) {
                            String port = pin.path("name").asText();
                            pins.put(model.symbolPorts().getOrDefault(port, port),
                                    new double[] {pin.path("x").asDouble(), pin.path("y").asDouble()});
                        }
                    } catch (java.io.IOException unreadable) {
                        return Optional.empty();
                    }
                    return Optional.of(new PartDrawing(model.symbolReference(), pins));
                });
    }

    @Override
    public Map<String, String> pinsByNumber(String component) {
        Map<String, String> numbered = new LinkedHashMap<>();
        model(component).ifPresent(model -> model.pins().forEach(pin -> pin.numbers().forEach((unit, number) ->
                numbered.putIfAbsent(String.valueOf(number),
                        unit.isEmpty() || pin.shared() ? pin.name() : unit + "." + pin.name()))));
        return numbered;
    }

    /** The model as an editor reads it; empty for a component the reader may not see or the catalogue lacks. */
    private Optional<ComponentModels.ModelView> model(String component) {
        return described.computeIfAbsent(component, reference -> {
            try {
                return Optional.of(components.describe(reference));
            } catch (RuntimeException unknown) {
                return Optional.empty();
            }
        });
    }
}
