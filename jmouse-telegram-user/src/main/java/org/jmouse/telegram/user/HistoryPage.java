package org.jmouse.telegram.user;

import org.jmouse.telegram.update.IncomingMessage;

import java.util.List;

/**
 * A slice of a chat's history, newest first.
 *
 * @param messages        this slice
 * @param olderThan       the message id to pass for the next, older slice — or {@code null} when the
 *                        beginning of the chat was reached
 */
public record HistoryPage(List<IncomingMessage> messages, Integer olderThan) {

    public HistoryPage {
        messages = List.copyOf(messages);
    }

    public boolean reachedTheBeginning() {
        return olderThan == null;
    }
}
