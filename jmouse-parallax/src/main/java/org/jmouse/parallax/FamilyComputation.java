package org.jmouse.parallax;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A family — one member for every whole value of an index, {@code y = sin((2k - 1) x) / (2k - 1)  k in 1 .. N integer}
 * — computed by the member's own computation, once per member, into one dataset: the curves side by side, the shapes
 * of a construction side by side. {@link Computations} hands it out for any family, so whoever computes an object
 * computes a family the same way.
 */
final class FamilyComputation implements Computation {

    /** More members than this is a mistake rather than a picture. */
    static final int MAXIMUM_MEMBERS = 200;

    private final Computation member;

    FamilyComputation(Computation member) {
        this.member = member;
    }

    @Override
    public RepresentationKind kind() {
        return member.kind();
    }

    @Override
    public boolean accepts(ObjectDefinition definition) {
        return definition instanceof ObjectDefinition.Family family && member.accepts(family.member());
    }

    @Override
    public List<SettingSpecification> settings() {
        return member.settings();
    }

    @Override
    public long estimatedCost(CompiledObject object, ComputationSettings settings) {
        ObjectDefinition.Family family = (ObjectDefinition.Family) object.definition();
        int[] range = range(object, family);
        CompiledObject first = memberAt(object, family, range[0]);
        return member.estimatedCost(first, settings) * Math.max(1, range[1] - range[0] + 1);
    }

    @Override
    public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
        ObjectDefinition.Family family = (ObjectDefinition.Family) object.definition();
        int[] range = range(object, family);
        List<Dataset> members = new ArrayList<>();
        int count = range[1] - range[0] + 1;
        for (int index = range[0]; index <= range[1]; index++) {
            int position = index - range[0];
            progress.checkpoint((double) position / count, "computing the family");
            members.add(member.compute(memberAt(object, family, index), settings, memberProgress(progress, position, count)));
        }
        return merged(members);
    }

    /** One member's share of the family's progress: its reports scaled into its slot, the family's cancellation its own. */
    private static ComputationProgress memberProgress(ComputationProgress family, int position, int count) {
        return new ComputationProgress() {
            @Override
            public void report(double fraction, String message) {
                family.report((position + fraction) / count, message);
            }

            @Override
            public boolean cancelled() {
                return family.cancelled();
            }
        };
    }

    /** The first and last member, whole numbers; refused when there are none or too many. */
    private static int[] range(CompiledObject object, ObjectDefinition.Family family) {
        int from = (int) Math.round(object.constant(family.from()));
        int to = (int) Math.round(object.constant(family.to()));
        if (to < from) {
            throw new IllegalArgumentException("The family " + family.index() + " in " + from + " .. " + to + " has no members.");
        }
        if (to - from + 1 > MAXIMUM_MEMBERS) {
            throw new IllegalArgumentException("A family has at most " + MAXIMUM_MEMBERS + " members, not " + (to - from + 1) + ".");
        }
        return new int[]{from, to};
    }

    /** The member at one value of the index: compiled with every value of the family, and the index besides. */
    private static CompiledObject memberAt(CompiledObject object, ObjectDefinition.Family family, int index) {
        List<ParameterDefinition> parameters = new ArrayList<>(object.parameters());
        parameters.removeIf(parameter -> parameter.name().equals(family.index()));
        parameters.add(new ParameterDefinition(family.index(), family.index(), index, -1e9, 1e9, 1, false));
        Map<String, Double> values = new HashMap<>(object.parameterValues());
        values.put(family.index(), (double) index);
        return DefinitionCompiler.compile(family.member(), parameters, values);
    }

    /** The members as one dataset: lines after lines, points after points, part by part. */
    private static Dataset merged(List<Dataset> members) {
        Dataset first = members.getFirst();
        return switch (first) {
            case Dataset.Polylines lines -> {
                List<Dataset.Polyline> all = new ArrayList<>();
                double[] minimum = lines.minimum().clone();
                double[] maximum = lines.maximum().clone();
                for (Dataset member : members) {
                    Dataset.Polylines each = (Dataset.Polylines) member;
                    all.addAll(each.lines());
                    for (int axis = 0; axis < minimum.length; axis++) {
                        minimum[axis] = Math.min(minimum[axis], each.minimum()[axis]);
                        maximum[axis] = Math.max(maximum[axis], each.maximum()[axis]);
                    }
                }
                yield new Dataset.Polylines(lines.dimension(), all, minimum, maximum);
            }
            case Dataset.FeaturePoints points -> new Dataset.FeaturePoints(members.stream()
                    .flatMap(member -> ((Dataset.FeaturePoints) member).features().stream()).toList());
            case Dataset.Composite composite -> {
                Map<String, Dataset> parts = new LinkedHashMap<>();
                for (String name : composite.parts().keySet()) {
                    parts.put(name, merged(members.stream().map(member -> ((Dataset.Composite) member).parts().get(name)).toList()));
                }
                yield new Dataset.Composite(parts);
            }
            default -> throw new IllegalArgumentException("A family draws curves and shapes; "
                    + first.getClass().getSimpleName() + " cannot be laid side by side.");
        };
    }
}
