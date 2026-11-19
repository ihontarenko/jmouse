package org.jmouse.java.bytecode.validation;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.el.translate.NodeOperation;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.bytecode.JavaBytecodeLanguage;
import org.jmouse.java.bytecode.JavaBytecodeMode;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.JavaFieldNode;
import org.jmouse.java.bytecode.node.JavaMemberNode;
import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationNode;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationValueNode;
import org.jmouse.java.bytecode.node.attribute.JavaAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaBootstrapMethodsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLineNumberTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableTypeTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleMainClassAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaModulePackagesAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypeAnnotationNode;
import org.jmouse.java.bytecode.node.constant.*;
import org.jmouse.java.bytecode.node.instruction.JavaBranchInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaConstantInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaImmediateInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaIncrementInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInvokeDynamicInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInvokeInterfaceInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaLocalInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaLookupSwitchInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaMultiArrayInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaNoOperandInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaPrimitiveArrayInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaTableSwitchInstructionNode;
import org.jmouse.java.bytecode.vocabulary.JvmClassAccessFlag;

import java.util.function.BiConsumer;

/**
 * Exact handlers use the shared jME registry and walker; plugins can bind or decorate validations.
 */
public final class JavaBytecodeValidation {
    private JavaBytecodeValidation() {}

    public static NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome>
            standardRules() {
        NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> rules =
                new NodeOperationRegistry<>(JavaBytecodeNames.VALIDATION);
        JavaAttributeStructureValidation.install(rules);
        JavaAnnotationStructureValidation.install(rules);
        rules.bind(
                JavaLineNumberTableAttributeNode.KIND,
                JavaDebugTableStructureValidation.class.getName(),
                JavaDebugTableStructureValidation::lines);
        rules.bind(
                JavaLocalVariableTableAttributeNode.KIND,
                JavaDebugTableStructureValidation.class.getName(),
                JavaDebugTableStructureValidation::locals);
        rules.bind(
                JavaLocalVariableTypeTableAttributeNode.KIND,
                JavaDebugTableStructureValidation.class.getName(),
                JavaDebugTableStructureValidation::genericLocals);
        rules.bind(
                JavaModuleAttributeNode.KIND,
                JavaModuleStructureValidation.class.getName(),
                JavaModuleStructureValidation::module);
        rules.bind(
                JavaModulePackagesAttributeNode.KIND,
                JavaModuleStructureValidation.class.getName(),
                JavaModuleStructureValidation::packages);
        rules.bind(
                JavaModuleMainClassAttributeNode.KIND,
                JavaModuleStructureValidation.class.getName(),
                JavaModuleStructureValidation::mainClass);
        rules.bind(
                JavaClassFileNode.KIND,
                JavaClassStructureValidation.class.getName(),
                JavaClassStructureValidation::validate);
        rules.bind(
                JavaStackMapTableAttributeNode.KIND,
                JavaStackMapStructureValidation.class.getName(),
                JavaStackMapStructureValidation::validate);
        rules.bind(
                JavaBootstrapMethodsAttributeNode.KIND,
                JavaBootstrapStructureValidation.class.getName(),
                JavaBootstrapStructureValidation::validate);
        rules.bind(
                JavaCodeAttributeNode.KIND,
                JavaCodeStructureValidation.class.getName(),
                JavaCodeStructureValidation::code);
        bindInstruction(
                rules,
                JavaBranchInstructionNode.KIND,
                JavaCodeStructureValidation.class.getName(),
                JavaCodeStructureValidation::branch);
        bindInstruction(
                rules,
                JavaConstantInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::constant);
        bindInstruction(
                rules,
                JavaNoOperandInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::noOperand);
        bindInstruction(
                rules,
                JavaImmediateInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::local);
        bindInstruction(
                rules,
                JavaLocalInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::local);
        bindInstruction(
                rules,
                JavaIncrementInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::local);
        bindInstruction(
                rules,
                JavaPrimitiveArrayInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::local);
        bindInstruction(
                rules,
                JavaTableSwitchInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::tableSwitch);
        bindInstruction(
                rules,
                JavaLookupSwitchInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::lookupSwitch);
        bindInstruction(
                rules,
                JavaInvokeInterfaceInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::interfaceCall);
        bindInstruction(
                rules,
                JavaInvokeDynamicInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::dynamicCall);
        bindInstruction(
                rules,
                JavaMultiArrayInstructionNode.KIND,
                JavaInstructionStructureValidation.class.getName(),
                JavaInstructionStructureValidation::multiArray);
        rules.bind(
                JavaFieldNode.KIND,
                JavaMemberStructureValidation.class.getName(),
                JavaMemberStructureValidation::field);
        rules.bind(
                JavaMethodNode.KIND,
                JavaMemberStructureValidation.class.getName(),
                JavaMemberStructureValidation::method);
        bind(rules, JavaUtf8ConstantNode.KIND, (node, context) -> {});
        bind(rules, JavaIntegerConstantNode.KIND, (node, context) -> {});
        bind(rules, JavaFloatConstantNode.KIND, (node, context) -> {});
        bind(rules, JavaLongConstantNode.KIND, (node, context) -> {});
        bind(rules, JavaDoubleConstantNode.KIND, (node, context) -> {});
        bind(rules, JavaClassConstantNode.KIND, (node, context) -> context.className(node.index()));
        bind(
                rules,
                JavaStringConstantNode.KIND,
                (node, context) -> context.utf8(node.stringIndex()));
        bind(rules, JavaNameAndTypeConstantNode.KIND, JavaConstantStructureValidation::nameAndType);
        bind(
                rules,
                JavaFieldReferenceConstantNode.KIND,
                (node, context) ->
                        JavaConstantStructureValidation.fieldReference(
                                node.classIndex(), node.nameAndTypeIndex(), context));
        bind(
                rules,
                JavaMethodReferenceConstantNode.KIND,
                (node, context) ->
                        JavaConstantStructureValidation.methodReference(
                                node, node.classIndex(), node.nameAndTypeIndex(), context));
        bind(
                rules,
                JavaInterfaceMethodReferenceConstantNode.KIND,
                (node, context) ->
                        JavaConstantStructureValidation.methodReference(
                                node, node.classIndex(), node.nameAndTypeIndex(), context));
        bind(
                rules,
                JavaMethodHandleConstantNode.KIND,
                JavaConstantStructureValidation::methodHandle);
        bind(
                rules,
                JavaMethodTypeConstantNode.KIND,
                (node, context) -> {
                    context.require(
                            context.root().version().major()
                                    >= JvmValidationVersions.METHOD_HANDLES,
                            node,
                            "Method types are unavailable in this classfile version");
                    context.method(context.utf8(node.descriptorIndex()));
                });
        bind(
                rules,
                JavaDynamicConstantNode.KIND,
                (node, context) ->
                        JavaConstantStructureValidation.dynamic(
                                node,
                                node.bootstrapMethodIndex(),
                                node.nameAndTypeIndex(),
                                context,
                                JavaConstantStructureValidation.DynamicKind.CONSTANT));
        bind(
                rules,
                JavaInvokeDynamicConstantNode.KIND,
                (node, context) ->
                        JavaConstantStructureValidation.dynamic(
                                node,
                                node.bootstrapMethodIndex(),
                                node.nameAndTypeIndex(),
                                context,
                                JavaConstantStructureValidation.DynamicKind.CALL));
        bind(
                rules,
                JavaModuleConstantNode.KIND,
                (node, context) -> {
                    moduleConstant(node, context);
                    JavaModuleStructureValidation.moduleName(node.index(), context);
                });
        bind(
                rules,
                JavaPackageConstantNode.KIND,
                (node, context) -> {
                    moduleConstant(node, context);
                    JavaModuleStructureValidation.packageName(node.index(), context);
                });
        return rules;
    }

