package org.jmouse.cad.mesh;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.mesh.ModelSource.FetchedModel;
import org.jmouse.core.io.FileSystemResource;
import org.jmouse.mesh.MeshFormats;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🧊 A library of 3D model files on a disk — any library, whoever made it: folders searched in order, a model found by
 * its path from a folder's top, every file of it taken in any format {@link MeshFormats} reads, each written into the
 * model's document with the conventions the library's files keep against their formats' standards.
 *
 * <p>Knows no product: what a library is called, whose it is and which conventions its files keep is said by whoever
 * mounts it — a KiCad installation's {@code 3dmodels}, a company's own folder.</p>
 *
 * @param folders     the folders searched, the first that holds a model's files giving them
 * @param conventions how the library's files depart from their formats' standards
 */
public record ModelFolders(List<Path> folders, MeshFormats formats, Conventions conventions) {

    /**
     * How a library's files depart from their formats' standards, by format id — where they keep the standard, nothing.
     *
     * @param scales the millimetres a unit of the files is
     * @param ups    the axis the files stand up along
     */
    public record Conventions(Map<String, Double> scales, Map<String, MeshFormats.Axis> ups) {

        public static final Conventions STANDARD = new Conventions(Map.of(), Map.of());

        public Conventions {
            scales = Map.copyOf(scales);
            ups = Map.copyOf(ups);
        }
    }

    public ModelFolders {
        folders = List.copyOf(folders);
    }

    /**
     * The model at a path of the library — {@code Resistor_SMD.3dshapes/R_0805_2012Metric}, without an extension —, as a
     * document of the slug and title given, its {@code meta} the one given, and its files; empty when no folder holds a
     * file of it in a format read here.
     */
    public Optional<FetchedModel> find(String path, String slug, String title, MetaNode meta) {
        for (Path folder : folders) {
            List<Path> files = new ArrayList<>();
            for (String extension : formats.extensions()) {
                Path file = folder.resolve(path + "." + extension);
                if (Files.isRegularFile(file)) {
                    files.add(file);
                }
            }
            if (!files.isEmpty()) {
                return Optional.of(fetched(files, slug, title, meta));
            }
        }
        return Optional.empty();
    }

    private FetchedModel fetched(List<Path> files, String slug, String title, MetaNode meta) {
        DocumentNode document = new DocumentNode(ModelModule.MODEL, title, slug, null, 1);
        document.addExpression(meta);
        List<FetchedFile> fetched = new ArrayList<>();
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String format = formats.ofFile(fileName).orElseThrow().id();
            document.addExpression(new ModelFile(fileName, format, conventions.scales().get(format), conventions.ups().get(format)).line());
            fetched.add(new FetchedFile(slug, fileName, new FileSystemResource(file)));
        }
        return new FetchedModel(document, fetched);
    }
}
