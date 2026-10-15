package org.jmouse.telegram.command;

/**
 * The product's word on whether a bound person may run a command — asked before every handler, and again
 * before every page of a long answer is shown.
 *
 * <p>The library never decides who may do what. A product using {@code jmouse-access} answers from the
 * subject's grants; a single-person installation may answer {@link #everyone()}.
 */
@FunctionalInterface
public interface CommandGuard {

    boolean permits(String subject, String command);

    /** Every bound person may run every command — ⚠️ binding is then the whole of the authorization. */
    static CommandGuard everyone() {
        return (subject, command) -> true;
    }
}
