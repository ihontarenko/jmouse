package org.jmouse.git.local;

/**
 * What sending a branch came to.
 *
 * @param branch the branch sent
 * @param remote where it went, with no credentials in it
 * @param state  what happened there
 */
public record PushOutcome(String branch, String remote, State state) {

    public enum State {
        SENT,
        /** The remote had all of it already. */
        ALREADY_THERE
    }
}
