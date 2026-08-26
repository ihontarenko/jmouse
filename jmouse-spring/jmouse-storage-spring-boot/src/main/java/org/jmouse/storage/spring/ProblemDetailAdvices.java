package org.jmouse.storage.spring;

import org.springframework.core.Ordered;

/**
 * 📐 Where a library's error mapping sits relative to a product's.
 *
 * <h3>⚠️ Lowest precedence is not "last", it is "tied"</h3>
 *
 * <p>Spring resolves a handler by walking the {@code @ControllerAdvice} beans <em>in order</em> and
 * taking the first one that offers any matching method. It does not gather every candidate across
 * advices and choose the most specific — so a product's {@code @ExceptionHandler(Exception.class)}
 * does not mean <em>when nothing else matched</em>. It means <em>everything, from the moment that
 * advice is reached</em>.</p>
 *
 * <p>An unannotated {@code @RestControllerAdvice} sits at {@link Ordered#LOWEST_PRECEDENCE}. A library
 * advice sitting there too is therefore not last — it is tied, and a tie is broken by the order beans
 * happened to be registered in, which puts a component-scanned product class ahead of an
 * autoconfigured library one. The library's careful 400-with-a-sentence is then never consulted, and
 * the caller is told <em>an unexpected error occurred</em> about a refusal the library could describe
 * exactly.</p>
 *
 * <p>That is not a hypothetical tie-break: it is what turned every failed URL import, every oversized
 * upload and every unreadable storage key into a 500 with nothing to read, in a product whose
 * interface was already rendering the backend's sentence faithfully.</p>
 *
 * <h3>The three tiers</h3>
 *
 * <p>So a library advice claims {@link #LIBRARY_PRECEDENCE} — ahead of an unordered catch-all, behind
 * anything a product orders deliberately — and the arrangement a product wants is:</p>
 *
 * <ol>
 *   <li>the product's own exceptions, at any order ahead of {@link #LIBRARY_PRECEDENCE}</li>
 *   <li>the libraries' refusals, here</li>
 *   <li>the product's catch-all, <strong>alone in an advice of its own</strong> at
 *       {@link Ordered#LOWEST_PRECEDENCE}</li>
 * </ol>
 *
 * <p>⚠️ <strong>The third one is the product's part of the bargain and no library can do it for them.</strong>
 * A catch-all sharing a class with specific handlers inherits their precedence and swallows everything
 * from there down, however this constant is set.</p>
 */
public final class ProblemDetailAdvices {

    /**
     * The order every advice in this family declares.
     *
     * <p>The gap to {@link Ordered#LOWEST_PRECEDENCE} exists so a product can slot an advice
     * <em>between</em> a library and its own catch-all when it means to answer one library exception
     * differently without taking over the rest.</p>
     */
    public static final int LIBRARY_PRECEDENCE = Ordered.LOWEST_PRECEDENCE - 100;

    private ProblemDetailAdvices() {
    }
}
