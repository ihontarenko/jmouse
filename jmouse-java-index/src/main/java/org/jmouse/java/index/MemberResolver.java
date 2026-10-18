package org.jmouse.java.index;

import org.jmouse.java.parser.model.BodyReference;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.Expression;
import org.jmouse.java.parser.model.ImportDeclaration;
import org.jmouse.java.parser.model.LocalBinding;
import org.jmouse.java.parser.model.Modifier;
import org.jmouse.java.parser.model.Parameter;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeReferenceRole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Resolves body references over the same SymbolTable as type references. No filesystem or class
 * loading.
 */
public final class MemberResolver {
    private final SymbolTable symbols;
    private static final int MAXIMUM_INFERENCE_DEPTH = 64;
    private static final int MAXIMUM_HIERARCHY_TYPES = 1024;
    private final JavaTypeRules typeRules;
    private final TypeResolver types;

    public MemberResolver(SymbolTable symbols) {
        this.symbols = symbols;
        this.types = new TypeResolver(symbols);
        this.typeRules = new JavaTypeRules(symbols);
    }

    public MemberResolution resolve(FileIndex file, BodyReference reference) {
        if (file == null) {
            return unresolved(reference.name(), "Source is not indexed.");
        }
        if (!file.isBodyComplete(reference.owner())) {
            return unresolved(
                    reference.name(),
                    "The executable body has recovery diagnostics or is not indexed.");
        }
        return resolve(file, reference, reference.expression(), 0);
    }

    private MemberResolution resolve(
            FileIndex file, BodyReference context, Expression expression, int depth) {
        if (depth > MAXIMUM_INFERENCE_DEPTH) {
            return unresolved(expression.name(), "Receiver inference is too deep or cyclic.");
        }
        if (expression.kind() == Expression.Kind.NAME) {
            Optional<LocalBinding> local = binding(file, context, expression);
            if (local.isPresent()) {
                return new MemberResolution.Local(local.get());
            }
        }
        boolean constructorReference =
                expression.kind() == Expression.Kind.METHOD_REFERENCE
                        && expression.name().equals("new");
        boolean creation = expression.kind() == Expression.Kind.NEW || constructorReference;
        boolean callable =
                creation
                        || expression.kind() == Expression.Kind.CALL
                        || expression.kind() == Expression.Kind.METHOD_REFERENCE;
        Receiver receiver;
        if (constructorReference) {
            String written = qualified(expression.receiver());
            receiver =
                    new Receiver(written == null ? null : typeName(file, context, written), false);
        } else if (creation) {
            receiver = new Receiver(typeName(file, context, expression.name()), false);
        } else if (expression.receiver() != null) {
            receiver = receiver(file, context, expression.receiver(), depth + 1);
        } else {
            receiver = new Receiver(context.scopeType().value(), staticContext(file, context));
        }
        if (receiver == null || receiver.type() == null) {
            return unresolved(expression.name(), "Receiver type is unknown.");
        }
        if (expression.kind() == Expression.Kind.METHOD_REFERENCE) {
            receiver = new Receiver(receiver.type(), false);
        }
        List<MemberSymbol> candidates =
                candidates(file, context, receiver, expression.name(), callable, creation);
        if (candidates.isEmpty() && expression.receiver() == null && !creation) {
            // Enclosing classes and explicitly imported static members are real scopes, not a
            // global name search.
            SymbolId enclosing =
                    file.declaration(context.scopeType())
                            .map(IndexedDeclaration::container)
                            .orElse(null);
            while (enclosing != null && candidates.isEmpty()) {
                candidates =
                        candidates(
                                file,
                                context,
                                new Receiver(enclosing.value(), false),
                                expression.name(),
                                callable,
                                false);
                enclosing =
                        file.declaration(enclosing).map(IndexedDeclaration::container).orElse(null);
            }
            if (candidates.isEmpty()) {
                candidates = imported(file, context, expression.name(), callable);
            }
        }
        if (candidates.isEmpty()) {
            return unresolved(
                    expression.name(), "No accessible member of the receiver is indexed.");
        }
        if (!callable || expression.kind() == Expression.Kind.METHOD_REFERENCE) {
            // A method reference has no argument list; without a target functional type it cannot
            // select an overload.
            return choice(candidates);
        }
        return overload(file, context, expression, candidates, depth + 1);
    }

