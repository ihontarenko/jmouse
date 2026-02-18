package org.jmouse.dom;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@SuppressWarnings({"unused"})
public interface Node /*extends df.base.common.libs.parser.node.Node*/ {

    int getDepth();

    void setDepth(int depth);

    NodeType getNodeType();

    void prepend(Node child);

    void append(Node child);

    void removeChild(Node child);

    List<Node> getChildren();

    default boolean hasChildren() {
        return getChildren() != null && getChildren().size() > 0;
    }

    void wrap(Node wrapper);

    void unwrap();

    Node getParent();

    void setParent(Node node);

    default boolean hasParent() {
        return getParent() != null;
    }

    void insertBefore(Node sibling);

    void insertAfter(Node sibling);

    List<Node> getSiblings();

    TagName getTagName();

    void addAttribute(String key, String value);

    String getAttribute(String name);

    Map<String, String> getAttributes();

    default void execute(Consumer<Node> executor) {
        executor.accept(this);

        for (Node child : new CopyOnWriteArrayList<>(getChildren())) {
            child.execute(executor);
        }
    }

}

