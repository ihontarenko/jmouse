package org.jmouse.formula;

import java.util.HashMap;
import java.util.Map;

/**
 * Which slot of the evaluation frame each name reads while a formula is compiled. The top level is the caller's
 * {@link Symbols}; a binder adds one name on a slot of its own, shadowing an outer name of the same spelling.
 * A defined function's body is compiled from {@link #topLevel()}, which is what keeps it lexically scoped.
 */
final class Scope {

    private final Map<String, Integer> slots;
    private final Scope root;

    private Scope(Map<String, Integer> slots, Scope root) {
        this.slots = slots;
        this.root = root == null ? this : root;
    }

    static Scope of(Symbols symbols) {
        Map<String, Integer> slots = new HashMap<>();
        for (String name : symbols.names()) {
            slots.put(name, symbols.slotOf(name));
        }
        return new Scope(slots, null);
    }

    static Scope empty() {
        return new Scope(Map.of(), null);
    }

    Scope bind(String name, int slot) {
        Map<String, Integer> extended = new HashMap<>(slots);
        extended.put(name, slot);
        return new Scope(extended, root);
    }

    Scope topLevel() {
        return root;
    }

    int slotOf(String name) {
        Integer slot = slots.get(name);
        if (slot == null) {
            throw new IllegalStateException("'" + name + "' has no slot; the symbol check should have refused it.");
        }
        return slot;
    }
}
