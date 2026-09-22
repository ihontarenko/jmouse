package org.jmouse.grabber;

/**
 * 🖐️ What to do with a page.
 *
 * <p>The one thing a caller writes for every route, and the reason the module is a library rather
 * than a configuration format: a lambda is already the most flexible condition and the most flexible
 * action there is.</p>
 *
 * <p>⚠️ It returns a {@link Verdict} rather than recording one on the context, so a handler that
 * forgets to decide does not compile.</p>
 */
@FunctionalInterface
public interface PageHandler {

    Verdict handle(PageContext page);

}
