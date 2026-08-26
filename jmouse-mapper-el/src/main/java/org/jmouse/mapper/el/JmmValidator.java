package org.jmouse.mapper.el;

import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.access.descriptor.structured.record.ValueObjectDescriptor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.mapper.el.node.FromNode;
import org.jmouse.mapper.el.node.IncludeNode;
import org.jmouse.mapper.el.node.LetNode;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.RuleNode;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.parser.JmmSyntaxException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks a document against the types it names, before a single object is mapped. 🔍
 *
 * <h2>⚠️ Why this is worth more than any syntax in the language</h2>
 *
 * <p>A mistyped rule is <strong>silent</strong>. Rules are looked up by the target's property name as
 * each property is filled, so a rule written for a name the target does not have is stored under a key
 * nothing ever asks for: the file parses, loads, and does nothing, while the mapping quietly falls back
 * to same-named copying — which usually <em>almost</em> works. The same failure as a dotted left-hand
 * side, and it deserves the same answer: refuse when the file is read.</p>
 *
 * <h2>⚠️ Where it stops, and why stopping matters as much as checking</h2>
 *
 * <p>A check that is too strict refuses a valid file, and from a text file there is no way around it —
 * no cast, no annotation, no escape hatch. So the right-hand side is checked only where it
 * <em>actually reads the source</em>, which is exactly the shape a typo takes and the shape that carries
 * no ambiguity. An expression is left alone: proving which of its bare words are source properties,
 * rather than functions, filters, bound names or literals, means walking a compiled tree, and every word
 * missed would be a valid file refused.</p>
 *
 * <p>⚠️ <strong>And a bare name a block bound is not a source path either</strong>, though it looks
 * exactly like one. {@code buyerName : full} where {@code full} is a {@code let} reads nothing off the
 * source, and checking it against the source's properties refuses it every time — which is what it did,
 * for long enough that the reference document's own worked example could not be loaded. What decides it
 * is {@link SourcePath}, and the binder asks the same question there for the same reason.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmValidator {

    private JmmValidator() {
    }

    /**
     * Checks everything that applies to one pair.
     *
     * @param document   the file, for its fragments
     * @param target     the target block
     * @param from       the source block
     * @param sourceType the resolved source type
     * @param targetType the resolved target type
     * @param fed        the target properties the rules cover
     */
    public static void validate(
            MappingDocumentNode document,
            TargetNode target,
            FromNode from,
            Class<?> sourceType,
            Class<?> targetType,
            Set<String> fed
    ) {
        Set<String> writable = writableNames(targetType);
        Set<String> readable = readableNames(sourceType);

        for (RuleBlockNode block : blocks(document, target, from)) {
            checkRules(block, writable, readable, targetType, sourceType);
            checkBindings(block, readable, sourceType);
        }

        if (target.getUnmapped() == TargetNode.Unmapped.FAIL) {
            checkEverythingIsFed(writable, readable, fed, targetType, sourceType, from.getSpan());
        }
    }

    /**
     * Every block that contributes to this pair.
     *
     * @param document the file, for its fragments
     * @param target   the target block
     * @param from     the source block
     * @return the blocks, without duplicates
     */
    private static List<RuleBlockNode> blocks(
            MappingDocumentNode document,
            TargetNode target,
            FromNode from
    ) {
        List<RuleBlockNode> blocks = new ArrayList<>();

        collect(document, from.getRules(), blocks);
        collect(document, target.getAlways(), blocks);

        return blocks;
    }

    /**
     * Adds a block and whatever it includes.
     *
     * @param document the file, for its fragments
     * @param block    the block, or {@code null}
     * @param into     where blocks are collected
     */
    private static void collect(MappingDocumentNode document, RuleBlockNode block, List<RuleBlockNode> into) {
        if (block == null) {
            return;
        }

        into.add(block);

        // ⚠️ A fragment that is not declared is left to the binder, which refuses it naming the line.
        // Refusing it twice, differently, is how two messages for one mistake start to drift.
        for (IncludeNode include : block.getIncludes()) {
            RuleBlockNode fragment = document.getFragments().get(include.getName());

            if (fragment != null && !into.contains(fragment)) {
                into.add(fragment);
            }
        }
    }

    /**
     * Checks each rule's target name, and its source path where it has one.
     *
     * @param block      the block to check
     * @param writable   what the target can be given
     * @param readable   what the source can be asked for
     * @param targetType the target type, for the message
     * @param sourceType the source type, for the message
     */
    private static void checkRules(
            RuleBlockNode block,
            Set<String> writable,
            Set<String> readable,
            Class<?> targetType,
            Class<?> sourceType
    ) {
        Set<String> bound = boundNames(block);

        for (RuleNode rule : block.getRules().values()) {
            if (!writable.contains(rule.getProperty())) {
                throw failure(rule.getSpan(), ("'%s' is not a writable property of %s. It has: %s")
                        .formatted(rule.getProperty(), targetType.getSimpleName(), list(writable)));
            }

            if (rule.isIgnored()) {
                continue;
            }

            // ⚠️ Only a value that actually reads the source. Two things are skipped and for different
            // reasons: an expression, because its bare words are not necessarily source properties and
            // proving which are means walking a compiled tree; and a bare name this block BOUND, because
            // it is not a source path at all — it is a `let`, and checking it against the source refuses
            // it every time. That second case used to be missing, and it made the reference document's
            // own worked example unloadable. Both halves of the language ask SourcePath now.
            if (!SourcePath.readsSource(rule.getValue(), bound)) {
                continue;
            }

            String root = SourcePath.root(rule.getValue());

            if (!readable.contains(root)) {
                throw failure(rule.getSpan(), ("'%s' cannot be read from %s. It has: %s")
                        .formatted(root, sourceType.getSimpleName(), list(readable)));
            }
        }
    }

    /**
     * What one block's {@code let} lines name.
     *
     * <p>⚠️ Per block, never gathered across the pair. A binding is scoped to the block it is written
     * in, so a name bound in {@code always} does not make the same name legal in a {@code from} — and a
     * set built from both would quietly stop refusing a typo that happens to match a binding somewhere
     * else in the file.</p>
     *
     * @param block the block being checked
     * @return the names it binds
     */
    private static Set<String> boundNames(RuleBlockNode block) {
        Set<String> names = new LinkedHashSet<>();

        for (LetNode binding : block.getBindings()) {
            names.add(binding.getName());
        }

        return names;
    }

    /**
     * Checks that no binding shadows a source property.
     *
     * <p>⚠️ Shadowing is legal in most languages and is a trap here, because a binding and a source path
     * are both bare identifiers on the same line and nothing on the page says which one a name meant.
     * Refused rather than resolved by a precedence rule nobody would remember.</p>
     *
     * @param block      the block to check
     * @param readable   what the source can be asked for
     * @param sourceType the source type, for the message
     */
    private static void checkBindings(RuleBlockNode block, Set<String> readable, Class<?> sourceType) {
        for (LetNode binding : block.getBindings()) {
            if (readable.contains(binding.getName())) {
                throw failure(binding.getSpan(), ("'%s' is already a property of %s, and a rule reading "
                        + "that name could mean either. Give the binding a different name")
                        .formatted(binding.getName(), sourceType.getSimpleName()));
            }
        }
    }

    /**
     * Refuses the file while any writable target property is fed by nothing.
     *
     * @param writable   what the target can be given
     * @param readable   what the source can be asked for
     * @param fed        what the rules cover
     * @param targetType the target type, for the message
     * @param sourceType the source type, for the message
     * @param span       the {@code from} block, because the check is computed per source
     */
    private static void checkEverythingIsFed(
            Set<String> writable,
            Set<String> readable,
            Set<String> fed,
            Class<?> targetType,
            Class<?> sourceType,
            SpanNode span
    ) {
        Set<String> unfed = new TreeSet<>(writable);

        unfed.removeAll(fed);
        unfed.removeAll(readable);

        if (!unfed.isEmpty()) {
            // ⚠️ Positioned at the `from`, not at the `target`. `unmapped fail` is written once on the
            // target and computed once per source, so a target with three sources can fail this for one
            // of them — and naming the target would point at a line that is fine.
            throw JmmSyntaxException.at(span, ("'unmapped fail' is set, and %s has nothing to fill "
                    + "%s from %s — no rule names them and %s has no property of the same name")
                    .formatted(targetType.getSimpleName(), list(unfed), sourceType.getSimpleName(),
                               sourceType.getSimpleName()));
        }
    }

    /**
     * What a target can be given.
     *
     * <p>⚠️ A record has no writable properties in the JavaBean sense — it is built through its
     * components, and a check that understood only setters would refuse every record target, which is a
     * whole category of target rather than an edge case.</p>
     *
     * @param type the target type
     * @return the names that can be written
     */
    private static Set<String> writableNames(Class<?> type) {
        ObjectDescriptor<?> descriptor = DescriptorResolver.describe(type);

        if (descriptor instanceof ValueObjectDescriptor<?> record) {
            return new LinkedHashSet<>(record.getComponents().keySet());
        }

        Set<String> names = new LinkedHashSet<>();

        for (PropertyDescriptor<?> property : descriptor.getProperties().values()) {
            if (property.isWritable()) {
                names.add(property.getName());
            }
        }

        return names;
    }

    /**
     * What a source can be asked for.
     *
     * @param type the source type
     * @return the names that can be read
     */
    private static Set<String> readableNames(Class<?> type) {
        ObjectDescriptor<?> descriptor = DescriptorResolver.describe(type);
        Set<String>         names      = new LinkedHashSet<>();

        if (descriptor instanceof ValueObjectDescriptor<?> record) {
            names.addAll(record.getComponents().keySet());
        }

        for (PropertyDescriptor<?> property : descriptor.getProperties().values()) {
            if (property.isReadable()) {
                names.add(property.getName());
            }
        }

        return names;
    }

    /**
     * Renders a set for a message, so a refusal shows what would have worked.
     *
     * @param names the names to list
     * @return them, sorted and comma-separated
     */
    private static String list(Set<String> names) {
        return names.isEmpty() ? "nothing" : String.join(", ", new TreeSet<>(names));
    }

    /**
     * Raises a failure at a node's position.
     *
     * @param span    where the construction was written, or {@code null}
     * @param message what is wrong
     * @return never returns
     */
    private static JmmSyntaxException failure(SpanNode span, String message) {
        return JmmSyntaxException.at(span, message);
    }
}
