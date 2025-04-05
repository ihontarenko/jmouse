package org.jmouse.template;

import org.jmouse.el.rendering.Block;
import org.jmouse.el.rendering.Macro;

import java.util.HashMap;
import java.util.Map;

public class TemplateRegistry {

    private final Map<String, Block> blocks = new HashMap<>();
    private final Map<String, Macro> macros = new HashMap<>();
    private final Engine             engine;

    public TemplateRegistry(Engine engine) {
        this.engine = engine;
    }

    public Engine getEngine() {
        return engine;
    }

    public void registerBlock(String name, Block block) {
        blocks.put(name, block);
    }

    public Block getBlock(String name) {
        return blocks.get(name);
    }

    public void registerMacro(String name, Macro macro) {
        macros.put(name, macro);
    }

    public Macro getMacro(String name) {
        return macros.get(name);
    }

    public TemplateRegistry merge(TemplateRegistry child) {
        TemplateRegistry merged = new TemplateRegistry(engine);

        merge(merged, this);
        merge(merged, child);

        return merged;
    }

    public void merge(TemplateRegistry source, TemplateRegistry destination) {
        destination.blocks.putAll(source.blocks);
        destination.macros.putAll(source.macros);
    }

}
