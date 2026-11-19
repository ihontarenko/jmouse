package org.jmouse.git.local;

/**
 * What bringing another branch into the one in hand came to.
 *
 * @param branch the branch brought in
 * @param into   the branch it was brought into, the one in hand
 * @param state  what happened
 * @param head   the commit the branch in hand stands at now
 */
public record MergeOutcome(String branch, String into, State state, String head) {

    public enum State {
        /** The branch in hand had nothing of its own: it now stands where the other does. */
        FAST_FORWARD,
        /** The two went two ways and were brought together by a merge commit. */
        MERGED,
        /** The branch in hand had all of it already. */
        ALREADY_HERE
    }
}
