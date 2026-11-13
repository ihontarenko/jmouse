package org.jmouse.cad.drawing;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Whether a drawn item — a part on a sheet, a footprint, a track or a via on a pcb, a board on a panel, a group of them — may be
 * moved: free, or locked where it stands, the reference others are placed against. One field of one enum, never a flag: a new
 * state is a new constant.
 */
public enum DrawnState {

    /** Moved, turned, flipped and taken away by every tool. */
    FREE("free"),

    /** Stands where it is: no tool, pass or router moves, turns, flips, re-lays or removes it until it is unlocked. */
    LOCKED("locked");

    private final String word;

    DrawnState(String word) {
        this.word = word;
    }

    /** The word a document writes it with. */
    public String word() {
        return word;
    }

    /** Every state's word, in order. */
    public static List<String> words() {
        return Arrays.stream(values()).map(DrawnState::word).toList();
    }

    /** The state a word names; empty for a word no state has. */
    public static Optional<DrawnState> of(String word) {
        return Arrays.stream(values()).filter(state -> state.word.equals(word)).findFirst();
    }
}
