package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A KiCad project on disk read as an import reads it, its documents printed: {@code -Dkicad.peek=<folder>}, the output to
 * {@code -Dkicad.peek.out=<file>} (else beside the project), the library documents whose slug holds
 * {@code -Dkicad.peek.library=<words>} printed too.
 */
@EnabledIfSystemProperty(named = "kicad.peek", matches = ".+")
class KiCadWrittenPeek {

    @Test
    void peek() throws Exception {
        Path folder = Path.of(System.getProperty("kicad.peek"));
        Map<String, String> files = new LinkedHashMap<>();
        String project = null;
        try (var listed = Files.list(folder)) {
            for (Path path : listed.toList()) {
                String name = path.getFileName().toString();
                if (name.endsWith(".kicad_sch") || name.endsWith(".kicad_pcb") || name.endsWith(".kicad_pro")) {
                    files.put(name, Files.readString(path));
                    project = name.endsWith(".kicad_pro") ? name : project;
                }
            }
        }
        ImportedDocuments imported = new KiCadDesignImport(() -> KiCadFootprintLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS)
                .read(new ExchangeFiles(files, project, null), CadLibrary.library());
        StringBuilder out = new StringBuilder();
        String library = System.getProperty("kicad.peek.library");
        for (DocumentNode document : imported.library()) {
            if (library != null && document.slug().contains(library)) {
                out.append("=== ").append(document.slug()).append(System.lineSeparator())
                        .append(CadLanguage.standard().language().write(document)).append(System.lineSeparator());
            }
        }
        for (DocumentNode design : imported.designs()) {
            out.append("=== ").append(design.slug()).append(System.lineSeparator())
                    .append(CadLanguage.standard().language().write(design)).append(System.lineSeparator());
        }
        imported.notes().forEach(note -> out.append("NOTE ").append(note).append(System.lineSeparator()));
        Path written = Path.of(System.getProperty("kicad.peek.out", folder.resolve("read-back.txt").toString()));
        Files.writeString(written, out.toString());
    }
}
