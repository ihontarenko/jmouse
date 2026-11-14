package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.mesh.ModelFile;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.libre.pcb.LibrePcbFile;
import org.jmouse.libre.pcb.LibrePcbLibrary;
import org.jmouse.mesh.MeshFormats;
import org.jmouse.core.io.ByteArrayResource;

/** A 3D model of a package as a jM model document, and its file. A project keeps a model as STEP: one file, by its UUID. */
final class LibrePcbModelDrawing {

    /** A model document, and the file it holds. */
    record Made(DocumentNode document, FetchedFile file) {
    }

    private LibrePcbModelDrawing() {
    }

    static Made of(LibrePcbLibrary.Package found, String modelId, byte[] bytes, LibrePcbFiling filing) {
        String name = found.modelNames().getOrDefault(modelId, found.name());
        String slug = LibrePcbNames.model(found, modelId);
        String fileName = modelId + LibrePcbFile.MODEL_SUFFIX;
        String format = MeshFormats.standard().ofFile(fileName).orElseThrow().id();
        DocumentNode document = new DocumentNode(ModelModule.MODEL, name, slug, null, 1);
        Provenance provenance = filing.provenance(LibrePcbNames.source(LibrePcbNames.SourceKind.MODEL, name, modelId), found.author());
        MetaNode meta = MetaNode.filed(name + " — a 3D model imported from LibrePCB", LibrePcbNames.modelCode(found, modelId), filing.categoryOf(provenance),
                filing.footprintTags());
        provenance.writeInto(meta);
        document.addExpression(meta);
        document.addExpression(new ModelFile(fileName, format, null, null).line());
        return new Made(document, new FetchedFile(slug, fileName, new ByteArrayResource(bytes)));
    }
}
