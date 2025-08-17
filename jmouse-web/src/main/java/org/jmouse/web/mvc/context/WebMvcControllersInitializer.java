package org.jmouse.web.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.web.annotation.Controller;
import org.jmouse.core.reflection.ClassFinder;

import java.util.ArrayList;

/**
 * 🔎 Scans for {@link Controller @Controller}-annotated classes and registers them as beans.
 *
 * <p>This initializer wires the MVC controller layer by discovering classes annotated with
 * {@code @Controller} within the provided base packages (or the defaults used by
 * {@link ScannerBeanContextInitializer}, depending on your implementation) and adding them
 * to the application context.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Scan specific packages
 * context.addInitializer(new WebMvcControllersInitializer(com.example.web.App.class));
 *
 * // Or rely on the base behavior of ScannerBeanContextInitializer (if it supports defaults)
 * context.addInitializer(new WebMvcControllersInitializer());
 * }</pre>
 *
 * <h2>What it does</h2>
 * <ul>
 *   <li>Searches the given packages for classes annotated with {@link Controller}.</li>
 *   <li>Registers found controllers so they can be mapped by your MVC infrastructure.</li>
 * </ul>
 *
 * @author Ivan
 * @see Controller
 * @see ScannerBeanContextInitializer
 */
public class WebMvcControllersInitializer extends ScannerBeanContextInitializer {

    /**
     * Create a controller-scanning initializer.
     *
     * @param basePackages one or more classes whose packages will be used as scan roots
     *                     (if omitted, behavior falls back to the parent initializer's default strategy)
     */
    public WebMvcControllersInitializer(Class<?>... basePackages) {
        super(basePackages);

        addScanner(types -> new ArrayList<>(
                ClassFinder.findAnnotatedClasses(Controller.class, types)));
    }
}
