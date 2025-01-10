package svit.expression.compiler;

import svit.ast.compiler.EvaluationContext;
import svit.ast.configurer.Configurator;

public class EvaluationContextConfigurator implements Configurator<EvaluationContext> {

    @Override
    public void configure(EvaluationContext ctx) {
        ctx.addCompiler(new AnyCompiler());
        ctx.addCompiler(new LiteralCompiler());
        ctx.addCompiler(new ClassNameCompiler());
        ctx.addCompiler(new VariableCompiler());
        ctx.addCompiler(new FunctionCompiler());
        ctx.addCompiler(new MethodCompiler());
        ctx.addCompiler(new IdentifierCompiler());
        ctx.addCompiler(new ParametersCompiler());
        ctx.addCompiler(new ValuesCompiler());
        ctx.addCompiler(new StringDefinitionCompiler());
        ctx.addCompiler(new PathVariableCompiler());
    }

}
