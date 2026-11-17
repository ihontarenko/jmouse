package org.jmouse.cad.circuit;

import org.jmouse.cad.Index;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * A bus of a circuit: a named set of its nets, drawn as one thick line and handed across a sheet's edge as one port. It joins
 * nothing by itself — a part's pin is on a member by naming the member's net —; it says which nets travel together.
 * <ul>
 *     <li>a vector, {@code bus(D[0..7])}: its nets are the language's indexed names of it, {@code D_0} … {@code D_7};</li>
 *     <li>a group, {@code bus(I2C, members: [SDA, SCL])}: the nets it names, in their order.</li>
 * </ul>
 *
 * @param name    what it is called — {@code D}, {@code I2C} —, unique among the circuit's buses
 * @param index   a vector's numbers, {@code [0..7]}; null for a group
 * @param members its nets, in order: a vector's from its index, a group's as written
 */
public record Bus(String name, @Nullable Index index, List<String> members) {

    public Bus {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A bus is called something");
        }
        members = index == null ? List.copyOf(members) : index.names(name);
        if (members.isEmpty()) {
            throw new IllegalArgumentException("The bus " + name + " carries no net: a vector says its numbers, a group names its nets");
        }
        if (new LinkedHashSet<>(members).size() != members.size()) {
            throw new IllegalArgumentException("The bus " + name + " names a net twice: " + String.join(", ", members));
        }
    }

    /** A vector, {@code bus(D[0..7])}: its nets {@code D_0} … {@code D_7}. */
    public static Bus vector(String name, Index index) {
        return new Bus(name, index, List.of());
    }

    /** A group, {@code bus(I2C, members: [SDA, SCL])}: the nets it names. */
    public static Bus group(String name, List<String> members) {
        return new Bus(name, null, members);
    }

    /** Its numbers, when it is a vector. */
    public Optional<Index> numbers() {
        return Optional.ofNullable(index);
    }

    /** How many nets it carries: what a block's bus pin must match on the sheet above. */
    public int size() {
        return members.size();
    }

    /** Whether a net travels in it. */
    public boolean carries(String net) {
        return members.contains(net);
    }
}
