package org.jmouse.mapper.el.builder;

import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassFinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * What a builder offers in its two selects, and what a chosen pair is made of. 📇
 *
 * <h2>⚠️ The scan is a LISTING, never a filter on what may be mapped</h2>
 *
 * <p>Two questions that are easy to merge by accident, and merging them invents a restriction the
 * engine does not have:</p>
 *
 * <table border="1">
 *   <caption>What the scan decides and what it does not</caption>
 *   <tr><td>what to offer in the select</td>
 *       <td>a convenience — a product narrows it so the list is usable</td></tr>
 *   <tr><td>what may be a mapping target</td>
 *       <td>⚠️ <strong>not this.</strong> The engine maps whatever it is handed, so a type the scan did
 *           not find is still mappable</td></tr>
 * </table>
 *
 * <p>Which is why {@link #named(String)} exists beside {@link #offered}: a caller must be able to name
 * a type the list does not carry, and be given its properties like any other.</p>
 *
 * <h2>⚠️ The matcher is the PRODUCT's, not this library's</h2>
 *
 * <p>There is no universal answer to "which classes are mappable". Innoventa's response types are
 * <strong>nested records inside a {@code …ResponseDtos} holder</strong>; another product uses a suffix,
 * an annotation, or a package. A default of {@code nameEnds("Dto")} would find precisely nothing in the
 * product this was first written against.</p>
 *
 * <p>So the matcher is passed in, and it is a {@link Matcher} rather than a pattern in configuration —
 * a matcher composes, and composition is the whole reason {@code ClassMatchers} is worth reusing:
 * {@code isPublic().and(nameEnds("Response").or(isAnnotatedWith(Mapped.class)))} is a sentence, and no
 * glob expresses it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class MappableTypes {

    private MappableTypes() {
    }

    /**
     * The types to offer, as a product's matcher describes them.
     *
     * @param matcher     which classes belong in the list
     * @param baseClasses where to scan from — a class per package root
     * @return what to show, in package order
     */
    public static List<MappableType> offered(Matcher<Class<?>> matcher, Class<?>... baseClasses) {
        Collection<Class<?>> found = ClassFinder.findAll(
                matcher, List.of(ClassFinder.ORDER_PACKAGE_NAME), baseClasses);

        List<MappableType> offered = new ArrayList<>();

        for (Class<?> candidate : found) {
            offered.add(new MappableType(candidate.getName(), candidate.getSimpleName(),
                                         candidate.getPackageName()));
        }

        return offered;
    }

    /**
     * A type the list did not offer, named outright.
     *
     * @param qualified the type's name
     * @return what it is made of
     * @throws IllegalArgumentException where nothing on the classpath answers to that name
     */
    public static MappableShape named(String qualified) {
        try {
            return shapeOf(Class.forName(qualified, false,
                                         Thread.currentThread().getContextClassLoader()));
        } catch (ClassNotFoundException absent) {
            throw new IllegalArgumentException(
                    ("nothing on the classpath is called '%s'. ⚠️ A nested type is written the way a "
                     + "class loader spells it — Outer$Inner, not Outer.Inner")
                            .formatted(qualified));
        }
    }

    /**
     * What a type is made of — its readable properties and its writable ones.
     *
     * <h3>⚠️ Both, for both sides of a pair</h3>
     *
     * <p>A form needs the target's <em>writable</em> properties for its left column and the source's
     * <em>readable</em> ones for its right, and which of the two a class is playing changes with the
     * direction of the mapping. Serving one list with a flag on each entry is what lets one type be a
     * source on one screen and a target on the next without being described twice.</p>
     *
     * @param type the class
     * @return its properties
     */
    public static MappableShape shapeOf(Class<?> type) {
        ObjectDescriptor<Object> descriptor = describe(type);
        List<MappableProperty>   properties = new ArrayList<>();

        // ⚠️ A record is asked a different question, and asking it the JavaBean one used to throw — so
        // this screen could not open a single one of a product's response types, which are commonly
        // records. What a record can be given is its COMPONENTS: it is built through its constructor,
        // and every component is therefore both readable and writable. `isWritable()` answers `false`
        // for one and is right to — a component has no setter — which is exactly why the question has
        // to be asked of the right thing rather than answered from the wrong one.
        if (descriptor instanceof ValueObjectDescriptor<?> record) {
            for (PropertyDescriptor<?> component : record.getComponents().values()) {
                properties.add(new MappableProperty(
                        component.getName(),
                        component.getType().getJavaType().getName(),
                        true,
                        true));
            }

            return new MappableShape(type.getName(), type.getSimpleName(), properties);
        }

        for (PropertyDescriptor<Object> property : descriptor.getProperties().values()) {
            properties.add(new MappableProperty(
                    property.getName(),
                    property.getType().getJavaType().getName(),
                    property.isReadable(),
                    property.isWritable()));
        }

        return new MappableShape(type.getName(), type.getSimpleName(), properties);
    }

    @SuppressWarnings("unchecked")
    private static ObjectDescriptor<Object> describe(Class<?> type) {
        return (ObjectDescriptor<Object>) DescriptorResolver.describe(type);
    }

    /**
     * One entry of a select.
     *
     * @param qualified  the name a document writes in a {@code use} line
     * @param simple     the name a document writes everywhere else
     * @param packageName where it lives, so a list of forty can be grouped
     */
    public record MappableType(String qualified, String simple, String packageName) {
    }

    /**
     * What a chosen type is made of.
     *
     * @param qualified  its name
     * @param simple     its short name
     * @param properties everything it carries
     */
    public record MappableShape(String qualified, String simple, List<MappableProperty> properties) {
    }

    /**
     * One property of a type.
     *
     * <p>⚠️ {@code readable} and {@code writable} are separate flags rather than one direction, because
     * a property is routinely both and occasionally neither — and a form that assumed "not writable
     * means readable" would offer a computed getter as a target and produce a document that refuses to
     * load.</p>
     *
     * @param name     what a rule writes
     * @param type     what it holds, so a form can say whether a value will convert
     * @param readable whether it can stand on the right of a rule
     * @param writable whether it can stand on the left
     */
    public record MappableProperty(String name, String type, boolean readable, boolean writable) {
    }
}
