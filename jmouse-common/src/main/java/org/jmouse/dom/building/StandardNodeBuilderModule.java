package org.jmouse.dom.building;

/**
 * Registers standard node builders for common Java data shapes.
 */
public final class StandardNodeBuilderModule {

    private StandardNodeBuilderModule() {}

    public static void registerAll(NodeBuilderRegistry registry) {
        registry.register(new StandardSelectOptionsNodeBuilder());
    }

}
