package org.jmouse.cad.circuit;

import org.jspecify.annotations.Nullable;
import org.jmouse.cad.DocumentHead;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Parameter;
import org.jmouse.cad.drawing.ArrangedDrawing;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnGroups;
import org.jmouse.cad.drawing.StandingItem;

import java.util.List;
import java.util.Optional;

/**
 * A circuit: its parameters, its parts between named nets — net {@value #GROUND} the reference —, where they are drawn,
 * what the bench probes, what it is expected to show, and how it is run.
 *
 * @param head         what the document says of itself: its slug, its title, its role and its meta fields
 * @param parameters   its parameters, which its parts' settings may name
 * @param parts        its parts, each between named nets
 * @param layout       where each part, wire, mark, note and drawing stands on its sheet
 * @param probes       the channels the bench's oscilloscope reads
 * @param timeBase     the oscilloscope's time a division — {@code time 5 ms/div} —, or null
 * @param expectations what it says it does, to be checked by running it
 * @param analysis     how it is run, or null where whoever runs it says
 * @param views        the views it is shown in: a run in time, a sweep
 * @param frame        the frame its sheet stands in, by slug — {@code frame iso-a4-landscape-frame} —, or null
 * @param boundary     what it hands across its sheet's edge when it is placed as a block
 * @param buses        its buses: the named sets of its nets that travel together, drawn by its bus lines
 * @param groups       the groups its layout's items are chosen and moved with, each with its own state
 */
public record Circuit(DocumentHead head, List<Parameter> parameters, List<Part> parts, List<Placement> layout,
                      List<Probe> probes, @Nullable Formula timeBase, List<Expectation> expectations, @Nullable Analysis analysis,
                      List<View> views, @Nullable String frame, SheetBoundary boundary, List<Bus> buses, List<DrawnGroup> groups) implements ArrangedDrawing {

    public static final String GROUND = "0";

    public Circuit {
        parameters = List.copyOf(parameters);
        parts = List.copyOf(parts);
        layout = List.copyOf(layout);
        probes = List.copyOf(probes);
        expectations = List.copyOf(expectations);
        views = List.copyOf(views);
        boundary = boundary == null ? SheetBoundary.NONE : boundary;
        buses = buses == null ? List.of() : List.copyOf(buses);
        if (buses.stream().map(Bus::name).distinct().count() != buses.size()) {
            throw new IllegalArgumentException("A circuit names each of its buses once: " + String.join(", ", buses.stream().map(Bus::name).toList()));
        }
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    /** Its layout's items as a lock looks at them: each named by its reference, apart from how it stands. */
    @Override
    public List<StandingItem> standingItems() {
        return layout.stream().map(placement -> new StandingItem(placement instanceof Placement.PlacedPart part ? part.placedName() : placement.reference(),
                placement.arranged(Arrangement.FREE), placement.arrangement())).toList();
    }

    @Override
    public DrawnGroups drawnGroups() {
        return new DrawnGroups(groups);
    }

    /** Its bus of a name. */
    public Optional<Bus> bus(String name) {
        return buses.stream().filter(bus -> bus.name().equals(name)).findFirst();
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
