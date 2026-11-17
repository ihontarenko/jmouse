package org.jmouse.j2js.teavm.supplement;

import org.jmouse.j2js.teavm.ClassLibrarySubstitution;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 🧩 Gives a JDK class of TeaVM's class library the methods it lacks: for each {@link Supplement} of a {@link Supplements} class,
 * a method of the JDK class, under the supplement's name, that calls the supplement — the object first for an
 * {@link SupplementKind#INSTANCE instance} method.
 *
 * <p>The classes of supplements are those named in {@value #SUPPLEMENTS_RESOURCE} anywhere on the class path TeaVM compiles,
 * read once, when the first class is transformed. A substitute may hold supplements too — one that answers with that substitute
 * must: TeaVM reads a substitute under its JDK name, its references renamed, so its methods are named in JDK types; a class of
 * its own would name the substitute.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SupplementTransformer implements ClassHolderTransformer {

    /** Where a module names its classes of supplements, one class a line. */
    public static final String SUPPLEMENTS_RESOURCE = "META-INF/services/org.jmouse.j2js.teavm.supplement.Supplements";

    private final ClassLoader                         classLoader;
    private       Map<String, List<SuppliedMethod>> suppliedByClass;

    public SupplementTransformer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void transformClass(ClassHolder target, ClassHolderTransformerContext context) {
        List<SuppliedMethod> supplied = suppliedByClass(context).getOrDefault(target.getName(), List.of());

        for (SuppliedMethod method : supplied) {
            MethodHolder existing = target.getMethod(method.descriptor());

            // A gap: a method the class lacks, or a native one TeaVM has no body for. A method with a body is replaced only by a
            // supplement that takes the body's place.
            if (existing != null && !existing.hasModifier(ElementModifier.NATIVE) && method.place() != SupplementPlace.BODY) {
                continue;
            }

            if (existing != null) {
                target.removeMethod(existing);
            }

            target.addMethod(method.delegating(target.getName(), context.getHierarchy()));
        }
    }

    private Map<String, List<SuppliedMethod>> suppliedByClass(ClassHolderTransformerContext context) {
        if (suppliedByClass == null) {
            suppliedByClass = readSupplements(context);
        }
        return suppliedByClass;
    }

    private Map<String, List<SuppliedMethod>> readSupplements(ClassHolderTransformerContext context) {
        Map<String, List<SuppliedMethod>> byClass = new LinkedHashMap<>();

        for (String holderName : supplementClassNames()) {
            ClassReader holder = context.getHierarchy().getClassSource().get(ClassLibrarySubstitution.originalName(holderName));

            if (holder == null) {
                context.getDiagnostics().error(null, "The supplements {{c0}} named in " + SUPPLEMENTS_RESOURCE + " are not on the class path", holderName);
                continue;
            }

            AnnotationReader supplements = holder.getAnnotations().get(Supplements.class.getName());

            if (supplements == null) {
                context.getDiagnostics().error(null, "{{c0}} is named as supplements but is not annotated @Supplements", holderName);
                continue;
            }

            String target = ((ValueType.Object) supplements.getValue("value").getJavaClass()).getClassName();

            for (MethodReader method : holder.getMethods()) {
                AnnotationReader supplement = method.getAnnotations().get(Supplement.class.getName());
                if (supplement != null) {
                    SupplementKind  kind  = SupplementKind.valueOf(supplement.getValue("value").getEnumValue().getFieldName());
                    SupplementPlace place = placeOf(supplement);
                    byClass.computeIfAbsent(target, name -> new ArrayList<>()).add(SuppliedMethod.of(method.getReference(), kind, place));
                }
            }
        }

        return Collections.unmodifiableMap(byClass);
    }

    /** What a supplement takes the place of, as it says — a gap where it says nothing. */
    private static SupplementPlace placeOf(AnnotationReader supplement) {
        AnnotationValue place = supplement.getValue("place");
        return place == null ? SupplementPlace.GAP : SupplementPlace.valueOf(place.getEnumValue().getFieldName());
    }

    /** Every class of supplements named on the class path, each once, in the order found. */
    private Set<String> supplementClassNames() {
        Set<String> names = new LinkedHashSet<>();

        try {
            for (URL resource : Collections.list(classLoader.getResources(SUPPLEMENTS_RESOURCE))) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
                    reader.lines()
                            .map(line -> line.replaceFirst("#.*", "").strip())
                            .filter(line -> !line.isEmpty())
                            .forEach(names::add);
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("The supplements named in " + SUPPLEMENTS_RESOURCE + " could not be read", exception);
        }

        return names;
    }

    /**
     * A method supplied to a JDK class: its descriptor there, the supplement it calls, whether it is called on an object, and
     * what it takes the place of.
     *
     * @param descriptor the method as the JDK class has it
     * @param delegate   the supplement
     * @param kind       an instance or a static method
     * @param place      only a gap, or a body too
     */
    private record SuppliedMethod(MethodDescriptor descriptor, MethodReference delegate, SupplementKind kind, SupplementPlace place) {

        static SuppliedMethod of(MethodReference delegate, SupplementKind kind, SupplementPlace place) {
            ValueType[] parameters = delegate.getParameterTypes();
            ValueType[] own        = kind == SupplementKind.INSTANCE ? Arrays.copyOfRange(parameters, 1, parameters.length) : parameters;
            ValueType[] signature  = Arrays.copyOf(own, own.length + 1);

            signature[own.length] = delegate.getReturnType();

            return new SuppliedMethod(new MethodDescriptor(delegate.getName(), signature), delegate, kind, place);
        }

        /**
         * The JDK class's method: it passes its object (an instance one) and its parameters to the supplement and answers what
         * the supplement answers. A program's variable 0 is the object — reserved in a static method too —, its parameters
         * follow.
         */
        MethodHolder delegating(String targetClass, ClassHierarchy hierarchy) {
            MethodHolder method = new MethodHolder(descriptor);
            method.setLevel(AccessLevel.PUBLIC);

            if (kind == SupplementKind.STATIC) {
                method.getModifiers().add(ElementModifier.STATIC);
            }

            ProgramEmitter emitter    = ProgramEmitter.create(method, hierarchy);
            ValueType[]    parameters = delegate.getParameterTypes();
            ValueEmitter[] arguments  = new ValueEmitter[parameters.length];

            for (int index = 0; index < parameters.length; index++) {
                int variable = kind == SupplementKind.INSTANCE ? index : index + 1;
                arguments[index] = emitter.var(variable, kind == SupplementKind.INSTANCE && index == 0 ? ValueType.object(targetClass) : parameters[index]);
            }

            ValueEmitter result = emitter.invoke(delegate, arguments);

            if (delegate.getReturnType() == ValueType.VOID) {
                emitter.exit();
            } else {
                result.returnValue();
            }

            return method;
        }
    }
}
