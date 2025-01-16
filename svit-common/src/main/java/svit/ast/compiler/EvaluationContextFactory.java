package svit.ast.compiler;

import svit.util.Booleans;
import svit.util.Maths;
import svit.reflection.Reflections;

import java.util.Arrays;
import java.util.stream.Stream;

public class EvaluationContextFactory {

    public static final Class<?>[] DEFAULT_FUNCTIONS= new Class[]{
            Maths.class, Booleans.class, Math.class
    };

    public static EvaluationContext newEvaluationContext() {
        return new EvaluationContext();
    }

    public static EvaluationContext defaultEvaluationContext(Class<?>... types) {
        EvaluationContext context = newEvaluationContext();

        Stream.concat(Arrays.stream(types), Arrays.stream(DEFAULT_FUNCTIONS))
                .flatMap(clazz -> Reflections.extractStaticMethods(clazz).stream())
                .forEach(context::setFunction);

        return context;
    }

}