    private static <N extends JavaInstructionNode> void bindInstruction(
            NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> rules,
            NodeKind<N> kind,
            String identity,
            NodeOperation<N, JvmValidationContext, JvmValidationOutcome> validation) {
        rules.bind(
                kind,
                identity,
                (node, context) -> {
                    JavaInstructionStructureValidation.admission(node, context);
                    return validation.apply(node, context);
                });
    }

    private static void moduleConstant(JavaBytecodeNode node, JvmValidationContext context) {
        context.require(
                context.root().version().major() >= JvmValidationVersions.MODULES
                        && (context.root().accessFlags() & JvmClassAccessFlag.MODULE.code()) != 0,
                node,
                "Module/package constants require a module descriptor");
    }

    private static <N extends JavaBytecodeNode> void bind(
            NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> rules,
            NodeKind<N> kind,
            BiConsumer<N, JvmValidationContext> validation) {
        rules.bind(
                kind,
                JavaConstantStructureValidation.class.getName(),
                (node, context) -> {
                    validation.accept(node, context);
                    return JvmValidationOutcome.VALID;
                });
    }

    public static void validate(
            JavaClassFileNode root,
            JavaBytecodeLanguage language,
            JavaBytecodeMode mode,
            ParseSession session) {
        JvmValidationContext context = new JvmValidationContext(root, language, mode, session);
        root.accept(
                new Visitor() {
                    @Override
                    public void visit(Node node) {
                        if (node instanceof JavaAttributeNode
                                || node instanceof JavaAnnotationNode
                                || node instanceof JavaAnnotationValueNode
                                || node instanceof JavaTypeAnnotationNode
                                || node instanceof JavaInstructionNode
                                || node instanceof JavaClassFileNode
                                || node instanceof JavaMemberNode
                                || node instanceof JavaConstantNode) {
                            JavaBytecodeNode binary = (JavaBytecodeNode) node;
                            context.at(
                                    binary,
                                    () -> {
                                        language.validations().apply(binary, context);
                                    });
                        }
                    }
                });
    }
}
