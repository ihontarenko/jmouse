package org.jmouse.telegram.command;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Long answers, split into pages and kept for a while so ‹ › buttons can turn them.
 *
 * <p>⚠️ In memory and bounded: a page turned after a restart or an hour later answers "expired", which is
 * honest and cheap. Every set of pages remembers <em>whose</em> it is and the command that produced it, so
 * a forwarded button pressed by somebody else is refused rather than shown.
 */
final class ReplyPages {

    /** What a page may hold, leaving room for the "2 / 5" line under it. */
    static final int PAGE_LIMIT = 3800;

    private static final int      CAPACITY = 200;
    private static final Duration LIFETIME = Duration.ofHours(1);

    private final SecureRandom             random = new SecureRandom();
    private final Clock                    clock;
    private final Map<String, Stored>      pages  = new LinkedHashMap<>();

    record Stored(String subject, String command, List<String> pages, Instant createdAt) {
    }

    ReplyPages(Clock clock) {
        this.clock = clock;
    }

    /** Keeps the pages and answers the token their buttons carry. */
    synchronized String keep(String subject, String command, List<String> split) {
        forgetExpired();

        String token = token();
        pages.put(token, new Stored(subject, command, List.copyOf(split), clock.instant()));

        while (pages.size() > CAPACITY) {
            Iterator<String> oldest = pages.keySet().iterator();
            oldest.next();
            oldest.remove();
        }

        return token;
    }

    synchronized Optional<Stored> find(String token) {
        forgetExpired();
        return Optional.ofNullable(pages.get(token));
    }

    /**
     * Splits text into pages, at a line break when there is one near the limit, never inside a surrogate
     * pair.
     */
    static List<String> split(String text) {
        List<String> split = new ArrayList<>();
        int          start = 0;

        while (start < text.length()) {
            int end = Math.min(start + PAGE_LIMIT, text.length());

            if (end < text.length()) {
                int lineBreak = text.lastIndexOf('\n', end);

                if (lineBreak > start + PAGE_LIMIT / 2) {
                    end = lineBreak + 1;
                } else if (Character.isHighSurrogate(text.charAt(end - 1))) {
                    end--;
                }
            }

            split.add(text.substring(start, end));
            start = end;
        }

        return split;
    }

    private void forgetExpired() {
        Instant oldestKept = clock.instant().minus(LIFETIME);
        pages.values().removeIf(stored -> stored.createdAt().isBefore(oldestKept));
    }

    /** 9 random bytes, 12 characters — short enough for Telegram's 64-byte callback data. */
    private String token() {
        byte[] bytes = new byte[9];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
