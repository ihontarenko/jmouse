package org.jmouse.el.renderable;

import java.util.HashMap;
import java.util.Map;

/**
 * The TemplateRegistry class maintains the definitions of blocks and macros
 * for a template. It allows for registering, retrieving, checking existence,
 * merging, and importing of definitions across templates.
 */
public class TemplateRegistry {

    private final Map<String, Block> blocks = new HashMap<>();
    private final Map<String, Macro> macros = new HashMap<>();
    private final Engine             engine;

    /**
     * Constructs a TemplateRegistry with the specified engine.
     *
     * @param engine the engine used for loading templates and related operations
     */
    public TemplateRegistry(Engine engine) {
        this.engine = engine;
    }

    /**
     * Returns the engine associated with this registry.
     *
     * @return the engine
     */
    public Engine getEngine() {
        return engine;
    }

    /**
     * Registers a block definition under the given name.
     *
     * @param name  the name of the block
     * @param block the block to register
     */
    public void registerBlock(String name, Block block) {
        blocks.put(name, block);
    }

    /**
     * Retrieves a block by its name.
     *
     * @param name the name of the block
     * @return the block associated with the name, or {@code null} if not found
     */
    public Block getBlock(String name) {
        return blocks.get(name);
    }

    /**
     * Returns {@code true} if a block with the given name is registered.
     *
     * @param name the name of the block
     * @return {@code true} if the block exists; {@code false} otherwise
     */
    public boolean hasBlock(String name) {
        return blocks.containsKey(name);
    }

    /**
     * Registers a macro definition under the given name.
     *
     * @param name  the name of the macro
     * @param macro the macro to register
     */
    public void registerMacro(String name, Macro macro) {
        macros.put(name, macro);
    }

    /**
     * Retrieves a macro by its name.
     *
     * @param name the name of the macro
     * @return the macro associated with the name, or {@code null} if not found
     */
    public Macro getMacro(String name) {
        return macros.get(name);
    }

    /**
     * Returns {@code true} if a macro with the given name is registered.
     *
     * @param name the name of the macro
     * @return {@code true} if the macro exists; {@code false} otherwise
     */
    public boolean hasMacro(String name) {
        return macros.containsKey(name);
    }

    /**
     * Imports definitions (blocks and macros) from another registry into this registry.
     * This is equivalent to a full merge where definitions from the source are added
     * to this registry.
     *
     * @param imported the source registry whose definitions are to be imported
     */
    public void copyFrom(TemplateRegistry imported) {
        merge(imported, this);
    }

    /**
     * Imports macros from another registry using a prefix alias.
     * <p>
     * Each imported macro will be registered under a key
     * of the form {@code prefix + "." + originalName}, or just {@code originalName}
     * if {@code prefix} is null.
     * </p>
     *
     * @param imported the registry from which macros are imported
     * @param prefix   the alias to prefix imported macro names with
     */
    public void copyMacros(TemplateRegistry imported, String prefix) {
        imported.macros.forEach(
                (name, macro) -> macros.put(
                        (prefix == null ? name : prefix + "." + name),
                        macro
                )
        );
    }

    /**
     * Imports blocks from another registry using a prefix alias.
     * <p>
     * Each imported block will be registered under a key
     * of the form {@code prefix + "." + originalName}, or just {@code originalName}
     * if {@code prefix} is null.
     * </p>
     *
     * @param imported the registry from which blocks are imported
     * @param prefix   the alias to prefix imported block names with
     */
    public void copyBlocks(TemplateRegistry imported, String prefix) {
        imported.blocks.forEach(
                (name, block) -> blocks.put(
                        (prefix == null ? name : prefix + "." + name),
                        block
                )
        );
    }

    /**
     * Merges the definitions from the given child registry with this registry.
     * Definitions from the child registry will override definitions in this registry
     * if keys conflict.
     *
     * @param child the child registry whose definitions are to be merged
     * @return a new TemplateRegistry instance containing merged definitions
     */
    public TemplateRegistry merge(TemplateRegistry child) {
        TemplateRegistry merged = new TemplateRegistry(engine);

        merge(child, merged);
        merge(this, merged);

        return merged;
    }

    /**
     * Copies all definitions from the source registry into the destination registry.
     *
     * @param source      the registry to copy definitions from
     * @param destination the registry to copy definitions into
     */
    public void merge(TemplateRegistry source, TemplateRegistry destination) {
        destination.blocks.putAll(source.blocks);
        destination.macros.putAll(source.macros);
    }
}
