package org.jmouse.core.proxy;

import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;

import java.util.Optional;

/**
 * 📦 Registrar that scans the classpath for {@link MethodInterceptor}s annotated with {@link Intercept}
 * and registers them into an {@link InterceptorRegistry}.
 *
 * <p>This component centralizes interceptor discovery and registration:
 * instead of manually adding interceptors, they can be automatically discovered
 * based on the {@link Intercept} annotation.</p>
 *
 * <h3>Workflow</h3>
 * <ol>
 *   <li>Scan the provided base classes/packages for classes annotated with {@link Intercept}.</li>
 *   <li>Extract target classes and priority from the annotation.</li>
 *   <li>Instantiate the interceptor via its first available constructor.</li>
 *   <li>Register it in the {@link InterceptorRegistry} with the corresponding {@link InterceptorMatcher}.</li>
 * </ol>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>✔️ Scanning is performed only once; repeated calls to {@link #register(Class[])} are ignored after the first run.</li>
 *   <li>🎯 If {@link Intercept#value()} is empty, the interceptor is registered with a matcher that applies to any type.</li>
 *   <li>⚖️ {@link Intercept#priority()} determines interceptor ordering within the registry.</li>
 * </ul>
 *
 * <pre>{@code
 * InterceptorRegistry registry = new InterceptorRegistry();
 * InterceptorRegistrar registrar = new InterceptorRegistrar(registry);
 * registrar.register(Application.class);
 *
 * // Now all interceptors annotated with @Intercept inside this package are active.
 * }</pre>
 */
public class InterceptorRegistrar {

    private final InterceptorRegistry registry;
    private       boolean             scanned = false;

    public InterceptorRegistrar(InterceptorRegistry registry) {
        this.registry = registry;
    }

    /**
     * Extract target classes defined by {@link Intercept#value()} on the given interceptor class.
     *
     * @param interceptorClass class annotated with {@link Intercept}
     * @return array of target classes (possibly empty)
     */
    private static Class<?>[] extractTargetClasses(Class<?> interceptorClass) {
        Class<?>[]                 classes    = new Class<?>[0];
        AnnotationRepository       repository = AnnotationRepository.ofAnnotatedElement(interceptorClass);
        Optional<MergedAnnotation> optional   = repository.get(Intercept.class);

        if (optional.isPresent()) {
            MergedAnnotation annotation = optional.get();
            Intercept        intercept  = annotation.synthesize();
            classes = intercept.value();
        }

        return classes;
    }

    /**
     * Scan for interceptors annotated with {@link Intercept} under the given base classes
     * and register them in the {@link InterceptorRegistry}.
     *
     * @param baseClasses root classes/packages to scan
     */
    public void register(Class<?>... baseClasses) {
        if (!scanned || baseClasses == null || baseClasses.length == 0) {
            for (Class<?> annotatedClass : ClassFinder.findAnnotatedClasses(Intercept.class, baseClasses)) {
                Intercept          intercept        = annotatedClass.getAnnotation(Intercept.class);
                Class<?>[]         supportedClasses = extractTargetClasses(annotatedClass);
                InterceptorMatcher matcher          = InterceptorMatcher.any();
                MethodInterceptor  interceptor      = (MethodInterceptor) Reflections.instantiate(
                        Reflections.findFirstConstructor(annotatedClass));

                if (supportedClasses.length > 0) {
                    matcher = InterceptorMatcher.forClasses(supportedClasses);
                }

                registry.register(interceptor, matcher, intercept.priority());
            }
            scanned = true;
        }
    }
}
