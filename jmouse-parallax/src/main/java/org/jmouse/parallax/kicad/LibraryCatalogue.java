package org.jmouse.parallax.kicad;

import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The answers of a {@link ComponentLibrary} — the jMX documents a catalogue holds by slug, components and symbols alike —
 * as an import asks them: a component's package numbers from its model, its drawing from the symbol it uses, each
 * symbol port on the pin the component maps it to ({@code symbol uses … { positive_supply Vcc }}), or on the pin of its
 * own name.
 */
final class LibraryCatalogue implements KiCadCatalogue {

    private final ComponentLibrary library;
    private final Map<String, Optional<PartDrawing>> drawings = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> numbers = new ConcurrentHashMap<>();

    LibraryCatalogue(ComponentLibrary library) {
        this.library = library;
    }

    @Override
    public Optional<PartDrawing> drawingOf(String component) {
        return drawings.computeIfAbsent(component, this::drawing);
    }

    @Override
    public Map<String, String> pinsByNumber(String component) {
        return numbers.computeIfAbsent(component, this::numbered);
    }

    @Override
    public Optional<String> footprintOf(String component) {
        return document(component).flatMap(document -> document.block(Block.FootprintUse.class))
                .map(Block.FootprintUse::footprint);
    }

    private Map<String, String> numbered(String component) {
        Optional<String> source = library.source(component);
        if (source.isEmpty()) {
            return Map.of();
        }
        ComponentModel model = ComponentDocuments.read(source.get(), component, library);
        Map<String, String> pins = new LinkedHashMap<>();
        for (ComponentModel.Pin pin : model.pins()) {
            pin.numbers().forEach((unit, number) -> pins.putIfAbsent(String.valueOf(number),
                    unit.isEmpty() || pin.shared() ? pin.name() : unit + "." + pin.name()));
        }
        return Map.copyOf(pins);
    }

    private Optional<PartDrawing> drawing(String component) {
        List<PartDrawing> all = drawingsOf(component);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getFirst());
    }

    @Override
    public List<PartDrawing> drawingsOf(String component) {
        Optional<ExperienceDocument> document = document(component);
        if (document.isEmpty()) {
            return List.of();
        }
        Optional<Block.Symbol> uses = symbolBlock(document.get()).filter(symbol -> !symbol.uses().isEmpty());
        if (uses.isEmpty()) {
            return List.of();
        }
        Map<String, String> ports = uses.get().ports();
        List<PartDrawing> drawings = new ArrayList<>();
        for (String slug : uses.get().uses()) {
            document(slug).flatMap(LibraryCatalogue::symbolBlock).ifPresent(drawn -> {
                Map<String, double[]> pins = new LinkedHashMap<>();
                drawn.pins().forEach(pin -> pins.put(ports.getOrDefault(pin.pin(), pin.pin()), new double[] {pin.x(), pin.y()}));
                drawings.add(new PartDrawing(slug, pins));
            });
        }
        return List.copyOf(drawings);
    }

    private Optional<ExperienceDocument> document(String slug) {
        return library.source(slug).map(source -> ExperienceReader.read(source, slug + ".jmx").getFirst());
    }

    private static Optional<Block.Symbol> symbolBlock(ExperienceDocument document) {
        return document.blocks().stream().filter(Block.Symbol.class::isInstance).map(Block.Symbol.class::cast).findFirst();
    }
}
