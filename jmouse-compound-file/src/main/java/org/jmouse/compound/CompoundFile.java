package org.jmouse.compound;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A Microsoft compound file — OLE2, [MS-CFB] — read whole: a little file system in one file, its storages folders and
 * its streams files. What it holds is reached by path, a storage's name and a slash before a stream's:
 * {@code FileHeader}, {@code Storage/Data}.
 *
 * <p>The file is cut into sectors of 512 bytes (version 3) or 4096 (version 4), chained by the file allocation table; a
 * stream shorter than the cutoff lives in the mini stream instead, cut into 64-byte sectors chained by the mini table.
 * The directory is a red-black tree of entries for each storage; its order is the file's, not one a reader sorts.</p>
 */
public final class CompoundFile {

    private static final long SIGNATURE = 0xE11AB1A1E011CFD0L;
    private static final int HEADER = 512;
    private static final int HEADER_DIFAT = 109;
    private static final int DIRECTORY_ENTRY = 128;
    private static final int END_OF_CHAIN = 0xFFFFFFFE;
    private static final int FREE = 0xFFFFFFFF;
    private static final int NO_STREAM = 0xFFFFFFFF;

    /** What a directory entry is. */
    private enum Kind {
        EMPTY, STORAGE, STREAM, ROOT;

        static Kind of(int written) {
            return switch (written) {
                case 1 -> STORAGE;
                case 2 -> STREAM;
                case 5 -> ROOT;
                default -> EMPTY;
            };
        }
    }

    private record Entry(String name, Kind kind, int left, int right, int child, int start, long size) {
    }

    private final Map<String, byte[]> streams;

    private CompoundFile(Map<String, byte[]> streams) {
        this.streams = Collections.unmodifiableMap(streams);
    }

    /** @throws CompoundFileException when the bytes are no compound file, or one broken past reading */
    public static CompoundFile read(byte[] bytes) {
        return new Reading(bytes).file();
    }

    /** Whether the bytes open as a compound file does: what tells one from a text a format may also be written as. */
    public static boolean isCompoundFile(byte[] bytes) {
        return bytes.length >= HEADER && ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong(0) == SIGNATURE;
    }

    /** A stream by its path: {@code FileHeader}, {@code Storage/Data}; empty where the file holds none of that name. */
    public Optional<byte[]> stream(String path) {
        return Optional.ofNullable(streams.get(path)).map(byte[]::clone);
    }

    /** Every stream's path, in the file's order. */
    public List<String> streams() {
        return List.copyOf(streams.keySet());
    }

    /** One reading of the bytes: the header, the tables, the directory, then every stream. */
    private static final class Reading {

        private final ByteBuffer bytes;
        private final int sectorSize;
        private final int miniSectorSize;
        private final int miniCutoff;
        private final List<Integer> fat = new ArrayList<>();
        private final List<Integer> miniFat = new ArrayList<>();
        private final List<Entry> entries = new ArrayList<>();
        private byte[] miniStream = new byte[0];

        Reading(byte[] raw) {
            if (!isCompoundFile(raw)) {
                throw new CompoundFileException("This is no compound file: it does not open with D0 CF 11 E0 A1 B1 1A E1.");
            }
            this.bytes = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
            int sectorShift = Short.toUnsignedInt(bytes.getShort(0x1E));
            int miniShift = Short.toUnsignedInt(bytes.getShort(0x20));
            if (sectorShift != 9 && sectorShift != 12) {
                throw new CompoundFileException("A compound file's sectors are 512 or 4096 bytes, not 2^" + sectorShift + ".");
            }
            this.sectorSize = 1 << sectorShift;
            this.miniSectorSize = 1 << miniShift;
            this.miniCutoff = bytes.getInt(0x38);
        }

        CompoundFile file() {
            readFat();
            readDirectory();
            if (entries.isEmpty() || entries.getFirst().kind() != Kind.ROOT) {
                throw new CompoundFileException("A compound file's directory opens with its root, and this one's does not.");
            }
            Entry root = entries.getFirst();
            miniStream = chain(root.start(), root.size());
            readMiniFat();
            Map<String, byte[]> streams = new LinkedHashMap<>();
            collect(root.child(), "", streams, new HashSet<>());
            return new CompoundFile(streams);
        }

        /** The table's sectors: 109 named in the header, the rest in the DIFAT's chain, each ending with the next. */
        private void readFat() {
            List<Integer> fatSectors = new ArrayList<>();
            for (int index = 0; index < HEADER_DIFAT; index++) {
                int sector = bytes.getInt(0x4C + index * 4);
                if (sector != FREE && sector != END_OF_CHAIN) {
                    fatSectors.add(sector);
                }
            }
            int difat = bytes.getInt(0x44);
            int perSector = sectorSize / 4 - 1;
            Set<Integer> visited = new HashSet<>();
            while (difat != END_OF_CHAIN && difat != FREE) {
                if (!visited.add(difat)) {
                    throw new CompoundFileException("The DIFAT chain runs in a circle at sector " + difat + ".");
                }
                int offset = offsetOf(difat);
                for (int index = 0; index < perSector; index++) {
                    int sector = bytes.getInt(offset + index * 4);
                    if (sector != FREE && sector != END_OF_CHAIN) {
                        fatSectors.add(sector);
                    }
                }
                difat = bytes.getInt(offset + perSector * 4);
            }
            for (int sector : fatSectors) {
                int offset = offsetOf(sector);
                for (int index = 0; index < sectorSize / 4; index++) {
                    fat.add(bytes.getInt(offset + index * 4));
                }
            }
        }

