package org.jmouse.altium;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A pin as a newer Altium writes it — not as text but in a binary record of its own — read into the properties its text
 * record would have, so the rest of a reading takes both alike. The fields stand in a fixed order; a string is a length
 * in one byte and its bytes.
 */
final class AltiumBinaryPin {

    private static final Charset ANSI = Charset.forName("windows-1252");
    private static final int PIN = 2;

    private AltiumBinaryPin() {
    }

    static Map<String, String> properties(byte[] data) {
        ByteBuffer read = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        Map<String, String> properties = new LinkedHashMap<>();
        int record = read.getInt();
        if (record != PIN) {
            throw new AltiumReadException("A binary record of kind " + record + ": Altium writes only pins so.");
        }
        properties.put("RECORD", String.valueOf(record));
        read.get();
        properties.put("OWNERPARTID", String.valueOf(read.getShort()));
        properties.put("OWNERPARTDISPLAYMODE", String.valueOf(read.get()));
        properties.put("SYMBOL_INNEREDGE", String.valueOf(Byte.toUnsignedInt(read.get())));
        properties.put("SYMBOL_OUTEREDGE", String.valueOf(Byte.toUnsignedInt(read.get())));
        properties.put("SYMBOL_INNER", String.valueOf(Byte.toUnsignedInt(read.get())));
        properties.put("SYMBOL_OUTER", String.valueOf(Byte.toUnsignedInt(read.get())));
        properties.put("TEXT", text(read));
        read.get();
        properties.put("ELECTRICAL", String.valueOf(Byte.toUnsignedInt(read.get())));
        properties.put("PINCONGLOMERATE", String.valueOf(Byte.toUnsignedInt(read.get())));
        properties.put("PINLENGTH", String.valueOf(read.getShort()));
        properties.put("LOCATION.X", String.valueOf(read.getShort()));
        properties.put("LOCATION.Y", String.valueOf(read.getShort()));
        properties.put("COLOR", String.valueOf(read.getInt()));
        properties.put("NAME", text(read));
        properties.put("DESIGNATOR", text(read));
        return properties;
    }

    private static String text(ByteBuffer read) {
        int length = Byte.toUnsignedInt(read.get());
        byte[] bytes = new byte[length];
        read.get(bytes);
        return new String(bytes, ANSI);
    }
}
