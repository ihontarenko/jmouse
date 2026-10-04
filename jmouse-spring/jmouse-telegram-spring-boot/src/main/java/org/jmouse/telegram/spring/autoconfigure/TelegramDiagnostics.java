package org.jmouse.telegram.spring.autoconfigure;

import org.jmouse.telegram.spi.TelegramTransport;
import org.jmouse.telegram.spring.TelegramSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * One line at startup saying what was actually built.
 *
 * <p>⚠️ It exists because the failure it detects is <strong>silence</strong>. When autoconfiguration
 * does not register — a missing {@code .imports} entry, an exclusion, a classpath that lacks a
 * conditional class — nothing is logged and nothing throws. The application starts, and the first
 * symptom is a notification that never arrives, hours later, with no trace of why.
 *
 * <p>So: if this line is missing from the startup log, the autoconfiguration did not run, and that is
 * the fact worth knowing before reading any Java.
 *
 * <p>⚠️ It names the identities and never their tokens.
 */
public class TelegramDiagnostics {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramDiagnostics.class);

    public TelegramDiagnostics(TelegramSettings settings, List<TelegramTransport> transports) {
        String kinds = transports.stream()
                .map(transport -> transport.kind().name())
                .collect(Collectors.joining(", "));

        String purposes = settings.identities().isEmpty()
                ? "none configured"
                : String.join(", ", settings.identities().keySet());

        LOGGER.info("jmouse-telegram ready: transports=[{}], purposes=[{}], updates={}, pacing={}",
                kinds, purposes, settings.updates().mode(),
                settings.pace().enabled()
                        ? "%d/s global, %d/min per chat".formatted(
                                settings.pace().globalPerSecond(), settings.pace().perChatPerMinute())
                        : "off");

        // ⚠️ Worth a warning rather than a failure: an application may legitimately start before its
        // token is provisioned, and refusing to boot would make Telegram a hard dependency of
        // everything else the product does.
        if (settings.identities().isEmpty()) {
            LOGGER.warn("no Telegram identity is configured; every call will refuse. "
                        + "Set jmouse.telegram.identities.general.token");
        }
    }
}
