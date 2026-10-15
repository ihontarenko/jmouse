package org.jmouse.telegram;

import java.util.Objects;

/**
 * A file Telegram holds and is ready to hand over: what {@code getFile} answers.
 *
 * <p>⚠️ <strong>The path is short-lived and secret-adjacent.</strong> Telegram guarantees it for about an
 * hour, and the address it makes carries the bot token — which is why this record holds the path alone and
 * the transport builds the address only at the moment it downloads.
 *
 * <p>⚠️ A bot may download at most 20 MB through the public Bot API. A larger file answers {@code getFile}
 * with a refusal, not with a path; a local Bot API server lifts the limit.
 *
 * @param fileId       what was asked for
 * @param fileUniqueId stable across bots, or {@code null}
 * @param sizeBytes    the size Telegram reports, or {@code -1}
 * @param path         where the transport downloads it from, relative to the identity's file address
 */
public record TelegramFile(String fileId, String fileUniqueId, long sizeBytes, String path) {

    public TelegramFile {
        Objects.requireNonNull(fileId, "file id");
        Objects.requireNonNull(path, "path");
    }
}
