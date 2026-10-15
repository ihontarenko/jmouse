package org.jmouse.telegram.command;

/**
 * What a command does.
 *
 * <p>⚠️ Runs off the polling thread, on the router's executor — it may take its time. Anything it throws
 * is logged and answered with one sentence; the person never sees a stack trace.
 */
@FunctionalInterface
public interface CommandHandler {

    void handle(CommandContext context) throws Exception;
}