    /**
     * Visible completion candidates, using the same receiver and access rules as member resolution.
     */
    public List<MemberSymbol> complete(FileIndex file, BodyReference context) {
        Expression expression = context.expression();
        Receiver receiver =
                expression.receiver() == null
                        ? new Receiver(context.scopeType().value(), staticContext(file, context))
                        : receiver(file, context, expression.receiver(), 0);
        if (receiver == null || receiver.type() == null) {
            return List.of();
        }
        return receiverMembers(file, context, receiver, member -> !constructor(member), true);
    }

    private List<MemberSymbol> candidates(
            FileIndex file,
            BodyReference context,
            Receiver receiver,
            String name,
            boolean callable,
            boolean creation) {
        return receiverMembers(
                file,
                context,
                receiver,
                member ->
                        member.callable() == callable
                                && constructor(member) == creation
                                && (creation || member.name().equals(name)),
                !creation);
    }

    private List<MemberSymbol> receiverMembers(
            FileIndex file,
            BodyReference context,
            Receiver receiver,
            Predicate<MemberSymbol> accepts,
            boolean includeInherited) {
        Map<String, MemberSymbol> found = new LinkedHashMap<>();
        List<TypeSymbol> roots = symbols.findTypes(erase(receiver.type()));
        List<TypeSymbol> pending = new ArrayList<>(roots);
        Set<TypeSymbol> visited = new HashSet<>();
        Set<String> signatures = new HashSet<>();
        for (int cursor = 0;
                cursor < pending.size() && cursor < MAXIMUM_HIERARCHY_TYPES;
                cursor++) {
            TypeSymbol type = pending.get(cursor);
            if (!visited.add(type)) {
                continue;
            }
            boolean inherited = !roots.contains(type);
            for (MemberSymbol member : symbols.findMembers(type)) {
                if (!accepts.test(member)
                        || (receiver.staticOnly() && !member.modifiers().contains(Modifier.STATIC))
                        || (inherited && member.modifiers().contains(Modifier.PRIVATE))
                        || !accessible(file, context, member, type)) {
                    continue;
                }
                String signature = signature(member);
                if (inherited && signatures.contains(signature)) {
                    continue;
                }
                String key =
                        inherited
                                ? signature
                                : signature
                                        + "@"
                                        + member.source()
                                        + ":"
                                        + member.symbolId().value();
                found.putIfAbsent(key, member);
                signatures.add(signature);
            }
            if (includeInherited) {
                pending.addAll(symbols.supertypes(type));
            }
        }
        return List.copyOf(found.values());
    }

    private static String signature(MemberSymbol member) {
        return member.name()
                + member.parameters().stream().map(parameter -> erase(parameter.type())).toList();
    }

    private static boolean constructor(MemberSymbol member) {
        return member.kind() == DeclarationKind.CONSTRUCTOR
                || member.kind() == DeclarationKind.COMPACT_CONSTRUCTOR;
    }

    private boolean accessible(
            FileIndex file, BodyReference context, MemberSymbol member, TypeSymbol declaringType) {
        FileIndex declared = symbols.declaringFile(declaringType).orElse(file);
        boolean samePackage = file.packageName().equals(declared.packageName());
        if (member.modifiers().contains(Modifier.PUBLIC)
                || sameNest(context.scopeType(), member.container())) {
            return true;
        }
        if (member.modifiers().contains(Modifier.PRIVATE)) {
            return false;
        }
        if (samePackage) {
            return true;
        }
        if (member.modifiers().contains(Modifier.PROTECTED)) {
            return typeRules.subtype(
                    context.scopeType().value(), member.container().value(), new HashSet<>());
        }
        // Interface members are implicitly public even without a modifier token.
        return declaringType.kind() == DeclarationKind.INTERFACE
                || declaringType.kind() == DeclarationKind.ANNOTATION_TYPE;
    }

