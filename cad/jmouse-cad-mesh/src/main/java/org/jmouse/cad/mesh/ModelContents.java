package org.jmouse.cad.mesh;

import java.util.List;

/**
 * What a model document is to the engine: the files its body is in, each in its format and convention, or the shapes it
 * is drawn from here.
 */
public record ModelContents(List<ModelFile> files, List<ModelShapes.Shape> shapes) {

    public ModelContents {
        files = List.copyOf(files);
        shapes = List.copyOf(shapes);
    }
}
