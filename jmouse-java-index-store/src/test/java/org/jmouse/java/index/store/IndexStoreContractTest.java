package org.jmouse.java.index.store;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.java.index.CodeIndex;
import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.SourceReferences;
import org.jmouse.java.index.store.disk.DiskIndexStore;
import org.jmouse.java.index.store.disk.IndexBinaryCodec;
import org.jmouse.java.parser.JavaParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

class IndexStoreContractTest {
    @TempDir Path cache;
    private final String text =
            "package sample; /** See {@link Sample#run()}. */ class Sample { void run() { run(); }"
                    + " }";

    @Test
    void payloadRoundTripRetainsNewFactsAndRejectsCorruption() throws Exception {
        FileIndex file =
                FileIndex.of(
                        new JavaParser().parse("Sample.java", text, JavaParser.Level.EXPRESSIONS));
        byte[] payload = IndexBinaryCodec.encodeFile(file);
        assertEquals(file, IndexBinaryCodec.decodeFile(payload));
        assertEquals(
                SourceReferences.of(file),
                SourceReferences.of(IndexBinaryCodec.decodeFile(payload)));
        payload[payload.length / 2] ^= 1;
        assertThrows(java.io.IOException.class, () -> IndexBinaryCodec.decodeFile(payload));
        assertThrows(java.io.IOException.class, () -> IndexBinaryCodec.decodeFile(new byte[3]));
    }

    @Test
    void diskRestoreRebasesDocumentationMembersAndLazyNavigation() throws Exception {
        FileIndex file;
        try (DiskIndexStore store =
                new DiskIndexStore(cache, "project", JavaParser.Level.EXPRESSIONS)) {
            file = store.index("Sample.java", text.getBytes(StandardCharsets.UTF_8), 1).file();
            assertEquals(file, store.read("Sample.java").orElseThrow());
        }
        try (DiskIndexStore store =
                new DiskIndexStore(cache, "project", JavaParser.Level.EXPRESSIONS)) {
            CodeIndex symbols =
                    new CodeIndex(store.catalog(), source -> store.read(source).orElseThrow());
            assertEquals(1, symbols.searchTypes("Sample", 10).size());
            assertEquals(file, symbols.file("Sample.java").orElseThrow());
            var renamed = store.index("Renamed.java", text.getBytes(StandardCharsets.UTF_8), 2);
            assertFalse(renamed.parsed());
            assertEquals(file.withSource("Renamed.java"), renamed.file());
            assertEquals(file.javaDocs(), renamed.file().javaDocs());
            store.remove("Sample.java");
        }
        try (DiskIndexStore store =
                new DiskIndexStore(cache, "project", JavaParser.Level.EXPRESSIONS)) {
            assertTrue(store.read("Sample.java").isEmpty());
            assertTrue(store.read("Renamed.java").isPresent());
        }
    }

    @Test
    void incompatibleDepthDoesNotReuseExpressionStub() throws Exception {
        try (DiskIndexStore deep =
                new DiskIndexStore(cache, "project", JavaParser.Level.EXPRESSIONS)) {
            deep.index("Sample.java", text.getBytes(StandardCharsets.UTF_8), 1);
        }
        try (DiskIndexStore shallow =
                new DiskIndexStore(cache, "project", JavaParser.Level.DECLARATIONS)) {
            var result = shallow.index("Sample.java", text.getBytes(StandardCharsets.UTF_8), 1);
            assertTrue(result.parsed());
            assertTrue(result.file().bodies().isEmpty());
        }
    }

    @Test
    void corruptedStubIsRebuiltFromOriginal() throws Exception {
        try (DiskIndexStore store =
                new DiskIndexStore(cache, "project", JavaParser.Level.EXPRESSIONS)) {
            store.index("Sample.java", text.getBytes(StandardCharsets.UTF_8), 1);
        }
        String hash =
                HexFormat.of()
                        .formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(text.getBytes(StandardCharsets.UTF_8)));
        try (var paths = Files.walk(cache)) {
            Path stub =
                    paths.filter(path -> path.getFileName().toString().equals(hash + ".stub"))
                            .findFirst()
                            .orElseThrow();
            Files.write(stub, new byte[] {1, 2, 3});
        }
        try (DiskIndexStore store =
                new DiskIndexStore(cache, "project", JavaParser.Level.EXPRESSIONS)) {
            assertTrue(store.read("Sample.java").isEmpty());
            assertTrue(
                    store.index("Sample.java", text.getBytes(StandardCharsets.UTF_8), 1).parsed());
        }
    }
}
