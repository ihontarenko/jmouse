package org.jmouse.cad.drawing;

/**
 * A group of a drawing as an item of its own: its name, by which its members say they are in it, and its state — a locked group
 * holds every member where it stands, whatever each says of itself.
 *
 * @param name  the name its members carry
 * @param state the group's own state
 */
public record DrawnGroup(String name, DrawnState state) {

    public DrawnGroup {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A group is named");
        }
        state = state == null ? DrawnState.FREE : state;
    }

    /** Whether it holds its members where they stand. */
    public boolean locked() {
        return state == DrawnState.LOCKED;
    }
}
