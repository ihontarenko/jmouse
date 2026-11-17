package org.jmouse.cad.plugin.mesh;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.mesh.ModelFile;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.core.io.ByteArrayResource;
import org.jmouse.mesh.MeshFormats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 3D models brought in as they are: each file a model of its own, in any format a model may be in, named after the file and
 * coded in the namespace of the folder it stands in — {@code Resistor_SMD.3dshapes/R_0805.step} is {@code RESISTOR-SMD:R-0805:M3D},
 * as KiCad's import codes the same model —, or in the format's own when it stands in none. A footprint then shows one by naming it.
 */
public final class ModelFilesImport implements DocumentImport {

    /** The format's word on the exchange routes. */
    static final String FORMAT = "model";

    private static final int FIRST_LINE = 1;

    private final MeshFormats formats;

    ModelFilesImport(MeshFormats formats) {
        this.formats = formats;
    }

    @Override
    public String id() {
        return FORMAT;
    }

    @Override
    public String title() {
        return "3D model";
    }

    @Override
    public List<String> fileNames() {
        return formats.extensions().stream().map(extension -> "*." + extension).toList();
    }

    @Override
    public List<String> fileNamesReadAlone() {
        return fileNames();
    }

    @Override
    public List<String> fileNamesAsBytes() {
        return fileNames();
    }

    @Override
    public ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue) {
        List<DocumentNode> models = new ArrayList<>();
        List<FetchedFile> kept = new ArrayList<>();
        for (String path : files.files().keySet()) {
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            String format = formats.ofFile(fileName.toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new IllegalArgumentException(fileName + " is in no format a 3D model may be in.")).id();
            String title = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            String slug = ImportedNames.slug(title, "model");
            DocumentNode model = new DocumentNode(ModelModule.MODEL, title, slug, null, FIRST_LINE);
            model.addExpression(MetaNode.filed(title + " — a 3D model from " + fileName, codeOf(path, title), null, List.of()));
            model.addExpression(ModelFile.of(fileName, format).line());
            models.add(model);
            kept.add(new FetchedFile(slug, fileName, new ByteArrayResource(files.bytesOf(path))));
        }
        return new ImportedDocuments(models, List.of(), List.of(), kept);
    }

    /**
     * The code a model file is named by: its folder's name, without what says the folder's kind ({@code .3dshapes}), is its
     * library; a file in no folder is the format's.
     */
    public static String codeOf(String path, String title) {
        int slash = path.lastIndexOf('/');
        String folder = slash < 0 ? "" : path.substring(path.lastIndexOf('/', slash - 1) + 1, slash);
        int kind = folder.indexOf('.');
        String library = kind < 0 ? folder : folder.substring(0, kind);
        return library.isBlank() ? codeOf(title) : ImportedNames.code(TechnicalClass.MODEL, library, title);
    }

    /** The code of a model brought in as a file of its own, in no folder: in the format's own namespace. */
    public static String codeOf(String title) {
        return ImportedNames.code(TechnicalClass.MODEL, FORMAT, title);
    }
}
