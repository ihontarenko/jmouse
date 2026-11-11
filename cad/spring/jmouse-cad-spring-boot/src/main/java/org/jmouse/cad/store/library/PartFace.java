package org.jmouse.cad.store.library;

import org.jmouse.cad.Parameter;
import org.jmouse.cad.Text;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Drawing;
import org.jmouse.cad.component.Figure;
import org.jmouse.cad.component.Lead;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.schematic.drawing.DrawingBindings;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * How a part of a component is drawn and read on a sheet, worked out once on the server so an interface only draws it:
 * the letters a new part is named with, the box its pins stand on when it uses no symbol, which pin each port of its
 * symbol stands for, what its portrait's inputs are drawn from, what it reads as an instrument, the figures written
 * beside it, its leads and its parameters written as words.
 *
 * <p>A facet the component does not write is its nearest base's: a voltmeter's leads may be written once, on the meter
 * it extends. The symbol line is the component's own — a real part repeats it.</p>
 *
 * @param designator  the letters a new part's name starts with ({@code meta designator}); null when none is written
 * @param value       the parameter a part shows as its value beside it ({@code meta value resistance}); null for none
 * @param box         a box symbol's sides, each with its pins in order; empty for a part drawn with a symbol document
 * @param symbolPorts each port of the default symbol → the pin it stands for, for a port named otherwise than its pin
 * @param portrait    what each input of the portrait is drawn from; null for a part with no portrait
 * @param measures    what it reads as an instrument, figure by figure
 * @param display     the figures written beside it while it runs
 * @param leads       how each pin's lead looks, by pin
 * @param texts       its parameters written as words
 */
public record PartFace(String designator, String value, List<Drawing.BoxSide> box, Map<String, String> symbolPorts,
                       DrawingBindings portrait, List<Measure> measures, List<Display> display, Map<String, Look> leads,
                       List<Words> texts) {

    /** A figure an instrument reads: {@code reading = plus - minus unit V signal}. */
    public record Measure(String name, String unit, boolean signal) {
    }

    /** A figure written beside a running part, its unit and the caption it is shown under. */
    public record Display(String name, String unit, String caption) {
    }

    /** A lead's colour, and the colour of its stripes or null. */
    public record Look(String colour, String stripes) {
    }

    /** A parameter written as words. */
    public record Words(String name, String value, String description) {
    }

    /**
     * The face of a component.
     *
     * @param lineage  the components it is made of, the most general first, itself last
     * @param portrait the drawing its portrait line names, or null
     * @param names    the names a portrait's binding may read: the component's parameters
     */
    public static PartFace of(Component component, List<Component> lineage, SymbolDrawing portrait, Set<String> names) {
        List<Component> nearestFirst = lineage.reversed();
        String designator = nearest(nearestFirst, base -> base.head().meta().get(MetaNode.DESIGNATOR), null);
        String value = nearest(nearestFirst, base -> base.head().meta().get(MetaNode.VALUE), null);
        Drawing symbol = component.symbol();
        List<Drawing.BoxSide> box = symbol == null ? List.of() : symbol.box();
        Map<String, String> symbolPorts = new LinkedHashMap<>();
        if (symbol != null) {
            for (Drawing.Binding binding : symbol.bindings()) {
                if (binding.pin() != null) {
                    symbolPorts.put(binding.input(), binding.pin());
                }
            }
        }
        DrawingBindings drawn = portrait == null || component.portrait() == null ? null
                : DrawingBindings.of(component.head().title(), component.portrait(), portrait, names);
        List<Figure> readings = nearest(nearestFirst, base -> base.readings().isEmpty() ? null : base.readings(), List.of());
        List<Figure> display = nearest(nearestFirst, base -> base.display().isEmpty() ? null : base.display(), List.of());
        List<Lead> leads = nearest(nearestFirst, base -> base.leads().isEmpty() ? null : base.leads(), List.of());
        List<Words> texts = nearestFirst.stream().flatMap(base -> base.parameters().stream())
                .filter(parameter -> parameter.value() instanceof Text)
                .collect(Collectors.toMap(Parameter::name, PartFace::words, (nearer, farther) -> nearer, LinkedHashMap::new))
                .values().stream().toList();
        return new PartFace(designator, value, box, symbolPorts, drawn,
                            readings.stream().map(figure -> new Measure(figure.name(), figure.unit(), figure.signal())).toList(),
                            display.stream().map(figure -> new Display(figure.name(), figure.unit(), figure.caption())).toList(),
                            leads.stream().collect(Collectors.toMap(Lead::pin, lead -> new Look(lead.colour(), lead.stripes()),
                                                                    (first, second) -> first, LinkedHashMap::new)),
                            texts);
    }

    private static Words words(Parameter parameter) {
        return new Words(parameter.name(), ((Text) parameter.value()).text(), parameter.description());
    }

    /** The first value the components write, nearest first; {@code otherwise} when none writes one. */
    private static <T> T nearest(List<Component> nearestFirst, Function<Component, T> facet, T otherwise) {
        for (Component base : nearestFirst) {
            T written = facet.apply(base);
            if (written != null) {
                return written;
            }
        }
        return otherwise;
    }
}
