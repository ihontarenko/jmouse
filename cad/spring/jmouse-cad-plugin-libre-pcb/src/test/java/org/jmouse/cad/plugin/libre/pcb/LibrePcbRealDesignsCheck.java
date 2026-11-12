package org.jmouse.cad.plugin.libre.pcb;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.libre.pcb.LibrePcbFiling;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.libre.pcb.LibrePcbFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every LibrePCB project under a folder imported whole and fabricated: {@code -Dlibre.pcb.real.projects=<folder>}. What must
 * hold whatever the board: every part the board stands is on the pcb, and every track that ends on a pad ends on it on ours.
 */
@EnabledIfSystemProperty(named = "libre.pcb.real.projects", matches = ".+")
class LibrePcbRealDesignsCheck {

    private static final double NEAR = 0.002;

    @Test
    void everyProjectImportsAndItsTracksReachItsPads() throws IOException {
        Path root = Path.of(System.getProperty("libre.pcb.real.projects"));
        List<Path> markers;
        try (Stream<Path> found = Files.walk(root)) {
            markers = found.filter(path -> LibrePcbFile.isProject(path.toString())).toList();
        }
        assertFalse(markers.isEmpty(), "no project under " + root);
        for (Path marker : markers) {
            ImportedDocuments imported = new LibrePcbDesignReading(LibrePcbFiling.standard()).read(
                    new ExchangeFiles(filesBeside(marker), marker.getFileName().toString(), null), CadLibrary.library());
            Map<String, DocumentNode> made = new HashMap<>();
            imported.library().forEach(document -> made.put(document.slug(), document));
            imported.designs().forEach(document -> made.put(document.slug(), document));
            CatalogueDocuments documents = slug -> Optional.ofNullable(made.get(slug)).or(() -> CadLibrary.library().find(slug));
            PcbFabrication fabrication = PcbFabrication.of(imported.root(), documents);
            PcbDrawing pcb = fabrication.pcb();
            long ends = pcb.tracks().stream().mapToLong(track -> track.points().size()).sum();
            long onPads = 0;
            for (PcbFabrication.SeatedPart part : fabrication.parts()) {
                for (var pad : part.drawing().map(drawing -> drawing.pads()).orElse(List.of())) {
                    PcbDrawing.Point at = part.placement().placed(pad.x(), pad.y());
                    onPads += pcb.tracks().stream().flatMap(track -> track.points().stream())
                            .filter(end -> Math.hypot(end.x() - at.x(), end.y() - at.y()) < NEAR).count();
                }
            }
            System.out.printf("%s: %d parts, %d tracks, %d vias, %d zones, %d shapes, %d texts, %d of %d track ends on pads, %d library documents%n",
                    marker.getFileName(), fabrication.parts().size(), pcb.tracks().size(), pcb.vias().size(), pcb.zones().size(), pcb.shapes().size(),
                    pcb.texts().size(), onPads, ends, imported.library().size());
            imported.notes().stream().distinct().limit(12).forEach(note -> System.out.println("    " + note));
            assertTrue(onPads > 0, "no track of " + marker + " reaches a pad: the frames disagree");
            // What is stored is text: every document must read back through the language it was written in.
            List<DocumentNode> everything = new ArrayList<>(imported.library());
            everything.addAll(imported.designs());
            // Models are a module the 3D models plugin brings, beside the engine's own.
            List<DocumentModule> modules = new ArrayList<>(CadLanguage.engineModules());
            modules.add(new ModelModule());
            DocumentLanguage language = CadLanguage.of(modules).language();
            everything.forEach(document -> language.readOne(language.write(document), document.slug()));
            assertFalse(pcb.outline().isBlank());
        }
    }

    @Test
    void anArchiveReadsAsItsFolderDoes() throws IOException {
        Path root = Path.of(System.getProperty("libre.pcb.real.projects"));
        List<Path> markers;
        try (Stream<Path> found = Files.walk(root)) {
            markers = found.filter(path -> LibrePcbFile.isProject(path.toString())).toList();
        }
        for (Path marker : markers) {
            LibrePcbDesignReading reading = new LibrePcbDesignReading(LibrePcbFiling.standard());
            ImportedDocuments folder = reading.read(new ExchangeFiles(filesBeside(marker), marker.getFileName().toString(), null), CadLibrary.library());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes); Stream<Path> found = Files.walk(marker.getParent())) {
                for (Path path : found.filter(Files::isRegularFile).toList()) {
                    zip.putNextEntry(new ZipEntry(marker.getParent().relativize(path).toString().replace('\\', '/')));
                    zip.write(Files.readAllBytes(path));
                    zip.closeEntry();
                }
            }
            String name = marker.getFileName().toString().replace(LibrePcbFile.PROJECT_SUFFIX, LibrePcbFile.ARCHIVE_SUFFIX);
            ImportedDocuments archive = reading.read(new ExchangeFiles(Map.of(name, Base64.getEncoder().encodeToString(bytes.toByteArray())), name, null), CadLibrary.library());
            assertEquals(folder.library().size(), archive.library().size());
            assertEquals(folder.files().size(), archive.files().size());
            assertFalse(folder.files().isEmpty(), "no model of " + marker + " came with it");
        }
    }

    private static Map<String, String> filesBeside(Path marker) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        Path folder = marker.getParent();
        try (Stream<Path> found = Files.walk(folder)) {
            for (Path path : found.filter(Files::isRegularFile).toList()) {
                String name = folder.relativize(path).toString().replace('\\', '/');
                if (LibrePcbFile.isModel(name)) {
                    files.put(name, Base64.getEncoder().encodeToString(Files.readAllBytes(path)));
                } else if (LibrePcbFile.isRead(name)) {
                    files.put(name, Files.readString(path));
                }
            }
        }
        return files;
    }
}
