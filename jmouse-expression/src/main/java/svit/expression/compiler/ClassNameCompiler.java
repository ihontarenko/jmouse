package svit.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
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
