package org.jmouse.compound;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("a compound file read as its storages and streams")
class CompoundFileTest {

    private static final int SECTOR = 512;
    private static final int END = 0xFFFFFFFE;
    private static final int FREE = 0xFFFFFFFF;
    private static final int FAT_SECTOR = 0xFFFFFFFD;
    private static final int NONE = 0xFFFFFFFF;

    /**
     * A version 3 file written by hand: a stream of 5000 bytes in ordinary sectors, one of 100 bytes and one of 10 — in a
     * storage — in the mini stream, and a directory long enough to take two sectors.
     *
     * <pre>
     * sector 0  the FAT         sectors 2-11  "Big"          sector 12  the mini FAT
     * sector 1  directory, 1    sector 13     the mini stream sector 14  directory, 2
     * </pre>
     */
    private static byte[] written(byte[] big, byte[] small, byte[] inner) {
        ByteBuffer file = ByteBuffer.allocate(SECTOR * 16).order(ByteOrder.LITTLE_ENDIAN);
        file.putLong(0, 0xE11AB1A1E011CFD0L);
        file.putShort(0x18, (short) 0x3E);
        file.putShort(0x1A, (short) 3);
        file.putShort(0x1C, (short) 0xFFFE);
        file.putShort(0x1E, (short) 9);
        file.putShort(0x20, (short) 6);
        file.putInt(0x2C, 1);
        file.putInt(0x30, 1);
        file.putInt(0x38, 4096);
        file.putInt(0x3C, 12);
        file.putInt(0x40, 1);
        file.putInt(0x44, END);
        file.putInt(0x4C, 0);
        for (int index = 1; index < 109; index++) {
            file.putInt(0x4C + index * 4, FREE);
        }
        int[] fat = new int[SECTOR / 4];
        Arrays.fill(fat, FREE);
        fat[0] = FAT_SECTOR;
        fat[1] = 14;
        fat[14] = END;
        for (int sector = 2; sector < 11; sector++) {
            fat[sector] = sector + 1;
        }
        fat[11] = END;
        fat[12] = END;
        fat[13] = END;
        for (int index = 0; index < fat.length; index++) {
            file.putInt(sectorOffset(0) + index * 4, fat[index]);
        }
        file.put(sectorOffset(2), big);
        // The mini FAT: "Small" takes mini sectors 0 and 1, "Inner" mini sector 2.
        int[] miniFat = new int[SECTOR / 4];
        Arrays.fill(miniFat, FREE);
        miniFat[0] = 1;
        miniFat[1] = END;
        miniFat[2] = END;
        for (int index = 0; index < miniFat.length; index++) {
            file.putInt(sectorOffset(12) + index * 4, miniFat[index]);
        }
        file.put(sectorOffset(13), small);
        file.put(sectorOffset(13) + 128, inner);
        entry(file, 0, "Root Entry", 5, NONE, NONE, 1, 13, 192);
        entry(file, 1, "Big", 2, NONE, 2, NONE, 2, big.length);
        entry(file, 2, "Small", 2, NONE, 3, NONE, 0, small.length);
        entry(file, 3, "Store", 1, NONE, NONE, 4, 0, 0);
        entry(file, 4, "Inner", 2, NONE, NONE, NONE, 2, inner.length);
        return file.array();
    }

    private static int sectorOffset(int sector) {
        return (sector + 1) * SECTOR;
    }

    private static void entry(ByteBuffer file, int index, String name, int kind, int left, int right, int child, int start,
                              int size) {
        int offset = index < 4 ? sectorOffset(1) + index * 128 : sectorOffset(14) + (index - 4) * 128;
        byte[] written = name.getBytes(StandardCharsets.UTF_16LE);
        file.put(offset, written);
        file.putShort(offset + 0x40, (short) (written.length + 2));
        file.put(offset + 0x42, (byte) kind);
        file.putInt(offset + 0x44, left);
        file.putInt(offset + 0x48, right);
        file.putInt(offset + 0x4C, child);
        file.putInt(offset + 0x74, start);
        file.putInt(offset + 0x78, size);
    }

    private static byte[] filled(int length, int seed) {
        byte[] bytes = new byte[length];
        for (int index = 0; index < length; index++) {
            bytes[index] = (byte) (index * 31 + seed);
        }
        return bytes;
    }

    @Test
    @DisplayName("a stream is read through its chain of sectors, a short one through the mini stream, a storage's by its path")
    void streamsAreRead() {
        byte[] big = filled(5000, 1);
        byte[] small = filled(100, 2);
        byte[] inner = filled(10, 3);
        CompoundFile file = CompoundFile.read(written(big, small, inner));
        assertEquals(List.of("Big", "Small", "Store/Inner"), file.streams());
        assertArrayEquals(big, file.stream("Big").orElseThrow());
        assertArrayEquals(small, file.stream("Small").orElseThrow());
        assertArrayEquals(inner, file.stream("Store/Inner").orElseThrow());
        assertTrue(file.stream("Store").isEmpty());
    }

    @Test
    @DisplayName("what is not one is refused, with why")
    void notOneIsRefused() {
        byte[] text = "|HEADER=Protel for Windows - Schematic Capture Ascii File".getBytes(StandardCharsets.US_ASCII);
        assertFalse(CompoundFile.isCompoundFile(text));
        assertThrows(CompoundFileException.class, () -> CompoundFile.read(text));
    }

    @Test
    @DisplayName("a chain that runs in a circle is refused rather than read for ever")
    void aCircleIsRefused() {
        byte[] file = written(filled(5000, 1), filled(100, 2), filled(10, 3));
        ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN).putInt(sectorOffset(0) + 11 * 4, 2);
        assertThrows(CompoundFileException.class, () -> CompoundFile.read(file));
    }
}
