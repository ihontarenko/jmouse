package org.jmouse.cad.kicad;

import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Drawing;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.schematic.drawing.LivingDrawing;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentFacts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The answers a set of documents gives an import, by slug: a component's package numbers from its compiled pins, its
 * drawings from the symbols it uses — each symbol pin on the component pin its binding names
 * ({@code bind:a(pin: positive)}), or on the pin of its own name — and the footprint it sits on.
 */
final class DocumentCatalogue implements KiCadCatalogue {

    private final CatalogueDocuments documents;
    private final ComponentFacts facts;
    private final Map<String, List<PartDrawing>> drawings = new ConcurrentHashMap<>();

    DocumentCatalogue(CatalogueDocuments documents) {
        this.documents = documents;
        this.facts = new ComponentFacts(documents);
    }

    @Override
    public Optional<PartDrawing> drawingOf(String component) {
        List<PartDrawing> all = drawingsOf(component);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getFirst());
    }

    @Override
    public List<PartDrawing> drawingsOf(String component) {
        return drawings.computeIfAbsent(component, this::drawn);
    }

    @Override
    public Map<String, String> pinsByNumber(String component) {
        return facts.pinsByNumber(component);
    }

    @Override
    public Optional<String> valueParameterOf(String component) {
        return facts.valueParameterOf(component);
    }

    @Override
    public List<String> parametersOf(String component) {
        return facts.parametersOf(component);
    }

    @Override
    public Optional<PartDrawing> symbolDrawing(String symbol) {
        return symbol(symbol).map(drawn -> {
            Map<String, double[]> pins = new LinkedHashMap<>();
            drawn.pins().forEach(pin -> pins.put(pin.name(), new double[] {pin.x(), pin.y()}));
            return new PartDrawing(symbol, pins);
        });
    }

    @Override
    public Optional<String> footprintOf(String component) {
        return facts.footprintOf(component);
    }

    private List<PartDrawing> drawn(String component) {
        Optional<Drawing> drawing = facts.component(component).flatMap(Component::drawn).filter(written -> !written.uses().isEmpty());
        if (drawing.isEmpty()) {
            return List.of();
        }
        Map<String, String> ports = new LinkedHashMap<>();
        drawing.get().bindings().forEach(binding -> binding.boundPin().ifPresent(pin -> ports.put(binding.input(), pin)));
        List<PartDrawing> found = new ArrayList<>();
        for (String slug : drawing.get().uses()) {
            symbol(slug).ifPresent(drawn -> {
                Map<String, double[]> pins = new LinkedHashMap<>();
                drawn.pins().forEach(pin -> pins.put(ports.getOrDefault(pin.name(), pin.name()), new double[] {pin.x(), pin.y()}));
                found.add(new PartDrawing(slug, pins));
            });
        }
        return List.copyOf(found);
    }

    private Optional<SymbolDrawing> symbol(String slug) {
        Optional<DocumentNode> document = documents.find(slug).filter(found -> found.kind().equals(SchematicModule.SYMBOL));
        try {
            return document.map(LivingDrawing::of);
        } catch (RuntimeException unreadable) {
            return Optional.empty();
        }
    }
}
