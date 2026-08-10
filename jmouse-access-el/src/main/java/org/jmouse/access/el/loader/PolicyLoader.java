package org.jmouse.access.el.loader;

import org.jmouse.access.el.ExpressionEvaluator;
import org.jmouse.access.el.PolicyParseException;
import org.jmouse.access.policy.PolicyDocuments;
import org.jmouse.access.policy.PolicyException;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyInclude;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Several {@code .jmp} files read as one policy.
 *
 * <p>Between the parser and the binder there is a job neither of them can do, and it is this one:
 * find the files, read them, follow what they include, notice when that goes round in a circle, and
 * put the result together. A parser cannot, because "relative to what" is a question about a load and
 * a cycle only exists across several files; a binder cannot, because by the time it runs the answer
 * has to be one document.
 *
 * <pre>{@code
 * PolicyDocument document = new PolicyLoader(sources).load("innoventa", List.of("classpath:policy/*.jmp"));
 * AccessPolicy   policy   = new PolicyBinder(scopes, permissions).bind(document);
 * GrantStore     store    = new PolicyGrantStore(policy);
 * }</pre>
 *
 * <p>⚠️ <strong>Everything here fails loudly.</strong> A location matching no file, an include
 * pointing at nothing, a file that will not parse, two files disagreeing about the vocabulary: all of
 * them throw. A policy that half-loaded is an installation whose permissions are neither what the
 * files say nor what they said yesterday, and there is no message that makes that acceptable.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PolicyLoader {

    private final PolicySources       sources;
    private final ExpressionEvaluator parser;

    public PolicyLoader(PolicySources sources) {
        this(sources, new ExpressionEvaluator());
    }

    public PolicyLoader(PolicySources sources, ExpressionEvaluator parser) {
        this.sources = sources;
        this.parser  = parser;
    }

    /**
     * Reads every file these locations name, and everything those files include.
     *
     * @param documentName what the merged policy is called — every grant reports it as provenance
     * @param locations    where to look, in the order the result should read
     * @return one document holding all of it, and no {@code include}: they have been answered
     * @throws PolicyException where a location names nothing, an include points at nothing, a file
     *                         will not parse, or two files state a vocabulary that disagrees
     */
    public PolicyDocument load(String documentName, List<String> locations) {
        List<PolicyDocument> documents = new ArrayList<>();
        Set<String>          loaded    = new LinkedHashSet<>();
        Deque<PolicySource>  reading   = new ArrayDeque<>();

        for (String location : locations) {
            List<PolicySource> found = sources.at(location);

            if (found.isEmpty()) {
                throw new PolicyException(
                        "The policy location '" + location + "' names no file. Authorization is "
                        + "configured to be read from there, so an empty answer is a mistake in the "
                        + "configuration rather than a policy that grants nothing.");
            }

            for (PolicySource source : found) {
                read(source, documents, loaded, reading);
            }
        }

        return PolicyDocuments.merge(documentName, documents);
    }

    /**
     * Reads one file and everything below it, depth first.
     *
     * <p>A file lands in the result <em>after</em> what it includes, so what a policy composes with is
     * read before the policy that composes with it — which is the order somebody opening the rendered
     * result would want to read them in.
     *
     * @param source    the file to read
     * @param documents where parsed documents accumulate, in merge order
     * @param loaded    every location already read, so a file two others include is read once
     * @param reading   the chain currently open, which is what makes a cycle nameable
     */
    private void read(
            PolicySource         source,
            List<PolicyDocument> documents,
            Set<String>          loaded,
            Deque<PolicySource>  reading) {

        refuseCycle(source, reading);

        if (!loaded.add(source.location())) {
            // A file two others include is one file. Reading it twice would declare its roles twice,
            // which the binder refuses — correctly, and about the wrong thing.
            return;
        }

        PolicyDocument document = parse(source);

        reading.push(source);

        for (PolicyInclude include : document.includes()) {
            read(resolve(include, source), documents, loaded, reading);
        }

        reading.pop();
        documents.add(document);
    }

    private PolicyDocument parse(PolicySource source) {
        try {
            return parser.parse(source.text(), source.name());
        } catch (PolicyParseException malformed) {
            throw new PolicyException(
                    "'" + source.location() + "' is not a policy this can read. " + malformed.getMessage(),
                    malformed);
        }
    }

    private PolicySource resolve(PolicyInclude include, PolicySource from) {
        return sources.included(include.path(), from).orElseThrow(() -> new PolicyException(
                "'" + from.location() + "' " + include.at() + ": there is nothing at '" + include.path()
                + "'. An include is resolved relative to the file that wrote it."));
    }

    /**
     * Refuses a file that is already open further up the chain.
     *
     * <p>⚠️ The failure names the whole loop rather than the file it noticed. "roles.jmp includes
     * itself" is unhelpful when nothing in roles.jmp mentions roles.jmp, and three files including
     * each other is the case where this actually happens.
     */
    private void refuseCycle(PolicySource source, Deque<PolicySource> reading) {
        boolean isOpen = reading.stream()
                .anyMatch(open -> open.location().equals(source.location()));

        if (!isOpen) {
            return;
        }

        List<String> loop = new ArrayList<>(reading.stream()
                .map(PolicySource::location)
                .toList()
                .reversed());

        loop.add(source.location());

        throw new PolicyException(
                "These policy files include each other, so there is no order to read them in:\n  - "
                + String.join("\n  - ", loop));
    }
}
