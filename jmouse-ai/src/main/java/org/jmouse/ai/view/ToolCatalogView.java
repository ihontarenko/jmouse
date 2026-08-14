package org.jmouse.ai.view;

import org.jmouse.ai.PublishedTool;
import org.jmouse.ai.ToolCatalog;
import org.jmouse.ai.ToolOrigin;

import java.util.List;
import java.util.Optional;

/**
 * What tools exist, what each costs, where each comes from, and which of them destroy something.
 *
 * <p>A port rather than "just use {@link ToolCatalog}", for one reason worth the interface: a screen
 * given the catalogue is a screen holding the object whose whole design is that it is the one door to
 * a handler. The door is package-private, so nothing could actually walk through it — but a reader of
 * a management module should not have to establish that, and a later author should not be one
 * {@code public} away from being able to. This hands over the answers and not the thing that has them.
 *
 * <p>{@link #tools()} is exactly what {@link ToolCatalog#published()} returns, in the same order, which
 * is namespaces alphabetically and actions in the order their feature declares them. A tool reads best
 * with its listing actions before its writing ones, and no sort reproduces that.
 */
public interface ToolCatalogView {

    /** Every action, in the catalogue's own order, as much of it as anything is allowed to see. */
    List<PublishedTool> tools();

    /** One action by its wire name, or empty where nothing is published under it. */
    Optional<PublishedTool> find(String publishedName);

    /**
     * The namespaces, which is how many <em>tools</em> there are.
     *
     * <p>Not derivable from the size of {@link #tools()}: eight actions may be three tools, and a
     * screen reporting the catalogue to a person needs both numbers.
     */
    List<String> toolNames();

    /** Everything forwarded to a server this application connected to, for a screen that groups by it. */
    default List<PublishedTool> remoteTools() {
        return tools().stream().filter(tool -> tool.origin() == ToolOrigin.REMOTE).toList();
    }

    /** The view over a catalogue. The only implementation any product should need. */
    static ToolCatalogView over(ToolCatalog catalog) {
        return new ToolCatalogView() {

            @Override
            public List<PublishedTool> tools() {
                return catalog.published();
            }

            @Override
            public Optional<PublishedTool> find(String publishedName) {
                return catalog.findPublished(publishedName);
            }

            @Override
            public List<String> toolNames() {
                return catalog.toolNames();
            }
        };
    }
}
