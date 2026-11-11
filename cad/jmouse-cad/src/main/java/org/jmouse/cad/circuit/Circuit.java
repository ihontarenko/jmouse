package org.jmouse.cad.circuit;

import org.jmouse.cad.DocumentHead;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Parameter;

import java.util.List;
import java.util.Optional;

/**
 * A circuit: its parameters, its parts between named nets — net {@value #GROUND} the reference —, where they are drawn,
 * what the bench probes, what it is expected to show, and how it is run.
 *
 * @param timeBase the oscilloscope's time a division — {@code time 5 ms/div} —, or null
 * @param analysis how it is run, or null where whoever runs it says
 * @param frame    the frame its sheet stands in, by slug — {@code frame iso-a4-landscape-frame} —, or null
 * @param boundary what it hands across its sheet's edge when it is placed as a block
 */
public record Circuit(DocumentHead head, List<Parameter> parameters, List<Part> parts, List<Placement> layout,
                      List<Probe> probes, Formula timeBase, List<Expectation> expectations, Analysis analysis,
                      List<View> views, String frame, SheetBoundary boundary) {

    public static final String GROUND = "0";

    public Circuit {
        parameters = List.copyOf(parameters);
        parts = List.copyOf(parts);
        layout = List.copyOf(layout);
        probes = List.copyOf(probes);
        expectations = List.copyOf(expectations);
        views = List.copyOf(views);
        boundary = boundary == null ? SheetBoundary.NONE : boundary;
    }

    public Optional<Part> part(String reference) {
        return parts.stream().filter(part -> part.reference().equals(reference)).findFirst();
    }

    public Optional<String> framed() {
        return Optional.ofNullable(frame);
    }

    public Optional<Analysis> run() {
        return Optional.ofNullable(analysis);
    }
}
