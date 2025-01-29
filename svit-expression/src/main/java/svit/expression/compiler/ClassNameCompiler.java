package svit.expression.compiler;

import svit.ast.compiler.Compiler;
import svit.ast.compiler.EvaluationContext;
import svit.expression.ast.ClassNameNode;
import org.jmouse.core.reflection.Reflections;

public class ClassNameCompiler implements Compiler<ClassNameNode, Class<?>> {

    @Override
    public Class<?> compile(ClassNameNode node, EvaluationContext evaluationContext) {
        return Reflections.getClassFor(node.getClassName());
    }

    @Override
    public Class<? extends ClassNameNode> nodeType() {
        return ClassNameNode.class;
    }

}
