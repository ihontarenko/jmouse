package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.QueryParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A whole {@code .jmq} file — the views and functions it declares, in the order it declares them.
 *
 * <p>⚠️ <strong>This exists because an expression parser reads one expression.</strong> Asked for an
 * expression, the language returns the first declaration and stops: a second {@code view} below the
 * first is not refused, not reported, simply absent. In a file whose whole job is to say what to fetch,
 * a declaration that quietly is not there is the worst failure available — the query runs, returns
 * rows, and nobody has a reason to look.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryDocumentNode extends AbstractExpression {

    private final List<SourceNode>    sources    = new ArrayList<>();
    private final List<StructureNode> structures = new ArrayList<>();
    private final List<MappingNode>   mappings   = new ArrayList<>();
    private final List<ViewNode>      views      = new ArrayList<>();
    private final List<FunctionNode>  functions  = new ArrayList<>();
    private final List<Object>        written    = new ArrayList<>();

    /**
     * The merged sources, computed once.
     *
     * <h2>⚠️ Cleared on every declaration rather than computed lazily and left</h2>
     *
     * <p>A document is built by a parser calling {@code add…} several times, so a value cached during that
     * would be a document missing whatever was declared after it — and missing it silently, because a
     * source that is simply absent reads as a document that never declared one.</p>
     */
    private List<SourceNode> resolved;

    /**
     * A declaration of where a product's data is.
     *
     * <p>⚠️ Kept in the same document type as views deliberately: a product may ship its sources in one
     * file and its views in another, or put both in one, and nothing downstream has to know which it
     * chose.</p>
     */
    public void addSource(SourceNode source) {
        sources.add(source);
        written.add(source);
        resolved = null;
    }

    public void addStructure(StructureNode structure) {
        structures.add(structure);
        written.add(structure);
        resolved = null;
    }

    public void addMapping(MappingNode mapping) {
        mappings.add(mapping);
        written.add(mapping);
        resolved = null;
    }

    public List<StructureNode> getStructures() {
        return List.copyOf(structures);
    }

    public List<MappingNode> getMappings() {
        return List.copyOf(mappings);
    }

    /**
     * Every shape-bound-to-a-place this document declares — what the loader, the checker and every
     * translator read.
     *
     * <h2>⚠️ The two spellings meet HERE and nowhere else</h2>
     *
     * <p>A {@code structure} and its {@code mapping}s merge into the same object an older {@code source}
     * block produces directly. So the split cost nothing below the parser: not one line of the compiler
     * knows there are two declarations, and a document written either way reaches it identically.</p>
     *
     * <p>⚠️ A mapping naming a structure that is not declared here is refused by name. Guessing — treating
     * it as an empty shape, or as the structure of the same name from another file — would produce a query
     * that compiles and reads nothing.</p>
     */
    public List<SourceNode> getSources() {
        if (resolved == null) {
            List<SourceNode> declared = new ArrayList<>(sources);

            for (MappingNode mapping : mappings) {
                declared.add(SourceNode.merge(structureFor(mapping), mapping));
            }

            resolved = List.copyOf(declared);
        }

        return resolved;
    }

    private StructureNode structureFor(MappingNode mapping) {
        return structures.stream()
                .filter(structure -> structure.getName().equals(mapping.getStructure()))
                .findFirst()
                .orElseThrow(() -> new QueryParseException(
                        ("mapping '%s' binds a structure called '%s', and this document declares no such "
                         + "structure; it declares %s").formatted(
                                mapping.getQualifiedName(), mapping.getStructure(),
                                structures.isEmpty()
                                        ? "none at all"
                                        : structures.stream().map(StructureNode::getName)
                                                .collect(Collectors.joining(", ")))));
    }

    public void addView(ViewNode view) {
        views.add(view);
        written.add(view);
    }

    public void addFunction(FunctionNode function) {
        functions.add(function);
        written.add(function);
    }

    public List<ViewNode> getViews() {
        return List.copyOf(views);
    }

    public List<FunctionNode> getFunctions() {
        return List.copyOf(functions);
    }

    /**
     * The single view a document holds, where it holds exactly one.
     *
     * <p>A saved view is stored as a document of one, and a caller that has to index into a list to
     * find it is a caller that will index into an empty one.</p>
     *
     * @return the only view, or empty when the document holds none or several
     */
    public Optional<ViewNode> getSingleView() {
        return views.size() == 1 ? Optional.of(views.getFirst()) : Optional.empty();
    }

    /**
     * A declared function, by name.
     *
     * @param name the function's name
     * @return the function, or empty when the document declares none by that name
     */
    public Optional<FunctionNode> getFunction(String name) {
        return functions.stream().filter(function -> function.getName().equals(name)).findFirst();
    }

    /**
     * Writes the whole document back out.
     *
     * <p>⚠️ Declarations are written in the order they were <em>read</em>, not with functions hoisted
     * above views. A person who put a helper next to the view that uses it finds it there again, and a
     * writer that reorganises somebody's file is a writer whose output nobody trusts.</p>
     */
    @Override
    public String toSource() {
        return written.stream()
                .map(declaration -> switch (declaration) {
                    case ViewNode view -> view.toSource();
                    case FunctionNode function -> function.toSource();
                    case SourceNode source -> source.toSource();
                    case StructureNode structure -> structure.toSource();
                    case MappingNode mapping -> mapping.toSource();
                    default -> declaration.toString();
                })
                .collect(Collectors.joining("\n\n"));
    }

    @Override
    public String toString() {
        return "document[%d view(s), %d function(s)]".formatted(views.size(), functions.size());
    }
}
