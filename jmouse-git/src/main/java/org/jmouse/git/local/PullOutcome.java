package org.jmouse.git.local;

/**
 * What taking a branch from its remote came to.
 *
 * @param branch the branch
 * @param remote where it was taken from, with no credentials in it
 * @param state  what happened here
 */
public record PullOutcome(String branch, String remote, State state) {

    public enum State {
        TAKEN,
        /** It had all of it already. */
        ALREADY_HERE
    }
}
