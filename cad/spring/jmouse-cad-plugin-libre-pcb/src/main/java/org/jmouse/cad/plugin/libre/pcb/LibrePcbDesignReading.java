package org.jmouse.cad.plugin.libre.pcb;

import org.jmouse.cad.exchange.imported.SheetMarks;

import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.libre.pcb.LibrePcbDesignImport;
import org.jmouse.cad.libre.pcb.LibrePcbFiling;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.libre.pcb.LibrePcbArchive;
import org.jmouse.libre.pcb.LibrePcbFile;
import org.jmouse.libre.pcb.LibrePcbProject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A LibrePCB project read as jM documents, from its marker and the folder of files beside it ({@link LibrePcbDesignImport}). */
final class LibrePcbDesignReading implements DocumentImport {

    private static final char SEPARATOR = '/';

    private final LibrePcbFiling filing;

    LibrePcbDesignReading(LibrePcbFiling filing) {
        this.filing = filing;
    }

    @Override
    public String id() {
        return LibrePcbPlugin.FORMAT;
    }

    @Override
    public String title() {
        return LibrePcbPlugin.TITLE;
    }

    @Override
    public List<String> fileNames() {
        return List.of("*" + LibrePcbFile.PROJECT_SUFFIX, "*" + LibrePcbFile.ARCHIVE_SUFFIX, "*" + LibrePcbFile.TEXT_SUFFIX, "*" + LibrePcbFile.MODEL_SUFFIX);
    }

    /** An archive holds the whole project. */
    @Override
    public List<String> fileNamesReadAlone() {
        return List.of("*" + LibrePcbFile.ARCHIVE_SUFFIX);
    }

    @Override
    public List<String> fileNamesAsBytes() {
        return List.of("*" + LibrePcbFile.ARCHIVE_SUFFIX, "*" + LibrePcbFile.MODEL_SUFFIX);
    }

    @Override
    public ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue) {
        String main = files.main().replace('\\', SEPARATOR);
        if (LibrePcbFile.isArchive(main)) {
            return fromArchive(files, main, catalogue);
        }
        if (!LibrePcbFile.isProject(main)) {
            throw new IllegalArgumentException("LibrePCB reads a project by its " + LibrePcbFile.PROJECT_SUFFIX + " file; '" + main + "' is none.");
        }
        int nameStarts = main.lastIndexOf(SEPARATOR) + 1;
        String file = main.substring(nameStarts);
        String folder = main.substring(0, nameStarts);
        LibrePcbProject project = LibrePcbProject.read(insideFolder(files.files(), folder));
        return LibrePcbDesignImport.read(project, modelsInside(files, folder), file, titleOf(files, project), filing,
                SheetMarks.of(catalogue, filing.markSymbols()));
    }

    private ImportedDocuments fromArchive(ExchangeFiles files, String main, CatalogueDocuments catalogue) {
        LibrePcbArchive archive = LibrePcbArchive.of(files.bytesOf(files.main()));
        LibrePcbProject project = LibrePcbProject.read(archive.texts());
        return LibrePcbDesignImport.read(project, archive.models(), main.substring(main.lastIndexOf(SEPARATOR) + 1), titleOf(files, project), filing,
                SheetMarks.of(catalogue, filing.markSymbols()));
    }

    private static String titleOf(ExchangeFiles files, LibrePcbProject project) {
        return files.title() == null || files.title().isBlank() ? project.name() : files.title();
    }

    /** The project's 3D models, sent in Base64, by their path inside its folder. */
    private static Map<String, byte[]> modelsInside(ExchangeFiles files, String folder) {
        Map<String, byte[]> models = new LinkedHashMap<>();
        files.files().keySet().forEach(name -> {
            String path = name.replace('\\', SEPARATOR);
            if (path.startsWith(folder) && LibrePcbFile.isModel(path)) {
                models.put(path.substring(folder.length()), files.bytesOf(name));
            }
        });
        return models;
    }

    /** The files of the project's folder, by their path inside it: what was chosen may have been a folder over it. */
    private static Map<String, String> insideFolder(Map<String, String> files, String folder) {
        Map<String, String> inside = new LinkedHashMap<>();
        files.forEach((name, text) -> {
            String path = name.replace('\\', SEPARATOR);
            if (path.startsWith(folder) && LibrePcbFile.isRead(path) && !LibrePcbFile.isModel(path)) {
                inside.put(path.substring(folder.length()), text);
            }
        });
        return inside;
    }
}