        private void readDirectory() {
            byte[] directory = chain(bytes.getInt(0x30), -1);
            ByteBuffer table = ByteBuffer.wrap(directory).order(ByteOrder.LITTLE_ENDIAN);
            for (int offset = 0; offset + DIRECTORY_ENTRY <= directory.length; offset += DIRECTORY_ENTRY) {
                int nameBytes = Math.min(64, Short.toUnsignedInt(table.getShort(offset + 0x40)));
                String name = nameBytes < 2 ? ""
                        : new String(directory, offset, nameBytes - 2, StandardCharsets.UTF_16LE);
                entries.add(new Entry(name, Kind.of(Byte.toUnsignedInt(table.get(offset + 0x42))), table.getInt(offset + 0x44),
                        table.getInt(offset + 0x48), table.getInt(offset + 0x4C), table.getInt(offset + 0x74),
                        Integer.toUnsignedLong(table.getInt(offset + 0x78))));
            }
        }

        private void readMiniFat() {
            byte[] table = chain(bytes.getInt(0x3C), -1);
            ByteBuffer read = ByteBuffer.wrap(table).order(ByteOrder.LITTLE_ENDIAN);
            for (int offset = 0; offset + 4 <= table.length; offset += 4) {
                miniFat.add(read.getInt(offset));
            }
        }

        /** The streams below a storage, in its tree's order — left, itself, right —, each storage's below it. */
        private void collect(int index, String path, Map<String, byte[]> streams, Set<Integer> visited) {
            if (index == NO_STREAM || index < 0 || index >= entries.size()) {
                return;
            }
            if (!visited.add(index)) {
                throw new CompoundFileException("The directory's tree runs in a circle at entry " + index + ".");
            }
            Entry entry = entries.get(index);
            collect(entry.left(), path, streams, visited);
            switch (entry.kind()) {
                case STREAM -> streams.put(path + entry.name(), entry.size() < miniCutoff
                        ? miniChain(entry.start(), entry.size()) : chain(entry.start(), entry.size()));
                case STORAGE -> collect(entry.child(), path + entry.name() + "/", streams, visited);
                case ROOT, EMPTY -> {
                }
            }
            collect(entry.right(), path, streams, visited);
        }

        /** A chain of sectors from its first, cut to {@code size} bytes; all of it for a size of -1. */
        private byte[] chain(int first, long size) {
            List<byte[]> pieces = new ArrayList<>();
            Set<Integer> visited = new HashSet<>();
            for (int sector = first; sector != END_OF_CHAIN && sector != FREE; sector = next(fat, sector)) {
                if (!visited.add(sector)) {
                    throw new CompoundFileException("A chain of sectors runs in a circle at sector " + sector + ".");
                }
                byte[] piece = new byte[sectorSize];
                int offset = offsetOf(sector);
                bytes.get(offset, piece, 0, Math.min(sectorSize, bytes.capacity() - offset));
                pieces.add(piece);
            }
            return joined(pieces, sectorSize, size);
        }

        /** A chain of mini sectors from its first, out of the mini stream, cut to {@code size} bytes. */
        private byte[] miniChain(int first, long size) {
            List<byte[]> pieces = new ArrayList<>();
            Set<Integer> visited = new HashSet<>();
            for (int sector = first; sector != END_OF_CHAIN && sector != FREE; sector = next(miniFat, sector)) {
                if (!visited.add(sector)) {
                    throw new CompoundFileException("A chain of mini sectors runs in a circle at " + sector + ".");
                }
                int offset = sector * miniSectorSize;
                if (offset + miniSectorSize > miniStream.length) {
                    throw new CompoundFileException("A mini sector " + sector + " lies past the mini stream's end.");
                }
                byte[] piece = new byte[miniSectorSize];
                System.arraycopy(miniStream, offset, piece, 0, miniSectorSize);
                pieces.add(piece);
            }
            return joined(pieces, miniSectorSize, size);
        }

        private static int next(List<Integer> table, int sector) {
            if (sector < 0 || sector >= table.size()) {
                throw new CompoundFileException("Sector " + sector + " lies past the allocation table's end.");
            }
            return table.get(sector);
        }

        private static byte[] joined(List<byte[]> pieces, int pieceSize, long size) {
            long whole = (long) pieces.size() * pieceSize;
            if (size > whole) {
                throw new CompoundFileException("A stream says it is " + size + " bytes and its chain holds " + whole + ".");
            }
            byte[] joined = new byte[(int) (size < 0 ? whole : size)];
            for (int index = 0, at = 0; index < pieces.size() && at < joined.length; index++, at += pieceSize) {
                System.arraycopy(pieces.get(index), 0, joined, at, Math.min(pieceSize, joined.length - at));
            }
            return joined;
        }

        private int offsetOf(int sector) {
            long offset = (long) (sector + 1) * sectorSize;
            if (sector < 0 || offset >= bytes.capacity()) {
                throw new CompoundFileException("Sector " + sector + " lies past the file's end.");
            }
            return (int) offset;
        }
    }
}
