package org.jmouse.kicad.pcb;

/**
 * How an item is to stand on a board the builder writes: the group it is put in — empty for none —, and whether it is locked.
 *
 * @param group the name of a group the builder was told of with {@link KiCadPcbBuilder#group}; empty for none
 * @param lock  whether KiCad holds it where it stands
 */
public record KiCadArrangement(String group, KiCadLock lock) {

    /** In no group, free: what an item is when nothing is said of it. */
    public static final KiCadArrangement FREE = new KiCadArrangement("", KiCadLock.FREE);

    public KiCadArrangement {
        group = group == null ? "" : group;
        lock = lock == null ? KiCadLock.FREE : lock;
    }
}
