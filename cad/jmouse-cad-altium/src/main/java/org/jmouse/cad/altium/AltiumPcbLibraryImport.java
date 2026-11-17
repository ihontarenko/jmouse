package org.jmouse.cad.altium;

import org.jmouse.altium.AltiumPcbLibrary;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.mesh.ModelFile;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.core.io.ByteArrayResource;
import org.jmouse.mesh.MeshFormats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An Altium PCB library as jM documents: a footprint for each of its footprints ({@link AltiumFootprintDrawing}) and a 3D
 * model for each STEP file it embeds, each named by its code in the library's namespace — {@code rcl.PcbLib}'s
 * {@code R0402} is {@code RCL:R0402:F} — and each model before the footprints that name it.
 *
 * <p>A library is somebody's work, and the file does not say whose: each document says only where it came from, and whose
 * it is is what the reader says of the file ({@link ImportedDocuments#whoseAsStated}), given where the import is read.</p>
 */
public final class AltiumPcbLibraryImport {

    private AltiumPcbLibraryImport() {
    }

    /**
     * @param file the library's file name, {@code rcl.PcbLib}
     */
    public static ImportedDocuments read(AltiumPcbLibrary library, String file, AltiumLibraries filing) {
        List<DocumentNode> models = new ArrayList<>();
        List<FetchedFile> files = new ArrayList<>();
        Map<String, String> modelCodes = new LinkedHashMap<>();
        Map<String, byte[]> contentOfCode = new LinkedHashMap<>();
        for (AltiumPcbLibrary.Model model : library.models()) {
            String identifier = model.identifier().toUpperCase(Locale.ROOT);
            String name = nameOf(model);
            String code = AltiumNames.modelCode(file, name);
            byte[] held = contentOfCode.get(code);
            if (held != null && Arrays.equals(held, model.content())) {
                // The same file embedded again under another identifier: one model, named by both.
                modelCodes.put(identifier, code);
                continue;
            }
            if (held != null) {
                // Another file of the same name: a model of its own, told apart by the start of its identifier.
                name = AltiumNames.withoutFileKind(name) + " " + identifier.replaceAll("[^0-9A-F]", "").substring(0, IDENTIFIER_KEPT);
                code = AltiumNames.modelCode(file, name);
            }
            modelCodes.put(identifier, code);
            contentOfCode.put(code, model.content());
            String slug = AltiumNames.model(file, name);
            String fileName = slug + "." + extensionOf(model.fileName());
            DocumentNode document = new DocumentNode(ModelModule.MODEL, AltiumNames.withoutFileKind(name), slug, null, 1);
            Provenance whose = filing.provenance(AltiumNames.modelSource(file, name), false);
            MetaNode meta = MetaNode.filed(AltiumNames.withoutFileKind(name) + " — a 3D model embedded in " + file, code,
                    filing.categoryOf(whose), filing.footprintTags());
            whose.writeInto(meta);
            document.addExpression(meta);
            String embedded = name;
            String format = MeshFormats.standard().ofFile(fileName).orElseThrow(() -> new IllegalArgumentException(
                    file + " embeds " + embedded + ", a file of no 3D format this engine reads.")).id();
            document.addExpression(new ModelFile(fileName, format, null, null).line());
            models.add(document);
            files.add(new FetchedFile(slug, fileName, new ByteArrayResource(model.content())));
        }
        List<DocumentNode> footprints = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        for (AltiumPcbLibrary.Footprint footprint : library.footprints()) {
            if (footprint.pads().isEmpty()) {
                notes.add(footprint.name() + ": no pad — a drawing template, not a footprint; not brought");
                continue;
            }
            AltiumFootprintDrawing.Converted converted = AltiumFootprintDrawing.documentOf(file, footprint, library.mechanicalKind(),
                    modelCodes, filing);
            footprints.add(converted.document());
            notes.addAll(converted.leftOut());
        }
        List<DocumentNode> made = new ArrayList<>(models);
        made.addAll(footprints);
        return new ImportedDocuments(made, List.of(), notes, files);
    }

    /** The kind of file Altium embeds a model as. */
    private static final String STEP = "step";

    /** How much of an identifier tells two files of one name apart. */
    private static final int IDENTIFIER_KEPT = 8;

    /**
     * The name a model is known by: its file's, as Altium kept it; its identifier where the library kept no file name — the
     * identifier is what Altium itself knows it by.
     */
    private static String nameOf(AltiumPcbLibrary.Model model) {
        return model.fileName().isBlank() ? model.identifier().replaceAll("[{}]", "") : model.fileName();
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        // A model is a STEP file, as Altium embeds them; one kept with no name says no kind of its own.
        return dot < 0 ? STEP : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
