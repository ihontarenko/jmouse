package svit.dom.builder;

import svit.dom.Node;

public interface NodeBuilder<T> {
    Node build(T t, NodeBuilderContext ctx);
}
