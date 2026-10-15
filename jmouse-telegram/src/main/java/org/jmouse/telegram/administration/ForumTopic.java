package org.jmouse.telegram.administration;

import org.jmouse.telegram.ChatReference;

/**
 * A topic in a forum supergroup.
 *
 * @param topic the chat, in the topic's thread — what a send to the topic addresses
 * @param name  its name
 */
public record ForumTopic(ChatReference topic, String name) {

    public ForumTopic {
        if (topic == null || !topic.hasThread()) {
            throw new IllegalArgumentException("A forum topic is a chat reference with a thread");
        }
    }

    public int threadId() {
        return topic.threadId();
    }
}
