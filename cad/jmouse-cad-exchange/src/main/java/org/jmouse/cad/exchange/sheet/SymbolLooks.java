package org.jmouse.cad.exchange.sheet;

import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Drawing;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.schematic.drawing.LivingDrawing;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The looks of the components a sheet's parts are, and the symbols they are drawn with, each read once from the catalogue
 * the sheet is read for: a sheet names the same component and the same symbol many times.
 */
public final class SymbolLooks {

    private final CatalogueDocuments                  catalogue;
    private final Map<String, Optional<SymbolLook>>    looks   = new HashMap<>();
    private final Map<String, Optional<SymbolDrawing>> symbols = new HashMap<>();

    public SymbolLooks(CatalogueDocuments catalogue) {
        this.catalogue = catalogue;
    }

    /**
     * How a component is drawn with one of its symbols: the one a part names when it is drawn with another than the default
     * ({@code symbol operational-amplifier-wide-symbol}), else its default. Empty when the catalogue holds no such component
     * the reader may see.
     *
     * @param variant the symbol the part names, or null
     */
    public Optional<SymbolLook> look(String component, String variant) {
        return looks.computeIfAbsent(component + "|" + (variant == null ? "" : variant), key -> read(component, variant));
    }

    /** A symbol's drawing, by its slug; empty when the catalogue holds no such symbol. */
    public Optional<SymbolDrawing> symbol(String slug) {
        return symbols.computeIfAbsent(slug, wanted -> catalogue.find(wanted)
                .filter(document -> document.kind().equals(SchematicModule.SYMBOL)).map(LivingDrawing::of));
    }

    private Optional<SymbolLook> read(String slug, String variant) {
        Optional<DocumentNode> document = catalogue.find(slug).filter(found -> found.kind().equals(SimulationModule.COMPONENT));
        if (document.isEmpty()) {
            return Optional.empty();
        }
        Component written = SimulationReading.component(document.get());
        SimulatedComponent compiled = ComponentCompilation.compile(written, catalogue);
        Map<String, Pin> pins = new LinkedHashMap<>();
        Set<String> shared = new LinkedHashSet<>();
        compiled.pins().forEach(pin -> {
            pins.put(pin.name(), pin);
            if (compiled.shared(pin)) {
                shared.add(pin.name());
            }
        });
        Optional<Drawing> uses = written.drawn();
        // The symbol the part is drawn with: the variant it names when its component offers it, else the default.
        Optional<String> drawnWith = uses.filter(symbol -> !symbol.uses().isEmpty()).map(symbol ->
                variant != null && symbol.uses().contains(variant) ? variant : symbol.uses().getFirst());
        Map<String, List<String>> sides = new LinkedHashMap<>();
        uses.ifPresent(symbol -> symbol.box().forEach(side -> sides.put(side.side(), side.pins().stream().map(Drawing.BoxPin::pin).toList())));
        Map<String, String> ports = new LinkedHashMap<>();
        uses.ifPresent(symbol -> symbol.bindings().forEach(binding -> binding.boundPin().ifPresent(pin -> ports.put(binding.input(), pin))));
        return Optional.of(new SymbolLook(slug, compiled.title(), List.copyOf(compiled.units()), pins, shared, List.copyOf(pins.keySet()),
                drawnWith, drawnWith.flatMap(this::symbol), ports, sides, written, compiled));
    }
}
