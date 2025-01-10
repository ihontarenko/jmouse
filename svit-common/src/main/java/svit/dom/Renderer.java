package svit.dom;

public interface Renderer {

    String render(Node node, NodeContext context);

    default String indentation(int depth) {
        return "\t".repeat(Math.max(0, depth));
    }

}

