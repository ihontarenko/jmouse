package org.jmouse.mapper.el.loader;

import org.jmouse.mapper.el.JmmReader;
import org.jmouse.mapper.el.JmmRuleSource;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.parser.JmmSyntaxException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every {@code .jmm} document a set of locations names, read into one rule source. 📚
 *
 * <h2>⚠️ Reading is EAGER, and that is the language's own choice rather than mine</h2>
 *
 * <p>The alternative is reading a document the first time its pair is mapped, which fails a request
 * instead of a startup. But {@code JmmValidator} already runs at load — every left-hand side checked
 * against the target's writable properties, every bare root against the source, every {@code use}
 * resolved — because the reference document's §10 says a mistyped rule that loads silently is the
 * failure the whole check exists against. A language that has decided to fail early has decided when:
 * a malformed file that only fails the request that happens to touch it is a file that reaches
 * production.</p>
 *
 * <h2>⚠️ A target belongs to one file, and a second claim is an error</h2>
 *
 * <p>§13 of the reference document: two files contributing rules to one target would technically merge,
 * and the cost is that <em>where is the rule for this property</em> stops having a single answer. So a
 * second file naming a target another file already claims refuses the load, naming <strong>both</strong>
 * files — one name would leave whoever reads it hunting for the other.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmLoader {

    private final JmmSources sources;
    private final JmmReader  reader;

    public JmmLoader(JmmSources sources, JmmReader reader) {
        this.sources = sources;
        this.reader = reader;
    }

    /**
     * Reads everything the locations name into one rule source.
     *
     * @param locations where documents live, in the order they should be read
     * @return the rules, ready to hand to a mapper
     * @throws JmmSyntaxException when a document will not load, or two claim one target
     */
    public JmmRuleSource load(List<String> locations) {
        JmmRuleSource rules = new JmmRuleSource();

        // ⚠️ Which file claimed which target, kept so the refusal can name both. A boolean "seen"
        // would refuse just as correctly and leave the message half as useful.
        Map<String, String> claimed = new LinkedHashMap<>();

        for (String location : locations) {
            for (JmmSource source : sources.at(location)) {
                read(source, rules, claimed);
            }
        }

        return rules;
    }

    /**
     * Reads one document, after checking it claims no target another already has.
     *
     * @param source  the document
     * @param rules   where its rules go
     * @param claimed which file claimed which target so far
     */
    private void read(JmmSource source, JmmRuleSource rules, Map<String, String> claimed) {
        // ⚠️ Parsed first and bound second, because the claim has to be checked BEFORE any of this
        // file's rules reach the source — a refusal that leaves half a document registered is worse
        // than none, and nothing here can take rules back out.
        MappingDocumentNode document = reader.parse(source.text(), source.location());

        for (TargetNode target : document.getTargets()) {
            String type  = target.getTargetType();
            String owner = claimed.putIfAbsent(type, source.location());

            if (owner != null) {
                throw JmmSyntaxException.at(target.getSpan(), ("'%s' claims target '%s', which '%s' "
                        + "already describes. A target type is described in one file: two files "
                        + "contributing to one target means 'where is the rule for this property' has no "
                        + "single answer").formatted(source.location(), type, owner))
                        .at(source.location());
            }
        }

        // ⚠️ The document above, not the text again. Re-reading the text here parsed every file twice —
        // once to look at its targets and once to bind them — and produced two trees for one file.
        reader.read(document, source.location(), rules);
    }
}
