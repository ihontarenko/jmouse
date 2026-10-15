package org.jmouse.notifier.web;

import java.util.List;

/**
 * Where screen notices wait to be collected.
 *
 * <p>{@link MemoryInbox} is here — Kitsu's {@code NoticeBoard} generalised. A notice is "say this now":
 * one nobody collected within the inbox's lifetime is not worth showing an hour later.
 */
public interface Inbox {

    void deposit(Notice notice);

    /**
     * What this device has not collected yet — and marks it collected for this device.
     *
     * <p>⚠️ <strong>Once per device, not once per person.</strong> The television and the browser upstairs
     * both see a notice for "everyone"; the same television asking again does not see it twice.
     *
     * @param subject  who is asking — ⚠️ the product's authenticated caller, never a request parameter
     * @param deviceId which of their screens; {@code null} is treated as one unnamed device
     * @return oldest first
     */
    List<Notice> collect(String subject, String deviceId);

    /**
     * The most recent notices for a subject, collected or not — a notification centre's list. Marks nothing.
     */
    List<Notice> recent(String subject, int limit);
}
