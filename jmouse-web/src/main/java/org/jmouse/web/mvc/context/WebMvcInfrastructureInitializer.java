package org.jmouse.web.mvc.context;

import org.jmouse.beans.BeanScanner;
import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.core.Bits;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.web.mvc.*;
import org.jmouse.web.mvc.exception.ExceptionMappingRegistry;
import org.jmouse.web.mvc.method.ArgumentResolver;
import org.jmouse.web.mvc.method.ReturnValueHandler;
import org.jmouse.web.mvc.method.ReturnValueProcessor;
import org.jmouse.web.mvc.method.converter.HttpMessageConverter;
import org.jmouse.web.mvc.method.converter.MessageConverterManager;
import org.jmouse.web.mvc.routing.MappingRegistry;
import org.jmouse.web.negotiation.MediaTypeLookup;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 🛠 Initializes core Web MVC infrastructure components.
 *
 * <p>Scans the specified base packages for essential MVC components such as:
 * <ul>
 *   <li>{@link HandlerMapping}</li>
 *   <li>{@link ReturnValueHandler}</li>
 *   <li>{@link HttpMessageConverter}</li>
 *   <li>{@link MessageConverterManager}</li>
 *   <li>{@link HandlerAdapter}</li>
 *   <li>{@link ArgumentResolver}</li>
 *   <li>{@link ViewResolver}</li>
 *   <li>{@link ExceptionResolver}</li>
 *   <li>{@link MappingRegistry}</li>
 * </ul>
 *
 * <p>Registers found classes into the application context for later use.
 *
 * @author Ivan
 */
public class WebMvcInfrastructureInitializer extends ScannerBeanContextInitializer {

    // ── flag constants (short is enough for 10 bits) ──────────────────────────
    public static final short HANDLER_MAPPING        = 1;        // 0x0001
    public static final short HANDLER_ADAPTER        = 1 << 1;   // 0x0002
    public static final short ARGUMENT_RESOLVER      = 1 << 2;   // 0x0004
    public static final short RETURN_VALUE_HANDLER   = 1 << 3;   // 0x0008
    public static final short RETURN_VALUE_PROCESSOR = 1 << 4;   // 0x0010
    public static final short MESSAGE_CONVERTER      = 1 << 5;   // 0x0020
    public static final short MESSAGE_MANAGER        = 1 << 6;   // 0x0040
    public static final short VIEW_RESOLVER          = 1 << 7;   // 0x0080
    public static final short EXCEPTION_RESOLVER     = 1 << 8;   // 0x0100
    public static final short ROUTING_MAPPING        = 1 << 9;   // 0x0200
    public static final short EXCEPTION_MAPPING      = 1 << 10;  // 0x0400
    public static final short MEDIA_TYPE_LOOKUP      = 1 << 11;  // 0x0800
    public static final short HANDLER_DISPATCHER     = 1 << 12;  // 0x1000

    private static final Map<Short, BeanScanner<AnnotatedElement>> REGISTRATIONS = new HashMap<>(16);

    private final Bits mask;

    static {
        REGISTRATIONS.put(HANDLER_MAPPING, types
                -> new ArrayList<>(ClassFinder.findImplementations(HandlerMapping.class, types)));
        REGISTRATIONS.put(HANDLER_ADAPTER, types
                -> new ArrayList<>(ClassFinder.findImplementations(HandlerAdapter.class, types)));
        REGISTRATIONS.put(ARGUMENT_RESOLVER, types
                -> new ArrayList<>(ClassFinder.findImplementations(ArgumentResolver.class, types)));
        REGISTRATIONS.put(RETURN_VALUE_HANDLER, types
                -> new ArrayList<>(ClassFinder.findImplementations(ReturnValueHandler.class, types)));
        REGISTRATIONS.put(RETURN_VALUE_PROCESSOR, types
                -> new ArrayList<>(ClassFinder.findExactlyClasses(ReturnValueProcessor.class, types)));
        REGISTRATIONS.put(MESSAGE_CONVERTER, types
                -> new ArrayList<>(ClassFinder.findImplementations(HttpMessageConverter.class, types)));
        REGISTRATIONS.put(MESSAGE_MANAGER, types
                -> new ArrayList<>(ClassFinder.findExactlyClasses(MessageConverterManager.class, types)));
        REGISTRATIONS.put(VIEW_RESOLVER, types
                -> new ArrayList<>(ClassFinder.findImplementations(ViewResolver.class, types)));
        REGISTRATIONS.put(EXCEPTION_RESOLVER, types
                -> new ArrayList<>(ClassFinder.findImplementations(ExceptionResolver.class, types)));
        REGISTRATIONS.put(ROUTING_MAPPING, types
                -> new ArrayList<>(ClassFinder.findExactlyClasses(MappingRegistry.class, types)));
        REGISTRATIONS.put(EXCEPTION_MAPPING, types
                -> new ArrayList<>(ClassFinder.findExactlyClasses(ExceptionMappingRegistry.class, types)));
        REGISTRATIONS.put(MEDIA_TYPE_LOOKUP, types
                -> new ArrayList<>(ClassFinder.findImplementations(MediaTypeLookup.class, types)));
        REGISTRATIONS.put(HANDLER_DISPATCHER, types
                -> new ArrayList<>(ClassFinder.findExactlyClasses(HandlerDispatcher.class, types)));
    }

    /**
     * 📦 Default: enable all component groups.
     */
    public WebMvcInfrastructureInitializer(Class<?>... basePackages) {
        this(Bits.of(
                HANDLER_MAPPING,
                HANDLER_ADAPTER,
                ARGUMENT_RESOLVER,
                RETURN_VALUE_HANDLER,
                RETURN_VALUE_PROCESSOR,
                MESSAGE_CONVERTER,
                MESSAGE_MANAGER,
                VIEW_RESOLVER,
                EXCEPTION_RESOLVER,
                ROUTING_MAPPING,
                EXCEPTION_MAPPING,
                MEDIA_TYPE_LOOKUP,
                HANDLER_DISPATCHER
        ), basePackages);
    }

    /**
     * 📦 Custom flags via {@link Bits}.
     */
    public WebMvcInfrastructureInitializer(Bits flags, Class<?>... basePackages) {
        super(basePackages);
        this.mask = (flags != null) ? flags : Bits.of();
        registerSelectedScanners();
    }

    /**
     * 📦 Convenience: pass a precomputed numeric mask (union of flags).
     */
    public WebMvcInfrastructureInitializer(long mask, Class<?>... basePackages) {
        this(Bits.of(mask), basePackages);
    }

    private void registerSelectedScanners() {
        REGISTRATIONS.entrySet().stream()
                .filter(e -> mask.has(e.getKey())).map(Map.Entry::getValue).forEach(this::addScanner);
    }

}

