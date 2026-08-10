package org.jmouse.access;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The questions this installation asks of a request, in the order it asks them.
 *
 * <p>The counterpart of {@link ScopeCatalog} on the other half of the model: that one says what
 * <em>places</em> exist, this one says what <em>questions</em> exist. Both used to be enums the engine
 * read directly, and both are now handed to it, which is the difference between a subsystem and
 * something a second product could use.
 *
 * <p>The engine needs the declared set — not merely the beans it found — for one reason: it can only
 * notice that nothing answers the permission axis if it knows the permission axis was supposed to be
 * answered. A registry that inferred the vocabulary from the registrations could never report the
 * absence, and an unanswered axis is a question silently answered yes.
 *
 * @see AxisKind
 */
public final class AxisCatalog {

    private final List<AxisKind> inOrder;

    public AxisCatalog(List<AxisKind> axes) {
        this.inOrder = axes.stream()
                .sorted(Comparator.comparingInt(AxisKind::order))
                .toList();

        requireDistinctNames(inOrder);
        requireDistinctOrder(inOrder);
        requireAtLeastOneRequired(inOrder);
    }

    /** Every axis this installation declares, outermost first. */
    public List<AxisKind> declared() {
        return inOrder;
    }

    /** How two axes sort — the declared order, which is the running order. */
    public Comparator<AxisKind> outermostFirst() {
        return Comparator.comparingInt(AxisKind::order);
    }

    private static void requireDistinctNames(List<AxisKind> axes) {
        Set<String> seen = new LinkedHashSet<>();

        for (AxisKind axis : axes) {
            if (!seen.add(axis.name())) {
                throw new IllegalArgumentException(
                        "Two axes are both called " + axis.name() + ". A refusal names the axis that "
                        + "produced it, so two axes with one name make a verdict unreadable.");
            }
        }
    }

    private static void requireDistinctOrder(List<AxisKind> axes) {
        for (int position = 1; position < axes.size(); position++) {
            AxisKind earlier = axes.get(position - 1);
            AxisKind later   = axes.get(position);

            if (earlier.order() == later.order()) {
                throw new IllegalArgumentException(
                        earlier.name() + " and " + later.name() + " share order " + earlier.order()
                        + ". The order has to be total, because a verdict is only worth reading if the "
                        + "axis it names is reliably the outermost reason.");
            }
        }
    }

    private static void requireAtLeastOneRequired(List<AxisKind> axes) {
        boolean anyRequired = axes.stream().anyMatch(AxisKind::required);

        if (!anyRequired) {
            throw new IllegalArgumentException(
                    "No declared axis is required. At least one has to be, or an installation whose "
                    + "axis beans all failed to register would start up granting everything.");
        }
    }
}
