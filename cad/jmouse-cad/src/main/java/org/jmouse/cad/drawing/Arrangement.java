package org.jmouse.cad.drawing;

/**
 * How a drawn item stands among the others: the group it is chosen and moved with — empty for none —, and its own state. An item
 * is held where it stands when it is locked itself or its group is ({@link DrawnGroups#holds}).
 *
 * @param group the group's name; empty when it is in none
 * @param state its own state
 */
public record Arrangement(String group, DrawnState state) {

    /** In no group, free: what an item is when its line says nothing of it. */
    public static final Arrangement FREE = new Arrangement("", DrawnState.FREE);

    public Arrangement {
        group = group == null ? "" : group;
        state = state == null ? DrawnState.FREE : state;
    }

    /** Free, in a group. */
    public static Arrangement in(String group) {
        return new Arrangement(group, DrawnState.FREE);
    }

    /** Whether it is in a group. */
    public boolean grouped() {
        return !group.isEmpty();
    }

    /** Whether it is locked itself — its group aside. */
    public boolean locked() {
        return state == DrawnState.LOCKED;
    }

    /** The same, in another group; empty takes it out of any. */
    public Arrangement inGroup(String group) {
        return new Arrangement(group, state);
    }

    /** The same, in another state. */
    public Arrangement withState(DrawnState state) {
        return new Arrangement(group, state);
    }

    /** Whether there is nothing to write: in no group, free. */
    public boolean isDefault() {
        return equals(FREE);
    }
}
