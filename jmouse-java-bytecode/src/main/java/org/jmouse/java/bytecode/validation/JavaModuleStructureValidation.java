package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.node.attribute.*;
import org.jmouse.java.bytecode.node.constant.JavaModuleConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaPackageConstantNode;
import org.jmouse.java.bytecode.vocabulary.JvmClassAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmModuleAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmModuleNames;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JavaModuleStructureValidation {
    private JavaModuleStructureValidation() {}

    public static JvmValidationOutcome module(
            JavaModuleAttributeNode node, JvmValidationContext context) {
        requireModule(node, context);
        String current = moduleName(node.moduleNameIndex(), context);
        optionalVersion(node.moduleVersionIndex(), context);
        context.require(
                (node.moduleFlags() & JvmModuleAccessFlag.OPEN.code()) == 0
                        || node.opens().isEmpty(),
                node,
                "An open module cannot contain an opens table");
        Set<String> required = new HashSet<>();
        for (JavaModuleRequireNode entry : node.requires()) {
            context.at(
                    entry,
                    () -> {
                        String name = moduleName(entry.moduleIndex(), context);
                        context.require(
                                !name.equals(current) && required.add(name),
                                entry,
                                "Duplicate or self-referential module requirement");
                        optionalVersion(entry.versionIndex(), context);
                    });
        }
        Set<String> exported = new HashSet<>();
        for (JavaModuleExportNode entry : node.exports()) {
            context.at(
                    entry,
                    () -> {
                        context.require(
                                exported.add(packageName(entry.packageIndex(), context)),
                                entry,
                                "Duplicate exported package");
                        targetModules(entry, entry.targetModuleIndices(), context);
                    });
        }
        Set<String> opened = new HashSet<>();
        for (JavaModuleOpenNode entry : node.opens()) {
            context.at(
                    entry,
                    () -> {
                        context.require(
                                opened.add(packageName(entry.packageIndex(), context)),
                                entry,
                                "Duplicate opened package");
                        targetModules(entry, entry.targetModuleIndices(), context);
                    });
        }
        classNames(node, node.usesIndices(), context);
        Set<String> services = new HashSet<>();
        for (JavaModuleProvideNode entry : node.provides()) {
            context.at(
                    entry,
                    () -> {
                        String service = context.className(entry.serviceIndex());
                        new JavaClassDescriptor(service);
                        context.require(services.add(service), entry, "Duplicate provided service");
                        context.require(
                                !entry.implementationIndices().isEmpty(),
                                entry,
                                "A provided service requires at least one implementation");
                        classNames(entry, entry.implementationIndices(), context);
                    });
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome packages(
            JavaModulePackagesAttributeNode node, JvmValidationContext context) {
        requireModule(node, context);
        Set<String> packages = new HashSet<>();
        for (int index : node.packageIndices()) {
            context.session().step();
            context.require(
                    packages.add(packageName(index, context)), node, "Duplicate module package");
        }
        return JvmValidationOutcome.VALID;
    }

    public static JvmValidationOutcome mainClass(
            JavaModuleMainClassAttributeNode node, JvmValidationContext context) {
        requireModule(node, context);
        new JavaClassDescriptor(context.className(node.mainClassIndex()));
        return JvmValidationOutcome.VALID;
    }

    public static String moduleName(int index, JvmValidationContext context) {
        JavaModuleConstantNode module = context.constant(index, JavaModuleConstantNode.class);
        return JvmModuleNames.decode(context.utf8(module.nameIndex()));
    }

    public static String packageName(int index, JvmValidationContext context) {
        JavaPackageConstantNode entry = context.constant(index, JavaPackageConstantNode.class);
        String name = context.utf8(entry.nameIndex());
        new JavaClassDescriptor(name);
        return name;
    }

    private static void optionalVersion(int index, JvmValidationContext context) {
        if (index != 0) {
            context.utf8(index);
        }
    }

    private static void requireModule(JavaAttributeNode node, JvmValidationContext context) {
        context.require(
                (context.root().accessFlags() & JvmClassAccessFlag.MODULE.code()) != 0,
                node,
                "Module attributes require a module descriptor");
    }

    private static void targetModules(
            JavaBytecodeNode node, List<Integer> indices, JvmValidationContext context) {
        Set<String> modules = new HashSet<>();
        for (int index : indices) {
            context.session().step();
            context.require(
                    modules.add(moduleName(index, context)), node, "Duplicate target module");
        }
    }

    private static void classNames(
            JavaBytecodeNode node, List<Integer> indices, JvmValidationContext context) {
        Set<String> classes = new HashSet<>();
        for (int index : indices) {
            context.session().step();
            String name = context.className(index);
            new JavaClassDescriptor(name);
            context.require(classes.add(name), node, "Duplicate service or implementation class");
        }
    }
}
