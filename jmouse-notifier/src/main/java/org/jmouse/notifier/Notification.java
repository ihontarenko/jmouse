package org.jmouse.notifier;

import java.util.ArrayList;
import java.util.List;

/**
 * What to say — with no destination.
 *
 * <p>⚠️ <strong>It carries no recipient and no sender.</strong> One notification goes to fifty people,
 * so a destination inside it would mean rebuilding the body per recipient; and the sender is decided by
 * the code that publishes, never by whoever composed the text (see {@link Sender}).
 *
 * @param title an optional heading, or {@code null}
 * @param body  the text — never blank: a notification with nothing to say is worse than none
 * @param level how much it matters
 * @param links where a reader can go next, in order; a channel that cannot show them may drop them
 */
public record Notification(String title, String body, Level level, List<Link> links) {

    public Notification {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("A notification needs a body; a blank one says nothing");
        }

        if (level == null) {
            throw new IllegalArgumentException("A notification needs a level");
        }

        title = title == null || title.isBlank() ? null : title.strip();
        body  = body.strip();
        links = links == null ? List.of() : List.copyOf(links);
    }

    /** Worth knowing; silent. */
    public static Notification info(String body) {
        return new Notification(null, body, Level.INFO, List.of());
    }

    /** Needs attention soon; audible. */
    public static Notification warn(String body) {
        return new Notification(null, body, Level.WARN, List.of());
    }

    /** Something is wrong now; audible. */
    public static Notification alarm(String body) {
        return new Notification(null, body, Level.ALARM, List.of());
    }

    /** The same notification under a heading. */
    public Notification titled(String heading) {
        return new Notification(heading, body, level, links);
    }

    /** The same notification with one more link at the end. */
    public Notification linking(String label, String url) {
        List<Link> extended = new ArrayList<>(links);
        extended.add(new Link(label, url));
        return new Notification(title, body, level, extended);
    }

    public boolean hasTitle() {
        return title != null;
    }

    /**
     * Somewhere a reader can go from a notification.
     *
     * @param label what the reader sees
     * @param url   an absolute address — ⚠️ some channels refuse addresses they cannot open from a
     *              phone, such as {@code localhost}; that is the channel's refusal to report, not this
     *              record's to predict
     */
    public record Link(String label, String url) {

        public Link {
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("A link needs a label");
            }

            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("A link needs an address");
            }

            label = label.strip();
            url   = url.strip();
        }
    }
}
