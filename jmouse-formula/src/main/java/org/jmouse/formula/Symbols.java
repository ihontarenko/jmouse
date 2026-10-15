package org.jmouse.formula;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The names an expression may mention and the slot each one reads from. A host's own {@link Formula.Slot slots} come
 * first, when it reserves any; then the variables (x, y, t…); then the object's parameters (a, σ, ρ…). A compiled
 * function reads one array laid out in this order.
 */
public final class Symbols {

    private final Map<String, Integer> slots;
    private final List<String> names;
    private final int reserved;

    private Symbols(int reserved, List<String> names) {
        if (reserved < 0) {
            throw new IllegalArgumentException("A host reserves no fewer than zero slots.");
        }
        this.reserved = reserved;
        this.names = List.copyOf(names);
        this.slots = new LinkedHashMap<>();
        for (int index = 0; index < names.size(); index++) {
            if (slots.putIfAbsent(names.get(index), reserved + index) != null) {
                throw new IllegalArgumentException("The symbol '" + names.get(index) + "' is declared twice.");
            }
        }
    }

    private Symbols(List<String> names) {
        this(0, names);
    }

    /**
     * A table whose first {@code reserved} positions belong to the host's slots — values it computes and binds by
     * position, such as a circuit's node voltages — followed by the named variables and parameters.
     */
    public static Symbols withSlots(int reserved, List<String> variables, List<String> parameters) {
        List<String> all = new ArrayList<>(variables);
        all.addAll(parameters);
        return new Symbols(reserved, all);
    }

    public static Symbols of(List<String> variables, List<String> parameters) {
        List<String> all = new ArrayList<>(variables);
        all.addAll(parameters);
        return new Symbols(all);
    }

    public static Symbols of(String... variables) {
        return new Symbols(List.of(variables));
    }

    public int slotOf(String name) {
        Integer slot = slots.get(name);
        return slot == null ? -1 : slot;
    }

    public int size() {
        return reserved + names.size();
    }

    /** How many positions at the front belong to the host's slots. */
    public int reserved() {
        return reserved;
    }

    public List<String> names() {
        return names;
    }

    public Set<String> declared() {
        return slots.keySet();
    }

    /**
     * Refuses an expression that mentions a name this table does not declare, naming it; a slot outside the reserved
     * positions; or a host call its host has not bound.
     */
    public void requireDeclares(Formula expression, String source) {
        if (expression.mentionsHostCall()) {
            throw new FormulaException("The expression still holds a call its host has to bind first.", source, -1);
        }
        for (int slot : expression.slots()) {
            if (slot >= reserved) {
                throw new FormulaException("Slot " + slot + " is not one of the " + reserved + " the host reserved.",
                        source, -1);
            }
        }
        for (String variable : expression.variables()) {
            if (!slots.containsKey(variable)) {
                throw new FormulaException("'" + variable + "' is not defined here. Known names: "
                        + String.join(", ", names) + ". Multiplication of single letters needs an operator or a space: x*y.",
                        source, source == null ? -1 : Math.max(-1, source.indexOf(variable)));
            }
        }
    }
}
