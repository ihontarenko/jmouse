package org.jmouse.altium;

import org.jmouse.compound.CompoundFile;
import org.jmouse.compound.CompoundFileException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The records an Altium schematic is made of, read from either of its forms: a compound file whose {@code FileHeader}
 * stream holds them one after another, each behind four bytes — its length, and whether it is text or Altium's binary
 * pin —, or an ASCII file holding one a line. The first record is the header; the rest are what the file draws.
 */
public final class AltiumRecords {

    /** What the binary file writes its text in, where a key does not say UTF-8. */
    private static final Charset ANSI = Charset.forName("windows-1252");
    private static final String UTF8 = "%UTF8%";
    private static final String STREAM = "FileHeader";
    private static final int TEXT = 0;
    private static final int BINARY_PIN = 1;
    private static final int LENGTH = 0xFFFFFF;

    private AltiumRecords() {
    }

    /**
     * The file's records, the header first.
     *
     * @throws AltiumReadException when the bytes are no Altium schematic, or one broken past reading
     */
    public static List<Map<String, String>> read(String name, byte[] bytes) {
        if (CompoundFile.isCompoundFile(bytes)) {
            try {
                byte[] stream = CompoundFile.read(bytes).stream(STREAM).orElseThrow(() -> new AltiumReadException(
                        name + " is a compound file with no " + STREAM + " stream: not an Altium schematic."));
                return binary(name, stream);
            } catch (CompoundFileException broken) {
                throw new AltiumReadException(name + ": " + broken.getMessage(), broken);
            }
        }
        return ascii(name, bytes);
    }

    private static List<Map<String, String>> binary(String name, byte[] stream) {
        ByteBuffer buffer = ByteBuffer.wrap(stream).order(ByteOrder.LITTLE_ENDIAN);
        List<Map<String, String>> records = new ArrayList<>();
        while (buffer.remaining() >= 4) {
            int header = buffer.getInt();
            int length = header & LENGTH;
            int type = header >>> 24;
            if (length > buffer.remaining()) {
                throw new AltiumReadException(name + ": a record says it is " + length + " bytes and " + buffer.remaining()
                        + " are left.");
            }
            byte[] data = new byte[length];
            buffer.get(data);
            records.add(switch (type) {
                case TEXT -> properties(data);
                case BINARY_PIN -> AltiumBinaryPin.properties(data);
                default -> throw new AltiumReadException(name + ": a record of type " + type + ", which Altium does not write.");
            });
        }
        return records;
    }

    private static List<Map<String, String>> ascii(String name, byte[] bytes) {
        List<Map<String, String>> records = new ArrayList<>();
        int start = 0;
        for (int at = 0; at <= bytes.length; at++) {
            if (at == bytes.length || bytes[at] == '\n' || bytes[at] == '\r') {
                if (at > start && bytes[start] == '|') {
                    byte[] line = new byte[at - start];
                    System.arraycopy(bytes, start, line, 0, line.length);
                    records.add(properties(line));
                }
                start = at + 1;
            }
        }
        if (records.isEmpty() || !records.getFirst().containsKey("HEADER")) {
            throw new AltiumReadException(name + " is no Altium schematic: it opens with no |HEADER= record.");
        }
        return records;
    }

    /**
     * {@code |KEY=VALUE|KEY=VALUE}: split on the bars as bytes, each value read in Windows-1252 unless its key says it is
     * UTF-8 — then it stands for the key without the mark.
     */
    static Map<String, String> properties(byte[] data) {
        Map<String, String> properties = new LinkedHashMap<>();
        Map<String, String> unicode = new LinkedHashMap<>();
        int start = 0;
        for (int at = 0; at <= data.length; at++) {
            if (at == data.length || data[at] == '|' || data[at] == 0) {
                pair(data, start, at, properties, unicode);
                start = at + 1;
            }
        }
        properties.putAll(unicode);
        return properties;
    }

    private static void pair(byte[] data, int start, int end, Map<String, String> properties, Map<String, String> unicode) {
        int equals = -1;
        for (int at = start; at < end; at++) {
            if (data[at] == '=') {
                equals = at;
                break;
            }
        }
        if (equals <= start) {
            return;
        }
        String key = new String(data, start, equals - start, StandardCharsets.US_ASCII);
        if (key.startsWith(UTF8)) {
            unicode.put(key.substring(UTF8.length()), new String(data, equals + 1, end - equals - 1, StandardCharsets.UTF_8));
        } else {
            properties.put(key, new String(data, equals + 1, end - equals - 1, ANSI));
        }
    }
}
