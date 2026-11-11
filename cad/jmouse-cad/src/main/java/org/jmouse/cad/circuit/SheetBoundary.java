package org.jmouse.cad.circuit;

import java.util.List;

/**
 * What a circuit hands across its sheet's edge when it is placed as a block in another: its ports — each a net and which
 * way it goes, {@code input in} —, and the nets it shares with every sheet of its project by name, {@code vcc global}.
 */
public record SheetBoundary(List<Port> ports, List<Net> nets) {

    /** A circuit that is no sheet of another. */
    public static final SheetBoundary NONE = new SheetBoundary(List.of(), List.of());

    public SheetBoundary {
        ports = List.copyOf(ports);
        nets = List.copyOf(nets);
    }

    /**
     * A net the sheet hands to its parent, a pin of the block that places it.
     *
     * @param direction {@code input}, {@code output}, {@code power}, {@code ground} or {@code analog}
     */
    public record Port(String net, String direction) {
    }

    /**
     * A net named for what it is beyond the sheet.
     *
     * @param ground whether it is the ground everything is measured from
     * @param global whether every sheet of the project shares it by its name
     */
    public record Net(String name, boolean ground, boolean global) {
    }

    public boolean isEmpty() {
        return ports.isEmpty() && nets.isEmpty();
    }
}