    private boolean sameNest(SymbolId first, SymbolId second) {
        // Use actual declarations, not the first dot (which usually separates package segments).
        return first.equals(second)
                || first.value().startsWith(second.value() + ".")
                || second.value().startsWith(first.value() + ".");
    }

    private List<MemberSymbol> imported(
            FileIndex file, BodyReference context, String name, boolean callable) {
        List<MemberSymbol> single = new ArrayList<>();
        List<MemberSymbol> wildcard = new ArrayList<>();
        for (ImportDeclaration declaration : file.imports()) {
            if (!declaration.isStatic()) {
                continue;
            }
            String owner = declaration.name();
            if (!declaration.onDemand()) {
                int dot = owner.lastIndexOf('.');
                if (dot < 0 || !owner.substring(dot + 1).equals(name)) {
                    continue;
                }
                owner = owner.substring(0, dot);
            }
            List<MemberSymbol> matches =
                    candidates(file, context, new Receiver(owner, true), name, callable, false);
            (declaration.onDemand() ? wildcard : single).addAll(matches);
        }
        return (single.isEmpty() ? wildcard : single).stream().distinct().toList();
    }

    private MemberResolution overload(
            FileIndex file,
            BodyReference context,
            Expression call,
            List<MemberSymbol> candidates,
            int depth) {
        List<String> arguments =
                call.arguments().stream()
                        .map(argument -> infer(file, context, argument, depth + 1))
                        .toList();
        record Applicable(
                MemberSymbol member, List<String> parameters, boolean uncertain, int phase) {}
        List<Applicable> applicable = new ArrayList<>();
        for (MemberSymbol member : candidates) {
            List<Parameter> parameters = member.parameters();
            boolean varargs = !parameters.isEmpty() && parameters.getLast().variableArity();
            if ((!varargs && parameters.size() != arguments.size())
                    || (varargs && arguments.size() < parameters.size() - 1)) {
                continue;
            }
            TypeSymbol owner =
                    symbols.findTypes(member.container().value()).stream()
                            .filter(type -> type.source().equals(member.source()))
                            .findFirst()
                            .orElse(null);
            FileIndex declaringFile =
                    owner == null ? file : symbols.declaringFile(owner).orElse(file);
            BodyReference memberContext =
                    new BodyReference(
                            call, BodyReference.Role.CALLS, member.symbolId(), member.container());
            // First try fixed arity (including passing an array to a varargs method), then expanded
            // varargs.
            for (int expansion = 0; expansion <= (varargs ? 1 : 0); expansion++) {
                if (expansion == 0 && parameters.size() != arguments.size()) {
                    continue;
                }
                boolean uncertain = false;
                boolean rejected = false;
                int phase = expansion == 1 ? 2 : 0;
                List<String> written = new ArrayList<>();
                for (int index = 0; index < arguments.size(); index++) {
                    Parameter parameter = parameters.get(Math.min(index, parameters.size() - 1));
                    String parameterType = typeName(declaringFile, memberContext, parameter.type());
                    if (parameter.variableArity() && expansion == 0) {
                        parameterType = parameterType == null ? null : parameterType + "[]";
                    }
                    written.add(parameterType);
                    int conversion = typeRules.conversion(arguments.get(index), parameterType);
                    if (conversion < 0) {
                        rejected = true;
                        break;
                    }
                    uncertain |= conversion == 3;
                    if (conversion == 2 && expansion == 0) {
                        phase = 1;
                    }
                }
                if (!rejected) {
                    applicable.add(new Applicable(member, written, uncertain, phase));
                    break;
                }
            }
        }
        if (applicable.isEmpty()) {
            return unresolved(call.name(), "No applicable overload for the known argument types.");
        }
        int phase = applicable.stream().mapToInt(Applicable::phase).min().orElse(0);
        List<Applicable> phaseCandidates =
                applicable.stream().filter(candidate -> candidate.phase() == phase).toList();
        List<Applicable> best =
                phaseCandidates.stream()
                        .filter(
                                candidate ->
                                        phaseCandidates.stream()
                                                .noneMatch(
                                                        other ->
                                                                other != candidate
                                                                        && moreSpecific(
                                                                                other.parameters(),
                                                                                candidate
                                                                                        .parameters())))
                        .toList();
        if (best.size() == 1 && !best.getFirst().uncertain()) {
            return new MemberResolution.Resolved(best.getFirst().member());
        }
        List<MemberSymbol> possible = best.stream().map(Applicable::member).distinct().toList();
        return possible.size() > 1
                ? new MemberResolution.Ambiguous(possible)
                : unresolved(call.name(), "Argument type inference is incomplete.");
    }

