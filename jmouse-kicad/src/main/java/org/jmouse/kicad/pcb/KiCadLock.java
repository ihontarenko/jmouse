package org.jmouse.kicad.pcb;

/** Whether KiCad holds an item of a board where it stands: {@code locked} — KiCad 7's bare word or KiCad 8's {@code (locked yes)}. */
public enum KiCadLock {

    /** Moved by every tool. */
    FREE,

    /** Held where it stands until it is unlocked. */
    LOCKED
}
