package org.jmouse.mesh;

import org.jmouse.mesh.gltf.GlbReader;
import org.jmouse.mesh.gltf.GltfReader;
import org.jmouse.mesh.obj.ObjReader;
import org.jmouse.mesh.stl.StlReader;
import org.jmouse.mesh.vrml.VrmlReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The formats a mesh can be read from here, by their readers: the library's own, and whatever is added — a reader that
 * needs a geometry kernel, as STEP does, is brought by whoever has one.
 */
public record MeshReaders(List<MeshReader> readers) {

    public MeshReaders {
        readers = List.copyOf(readers);
    }

    /** The library's own: glTF 2.0 (binary and JSON), STL, Wavefront OBJ and VRML97. */
    public static MeshReaders standard() {
        return new MeshReaders(List.of(new GlbReader(), new GltfReader(), new StlReader(), new ObjReader(), new VrmlReader()));
    }

    /** These and more; a reader added for a format already read here takes its place. */
    public MeshReaders with(List<MeshReader> added) {
        List<MeshReader> every = new ArrayList<>(added);
        readers.stream().filter(reader -> added.stream().noneMatch(other -> other.format().equals(reader.format())))
                .forEach(every::add);
        return new MeshReaders(every);
    }

    /** The reader of a format, while it can read. */
    public Optional<MeshReader> of(String format) {
        return readers.stream().filter(reader -> reader.format().equals(format)).filter(MeshReader::available).findFirst();
    }
}