    private boolean moreSpecific(List<String> first, List<String> second) {
        boolean strictly = false;
        for (int index = 0; index < first.size(); index++) {
            String left = first.get(index);
            String right = second.get(index);
            if (left == null
                    || right == null
                    || typeRules.conversion(left, right) < 0
                    || typeRules.conversion(left, right) >= 2) {
                return false;
            }
            strictly |= !left.equals(right);
        }
        return strictly;
    }

    /** 0 identity, 1 widening, 2 boxing/unboxing, 3 unknown, -1 incompatible. */
    private Receiver receiver(
            FileIndex file, BodyReference context, Expression expression, int depth) {
        String inferred = infer(file, context, expression, depth + 1);
        if (inferred != null) {
            return new Receiver(inferred, false);
        }
        String written = qualified(expression);
        if (written != null
                && !(expression.kind() == Expression.Kind.NAME
                        && binding(file, context, expression).isPresent())) {
            String type = typeName(file, context, written);
            if (type != null && !symbols.findTypes(type).isEmpty()) {
                return new Receiver(type, true);
            }
        }
        return null;
    }

    private String infer(FileIndex file, BodyReference context, Expression expression, int depth) {
        if (expression == null || depth > MAXIMUM_INFERENCE_DEPTH) {
            return null;
        }
        switch (expression.kind()) {
            case LITERAL:
                return expression.name();
            case NEW:
                return typeName(file, context, expression.name());
            case NAME:
                if (expression.name().equals("this")) {
                    return context.scopeType().value();
                }
                if (expression.name().equals("super")) {
                    return symbols.findTypes(context.scopeType().value()).stream()
                            .flatMap(type -> symbols.supertypes(type).stream())
                            .map(TypeSymbol::qualifiedName)
                            .findFirst()
                            .orElse(null);
                }
                Optional<LocalBinding> local = binding(file, context, expression);
                if (local.isPresent()) {
                    LocalBinding binding = local.get();
                    return binding.type() != null
                            ? typeName(file, context, binding.type())
                            : infer(file, context, binding.initializer(), depth + 1);
                }
                break;
            case ARRAY_ACCESS:
                String array = infer(file, context, expression.receiver(), depth + 1);
                return array != null && array.endsWith("[]")
                        ? array.substring(0, array.length() - 2)
                        : null;
            case OPERATION:
                if (expression.name().startsWith("cast:")) {
                    return typeName(file, context, expression.name().substring(5));
                }
                if (expression.name().equals("?:") && expression.arguments().size() == 3) {
                    String whenTrue =
                            infer(file, context, expression.arguments().get(1), depth + 1);
                    String whenFalse =
                            infer(file, context, expression.arguments().get(2), depth + 1);
                    return whenTrue != null && whenTrue.equals(whenFalse) ? whenTrue : null;
                }
                if (Set.of("==", "!=", "<", ">", "<=", ">=", "instanceof", "&&", "||", "!")
                        .contains(expression.name())) {
                    return "boolean";
                }
                List<String> arguments =
                        expression.arguments().stream()
                                .map(argument -> infer(file, context, argument, depth + 1))
                                .toList();
                if (expression.name().equals("=") || expression.name().endsWith("=")) {
                    return arguments.isEmpty() ? null : arguments.getFirst();
                }
                if (expression.name().equals("+") && arguments.contains("java.lang.String")) {
                    return "java.lang.String";
                }
                if (arguments.contains(null) || arguments.isEmpty()) {
                    return null;
                }
                for (String numeric : List.of("double", "float", "long")) {
                    if (arguments.contains(numeric)) {
                        return numeric;
                    }
                }
                return arguments.stream().allMatch(JavaTypeRules.PRIMITIVES::contains)
                        ? "int"
                        : null;
            case UNKNOWN, LAMBDA, METHOD_REFERENCE:
                return null;
            default:
                break;
        }
        MemberResolution resolved = resolve(file, context, expression, depth + 1);
        if (resolved instanceof MemberResolution.Resolved member) {
            MemberSymbol target = member.target();
            TypeSymbol owner =
                    symbols.findTypes(target.container().value()).stream()
                            .filter(type -> type.source().equals(target.source()))
                            .findFirst()
                            .orElse(null);
            FileIndex declared = owner == null ? file : symbols.declaringFile(owner).orElse(file);
            return typeName(
                    declared,
                    new BodyReference(
                            expression, context.role(), target.symbolId(), target.container()),
                    target.type());
        }
        return null;
    }

