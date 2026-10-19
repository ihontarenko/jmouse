/**
 * Components as their documents write them: a {@code component} or {@code family} jMX document read into a
 * {@link org.jmouse.parallax.component.ComponentModel} — its pins, parameters in SI, equations, states, behaviour, heat
 * law, instruments, displays and how it fails —, every expression in it written in one language
 * ({@link org.jmouse.parallax.component.ModelLanguage}) and every equation's units checked as it is read.
 *
 * <p>The lowest of the circuit engine's three layers: it knows no circuit, no bench and no solver. A circuit is
 * elaborated from these models ({@code circuit.elaboration}), then solved ({@code circuit}); nothing here looks up.</p>
 */
package org.jmouse.parallax.component;
