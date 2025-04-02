package org.jmouse.template;

import org.jmouse.template.node.BlockNode;
import org.jmouse.template.node.MacroNode;

import java.util.HashMap;
import java.util.Map;

public class TemplateRegistry {

    private final Map<String, BlockNode> blocks = new HashMap<>();
    private final Map<String, MacroNode> macros = new HashMap<>();
    private final Engine                 engine;

    public TemplateRegistry(Engine engine) {
        this.engine = engine;
    }

    public Engine getEngine() {
        return engine;
    }

    public void registerBlock(String name, BlockNode block) {
        blocks.put(name, block);
    }

    public BlockNode getBlock(String name) {
        return blocks.get(name);
    }

    public void registerMacro(String name, MacroNode macro) {
        macros.put(name, macro);
    }

    public MacroNode getMacro(String name) {
        return macros.get(name);
    }

}
