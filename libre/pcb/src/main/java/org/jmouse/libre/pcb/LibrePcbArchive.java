package org.jmouse.libre.pcb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A project packed into one {@code .lppz}: a zip of its folder. What it holds is read as the folder's files would be, its text
 * as text and its models as bytes, by their path inside the project.
 *
 * @param texts  the files of text, by path
 * @param models the 3D models, by path
 */
public record LibrePcbArchive(Map<String, String> texts, Map<String, byte[]> models) {

    /** @throws LibrePcbReadException when the bytes are no zip, or hold no project */
    public static LibrePcbArchive of(byte[] bytes) {
        Map<String, String> texts = new LinkedHashMap<>();
        Map<String, byte[]> models = new LinkedHashMap<>();
        try (ZipInputStream entries = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            for (ZipEntry entry = entries.getNextEntry(); entry != null; entry = entries.getNextEntry()) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (LibrePcbFile.isModel(entry.getName())) {
                    models.put(entry.getName(), entries.readAllBytes());
                } else if (LibrePcbFile.isRead(entry.getName())) {
                    texts.put(entry.getName(), new String(entries.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException unread) {
            throw new LibrePcbReadException("The archive does not read as a zip of a project: " + unread.getMessage(), unread);
        }
        if (texts.isEmpty()) {
            throw new LibrePcbReadException("The archive holds no project's files.");
        }
        return new LibrePcbArchive(texts, models);
    }
}
