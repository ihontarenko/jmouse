package org.jmouse.telegram.command;

import org.jmouse.storage.Content;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramFile;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.update.Attachment;

import java.util.Objects;

/**
 * The files people send a bot, as jMouse storage content.
 *
 * <pre>{@code
 * for (Attachment attachment : context.attachments()) {
 *     StoredObject stored = context.files().store(attachment, fileStore, StorageKey.of("inbox/" + attachment.fileNameOrDefault()));
 * }
 * }</pre>
 *
 * <p>⚠️ <strong>A {@link Content}, not a byte array.</strong> Storage already knows how to take content — check
 * it against an acceptance policy, digest it, write it to whichever backend is configured — so a file from a chat
 * goes the same way as an upload from a form. Its bytes stream from Telegram when the store reads them, and only
 * then: describing a file costs one call, downloading it costs the file.
 *
 * <p>⚠️ The size is checked twice. Once against what Telegram reports before anything is downloaded, so a file
 * over the ceiling costs no transfer; and storage counts the bytes that really arrive, because a reported size is
 * a claim like any other.
 */
public final class TelegramFiles {

    /** What the public Bot API will hand a bot at most; a local Bot API server allows more. */
    public static final long BOT_API_LIMIT_BYTES = 20L * 1024 * 1024;

    private final TelegramGateway gateway;
    private final long            maximumBytes;

    public TelegramFiles(TelegramGateway gateway) {
        this(gateway, BOT_API_LIMIT_BYTES);
    }

    /**
     * @param gateway      the gateway of the identity that received the files — a {@code file_id} is valid only for it
     * @param maximumBytes the largest file this product accepts from a chat
     */
    public TelegramFiles(TelegramGateway gateway, long maximumBytes) {
        this.gateway      = Objects.requireNonNull(gateway, "gateway");
        this.maximumBytes = maximumBytes;
    }

    /**
     * The attachment as storage content: named, typed as the sender claimed, sized as Telegram reports, and
     * streaming from Telegram when read.
     *
     * @throws TelegramException when Telegram will not offer the file, or it is larger than this product accepts
     */
    public Content contentOf(Attachment attachment) {
        requireWithinLimit(attachment.sizeBytes());

        TelegramFile file = gateway.describeFile(attachment.fileId());

        requireWithinLimit(file.sizeBytes());

        long size = file.sizeBytes() >= 0 ? file.sizeBytes() : attachment.sizeBytes();

        return Content.of(attachment.fileNameOrDefault(), attachment.mimeType(),
                          size >= 0 ? size : Content.UNKNOWN_SIZE, () -> gateway.openFile(file));
    }

    /** Downloads the attachment into a store under the given key — what keeping a sent file takes. */
    public StoredObject store(Attachment attachment, FileStore store, StorageKey key) {
        return store.write(key, contentOf(attachment));
    }

    private void requireWithinLimit(long sizeBytes) {
        if (sizeBytes > maximumBytes) {
            throw new TelegramException(new TelegramRefusal.Rejected(413,
                    "the file is %d bytes, over the %d this bot accepts".formatted(sizeBytes, maximumBytes)));
        }
    }
}
