package org.jmouse.mapper.management;

import org.jmouse.core.matcher.Matcher;

/**
 * Which classes a product offers in the builder's two selects. 📇
 *
 * <h2>⚠️ A bean the product declares, because there is no universal answer</h2>
 *
 * <p>Innoventa's response types are <strong>nested records inside a {@code …ResponseDtos} holder</strong>.
 * Another product uses a suffix, an annotation, or a package. A library shipping a default of
 * {@code nameEnds("Dto")} would find precisely nothing in the first product it was pointed at — and
 * worse, would look like it was working.</p>
 *
 * <p>So the library serves the screen and the product answers the one question only it can. A product
 * that declares no bean gets no listing, and the screen says so — which is a better failure than a
 * confident empty list.</p>
 *
 * <h2>⚠️ A matcher, not a pattern in configuration</h2>
 *
 * <p>{@code ClassMatchers} composes: {@code isPublic().and(nameEnds("Response").or(isAnnotatedWith(…)))}
 * is a sentence, and no glob in a properties file expresses it. Configuration would also put the answer
 * somewhere a compiler cannot check, for a question whose wrong answer is an empty screen.</p>
 *
 * <pre>{@code
 * @Bean
 * MappableTypeSource mappableTypes() {
 *     return new MappableTypeSource(
 *             isPublic().and(nameEnds("Response").or(nameEnds("Request"))),
 *             InnoventaApplication.class);
 * }
 * }</pre>
 *
 * @param matcher     which classes belong in the list
 * @param baseClasses where to scan from — one class per package root
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record MappableTypeSource(Matcher<Class<?>> matcher, Class<?>... baseClasses) {
}
