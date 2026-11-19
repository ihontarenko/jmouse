package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaTypeKey;

/** Defines run-time package equality for the compilation's class-loader/module universe. */
@FunctionalInterface
public interface RuntimePackageStrategy {

    boolean samePackage(JavaTypeKey first, JavaTypeKey second);

    /**
     * Default closed universe: one package identity per selected module and binary package name.
     */
    static RuntimePackageStrategy moduleScoped() {
        return (first, second) ->
                first.module().equals(second.module())
                        && packageName(first).equals(packageName(second));
    }

    private static String packageName(JavaTypeKey type) {
        int separator = type.binaryName().lastIndexOf('.');
        return separator < 0 ? "" : type.binaryName().substring(0, separator);
    }
}
