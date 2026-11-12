package org.jmouse.mesh;

import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Bodies set in a scene: each body held once, and every node naming one and where it stands — so the parts of an
 * assembly that share a body share its triangles —, all under one root placed over the whole.
 *
 * @param name   what the root is called
 * @param root   where the whole stands: a file's own frame and unit over the bodies' ({@link Placement#IDENTITY} for none)
 * @param meshes the bodies, each once
 * @param nodes  where each body stands, by its index among {@code meshes}
 * @param extras what the scene says of itself beyond its triangles, for the reader that knows to look — glTF's
 *               {@code extras} on the root, which a viewer reads as the root's own data; null for nothing
 */
public record MeshScene(String name, Placement root, List<Mesh> meshes, List<Node> nodes, JsonNode extras) {

    /** One body where it stands, under the root. */
    public record Node(String name, int mesh, Placement placement) {
    }

    public MeshScene {
        meshes = List.copyOf(meshes);
        nodes = List.copyOf(nodes);
        for (Node node : nodes) {
            if (node.mesh() < 0 || node.mesh() >= meshes.size()) {
                throw new IllegalArgumentException("The node " + node.name() + " names a body the scene does not hold");
            }
        }
        if (extras != null && !extras.isObject()) {
            throw new IllegalArgumentException("A scene's extras are an object of named values");
        }
    }

    /** A scene that says nothing of itself beyond its bodies. */
    public MeshScene(String name, Placement root, List<Mesh> meshes, List<Node> nodes) {
        this(name, root, meshes, nodes, null);
    }

    /** One body standing alone where it is. */
    public static MeshScene of(Mesh mesh) {
        return new MeshScene(null, Placement.IDENTITY, List.of(mesh), List.of(new Node(null, 0, Placement.IDENTITY)));
    }

    /** The same scene saying this of itself. */
    public MeshScene withExtras(JsonNode said) {
        return new MeshScene(name, root, meshes, nodes, said);
    }
}
