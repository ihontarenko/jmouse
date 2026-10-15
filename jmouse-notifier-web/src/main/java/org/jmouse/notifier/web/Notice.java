package org.jmouse.notifier.web;

import org.jmouse.notifier.Level;
import org.jmouse.notifier.Notification;

import java.time.Instant;
import java.util.List;

/**
 * One notification as a screen shows it — the shape a collect route answers with.
 *
 * @param id          stable, so a client can de-duplicate across reconnects
 * @param subject     whose it is
 * @param deviceId    the one device it is for, or {@code null} for every device of the subject
 * @param at          when it was published
 * @param level       how much it matters — ⚠️ a client sounds {@code WARN} and {@code ALARM} only
 * @param title       a heading, or {@code null}
 * @param body        the text
 * @param senderLabel who is speaking, already composed — every client says it the same way
 * @param links       where the reader can go next
 */
public record Notice(
        String                  id,
        String                  subject,
        String                  deviceId,
        Instant                 at,
        Level                   level,
        String                  title,
        String                  body,
        String                  senderLabel,
        List<Notification.Link> links
) {

    public Notice {
        links = links == null ? List.of() : List.copyOf(links);
    }

    /** Whether a device should be shown it. */
    public boolean isFor(String subjectAsking, String deviceAsking) {
        return subject.equals(subjectAsking) && (deviceId == null || deviceId.equals(deviceAsking));
    }
}
