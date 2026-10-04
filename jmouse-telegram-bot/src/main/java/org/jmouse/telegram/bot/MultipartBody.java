package org.jmouse.telegram.bot;

import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramRefusal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A {@code multipart/form-data} body, assembled by hand.
 *
 * <p>The JDK's HTTP client has no multipart publisher, and the alternative — adding a client library
 * that does — would be a large dependency for one wire format. This is about ninety lines and has no
 * transitive cost at all.
 *
 * <p>⚠️ <strong>Bytes throughout, never a string.</strong> A body that mixes UTF-8 text with the
 * contents of a JPEG cannot be assembled as a {@link String}: the file bytes are not valid text in any
 * charset, and converting them corrupts the upload in a way that fails at Telegram rather than here.
 *
 * <p>⚠️ The 50 MB upload ceiling is <em>not</em> enforced here. This class cannot tell Telegram's own
 * endpoint from a self-hosted {@code telegram-bot-api} server that legitimately accepts 2 GB, and only
 * the identity knows which is configured — so the check belongs in {@link BotApiTransport}, which has
 * one.
 */
final class MultipartBody {

    private final String                boundary = "jmouse" + UUID.randomUUID().toString().replace("-", "");
    private final ByteArrayOutputStream buffer   = new ByteArrayOutputStream();

    private boolean finished;

    /** A plain form field. */
    MultipartBody field(String name, String value) {
        if (value == null) {
            return this;
        }

        writeAscii("--" + boundary + "\r\n");
        writeUtf8("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        writeUtf8(value);
        writeAscii("\r\n");

        return this;
    }

    /** A file part. */
    MultipartBody file(String name, String fileName, String contentType, byte[] content) {
        writeAscii("--" + boundary + "\r\n");
        writeUtf8("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n"
                .formatted(name, escapeFileName(fileName)));
        writeAscii("Content-Type: " + (contentType == null ? "application/octet-stream" : contentType) + "\r\n\r\n");
        writeBytes(content);
        writeAscii("\r\n");

        return this;
    }

    /** A file part read from disk. */
    MultipartBody file(String name, Path path, String contentType) {
        byte[] content;

        try {
            content = Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new TelegramException(new TelegramRefusal.TransportFailure(
                    "the file to upload could not be read: " + path), exception);
        }

        String probed = contentType;

        if (probed == null) {
            try {
                probed = Files.probeContentType(path);
            } catch (IOException ignored) {
                // Not knowing the type is not a failure - Telegram infers one from the file name.
                probed = null;
            }
        }

        return file(name, path.getFileName().toString(), probed, content);
    }

    String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    byte[] content() {
        if (!finished) {
            writeAscii("--" + boundary + "--\r\n");
            finished = true;
        }

        return buffer.toByteArray();
    }

    /**
     * ⚠️ A quote or a newline in a file name would end the header early and corrupt every part after
     * it. Names arrive from users and from disk, so neither can be assumed clean.
     */
    private static String escapeFileName(String fileName) {
        return fileName.replace("\"", "%22").replace("\r", "").replace("\n", "");
    }

    private void writeAscii(String text) {
        writeBytes(text.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeUtf8(String text) {
        writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private void writeBytes(byte[] bytes) {
        try {
            buffer.write(bytes);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