    private Optional<LocalBinding> binding(
            FileIndex file, BodyReference context, Expression expression) {
        LocalBinding found = null;
        int offset = expression.nameSpan().start().offset();
        for (ExecutableBody body : file.bodies()) {
            if (!body.owner().equals(context.owner())) {
                continue;
            }
            for (LocalBinding variable : body.variables()) {
                if (variable.name().equals(expression.name())
                        && variable.scope().start().offset() <= offset
                        && offset < variable.scope().end().offset()
                        && (found == null
                                || variable.scope().start().offset()
                                        >= found.scope().start().offset())) {
                    found = variable;
                }
            }
        }
        return Optional.ofNullable(found);
    }

    private boolean staticContext(FileIndex file, BodyReference context) {
        return file.declaration(context.owner())
                .map(declaration -> declaration.modifiers().contains(Modifier.STATIC))
                .orElse(false);
    }

    private String typeName(FileIndex file, BodyReference context, String written) {
        String erased = erase(written);
        int dimensions = 0;
        while (erased.endsWith("[]")) {
            dimensions++;
            erased = erased.substring(0, erased.length() - 2);
        }
        String suffix = "[]".repeat(dimensions);
        if (JavaTypeRules.PRIMITIVES.contains(erased) || erased.equals("null")) {
            return erased + suffix;
        }
        IndexedReference reference =
                new IndexedReference(
                        erased,
                        TypeReferenceRole.PARAMETER_TYPE,
                        context.span(),
                        context.owner(),
                        context.scopeType());
        Resolution resolution = types.resolve(file, reference);
        if (resolution instanceof Resolution.Resolved resolved) {
            return resolved.target().qualifiedName() + suffix;
        }
        if (resolution instanceof Resolution.Unresolved unresolved) {
            if (unresolved.presumedQualifiedName() != null) {
                return unresolved.presumedQualifiedName() + suffix;
            }
            if (JavaTypeRules.JAVA_LANG.contains(erased)) {
                return "java.lang." + erased + suffix;
            }
        }
        return null;
    }

    private static String erase(String type) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        for (int index = 0; index < type.length(); index++) {
            char character = type.charAt(index);
            if (character == '<') {
                depth++;
            } else if (character == '>') {
                depth--;
            } else if (depth == 0 && !Character.isWhitespace(character)) {
                result.append(character);
            }
        }
        return result.toString().replace("...", "[]");
    }

    private static String qualified(Expression expression) {
        if (expression.kind() == Expression.Kind.NAME) {
            return expression.name();
        }
        if (expression.kind() == Expression.Kind.FIELD) {
            String receiver = qualified(expression.receiver());
            return receiver == null ? null : receiver + "." + expression.name();
        }
        return null;
    }

    private static MemberResolution choice(List<MemberSymbol> candidates) {
        return candidates.size() == 1
                ? new MemberResolution.Resolved(candidates.getFirst())
                : new MemberResolution.Ambiguous(candidates);
    }

    private static MemberResolution.Unresolved unresolved(String name, String reason) {
        return new MemberResolution.Unresolved(name, reason);
    }

    private record Receiver(String type, boolean staticOnly) {}
}
