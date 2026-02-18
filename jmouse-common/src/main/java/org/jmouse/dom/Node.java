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

    String interpret(NodeContext context);

    default void execute(Consumer<Node> executor) {
        executor.accept(this);

        for (Node child : new CopyOnWriteArrayList<>(getChildren())) {
            child.execute(executor);
        }
    }

    default void addClass(String className) {
        addClass(this, className);
    }

    default boolean containsClass(String required) {
        return containsClass(getAttribute("class"), required);
    }

    static void addClass(Node node, String className) {
        String existing = node.getAttribute("class");

        if (existing == null || existing.isBlank()) {
            node.addAttribute("class", className);
            return;
        }

        if (containsClass(existing, className)) {
            return;
        }

        node.addAttribute("class", existing + " " + className);
    }

    static boolean containsClass(String classValue, String required) {
        String[] parts = classValue.trim().split("\\s+");

        for (String string : parts) {
            if (string.equals(required)) {
                return true;
            }
        }

        return false;
    }

}

