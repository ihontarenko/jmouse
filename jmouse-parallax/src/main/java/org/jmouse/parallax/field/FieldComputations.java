package org.jmouse.parallax.field;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.PlaneWindow;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.Sampling;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.numerics.OdeIntegrator;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** A vector field seen as arrows, as the paths it carries things along, and through its derived scalar fields. */
public final class FieldComputations {

    private FieldComputations() {
    }

    /** Points of a regular lattice over the field's box, `perAxis` per axis. */
    static List<double[]> lattice(FieldView field, int perAxis) {
        List<double[]> points = new ArrayList<>();
        int dimension = field.dimension();
        int total = (int) Math.pow(perAxis, dimension);
        for (int index = 0; index < total; index++) {
            double[] point = new double[dimension];
            int remainder = index;
            for (int axis = 0; axis < dimension; axis++) {
                Interval bound = field.bounds().get(axis);
                point[axis] = bound.sample(remainder % perAxis, perAxis);
                remainder /= perAxis;
            }
            points.add(point);
        }
        return points;
    }

    public static class Arrows implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.VECTOR_ARROWS;
        }

        public boolean accepts(ObjectDefinition definition) {
            return FieldView.admits(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(Sampling.FIELD_RESOLUTION);
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return (long) Math.pow(settings.integer("resolution"), object.definition().ambientDimension());
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            FieldView field = FieldView.of(object);
            int dimension = field.dimension();
            List<double[]> points = lattice(field, settings.integer("resolution"));
            float[] origins = new float[points.size() * dimension];
            float[] vectors = new float[points.size() * dimension];
            float[] magnitudes = new float[points.size()];
            double[] value = new double[dimension];
            double largest = 0;
            for (int index = 0; index < points.size(); index++) {
                field.evaluate(points.get(index), value);
                double magnitude = 0;
                for (int axis = 0; axis < dimension; axis++) {
                    origins[index * dimension + axis] = (float) points.get(index)[axis];
                    vectors[index * dimension + axis] = (float) value[axis];
                    magnitude += value[axis] * value[axis];
                }
                magnitude = Math.sqrt(magnitude);
                magnitudes[index] = (float) magnitude;
                if (Double.isFinite(magnitude)) {
                    largest = Math.max(largest, magnitude);
                }
            }
            return new Dataset.VectorSamples(dimension, origins, vectors, magnitudes, largest);
        }
    }

    /** Integral curves of the field from a lattice of seeds, traced both forwards and backwards. */
    public static class Streamlines implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.STREAMLINES;
        }

        public boolean accepts(ObjectDefinition definition) {
            return FieldView.admits(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("seeds", 8, 2, 32, "Seeds per axis"),
                    SettingSpecification.number("length", 4, 0.01, 1000, "How long each line is followed"),
                    SettingSpecification.integer("samples", 120, 8, 2000, "Points per line"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return (long) Math.pow(settings.integer("seeds"), object.definition().ambientDimension()) * settings.integer("samples") * 12;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            FieldView field = FieldView.of(object);
            List<double[]> seeds = lattice(field, settings.integer("seeds"));
            double length = settings.number("length");
            int samples = settings.integer("samples");
            List<Dataset.Polyline> lines = new ArrayList<>();
            double escape = escapeRadius(field);
            for (int index = 0; index < seeds.size(); index++) {
                progress.checkpoint((double) index / seeds.size(), "tracing");
                for (double direction : new double[]{1, -1}) {
                    OdeIntegrator.Solution solution = OdeIntegrator.integrate((time, state, derivative) -> {
                        field.evaluate(state, derivative);
                        for (int axis = 0; axis < derivative.length; axis++) {
                            derivative[axis] *= direction;
                        }
                    }, seeds.get(index), 0, length, samples, 1e-6, 20_000, escape, ComputationProgress.NONE);
                    lines.add(polyline(solution, field.dimension(), "seed-" + index));
                }
            }
            return new Dataset.Polylines(field.dimension(), lines, minimum(field), maximum(field));
        }
    }

    /**
     * Particles dropped into the flow at random and carried by it: positions at every frame, so the client
     * animates them. Seeded, so the same settings give the same swarm.
     */
    public static class Particles implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.PARTICLES;
        }

        public boolean accepts(ObjectDefinition definition) {
            return FieldView.admits(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("count", 400, 1, 20_000, "Particles"),
                    SettingSpecification.integer("frames", 120, 2, 2000, "Frames of the animation"),
                    SettingSpecification.number("duration", 4, 0.01, 1000, "Field time the animation covers"),
                    SettingSpecification.integer("seed", 7, 0, Integer.MAX_VALUE, "Random seed"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return (long) settings.integer("count") * settings.integer("frames") * 8;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            FieldView field = FieldView.of(object);
            int count = settings.integer("count");
            int frames = settings.integer("frames");
            double step = settings.number("duration") / (frames - 1);
            int dimension = field.dimension();
            SplittableRandom random = new SplittableRandom(settings.integer("seed"));
            double[][] particles = new double[count][dimension];
            for (double[] particle : particles) {
                for (int axis = 0; axis < dimension; axis++) {
                    Interval bound = field.bounds().get(axis);
                    particle[axis] = bound.minimum() + random.nextDouble() * bound.length();
                }
            }
            float[] positions = new float[frames * count * dimension];
            double[] k1 = new double[dimension];
            double[] k2 = new double[dimension];
            double[] k3 = new double[dimension];
            double[] k4 = new double[dimension];
            double[] temporary = new double[dimension];
            for (int frame = 0; frame < frames; frame++) {
                if (frame % 16 == 0) {
                    progress.checkpoint((double) frame / frames, "advecting");
                }
                for (int index = 0; index < count; index++) {
                    double[] particle = particles[index];
                    for (int axis = 0; axis < dimension; axis++) {
                        positions[(frame * count + index) * dimension + axis] = (float) particle[axis];
                    }
                    rungeKuttaStep(field, particle, step, k1, k2, k3, k4, temporary);
                    if (!field.inside(particle)) {
                        for (int axis = 0; axis < dimension; axis++) {
                            Interval bound = field.bounds().get(axis);
                            particle[axis] = bound.minimum() + random.nextDouble() * bound.length();
                        }
                    }
                }
            }
            float[] frameIndex = new float[frames * count];
            for (int frame = 0; frame < frames; frame++) {
                java.util.Arrays.fill(frameIndex, frame * count, (frame + 1) * count, frame);
            }
            return new Dataset.PointCloud(dimension, positions, frameIndex, "frame");
        }
    }

    /** What the scalar field reads from the vector field at each point. */
    public enum ScalarMeasure { MAGNITUDE, DIVERGENCE, CURL }

    /**
     * One scalar read off the field on a plane slice, as a heat map a renderer colours: its length |F|, its
     * divergence ∇·F (sources positive, sinks negative) or its scalar curl ∂Fy/∂x − ∂Fx/∂y (a vortex shows up as a
     * peak). The measure is a setting, so every field offers all three through one representation.
     */
    public static class ScalarField implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.SCALAR_FIELD;
        }

        public boolean accepts(ObjectDefinition definition) {
            return FieldView.admits(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.choice("measure", ScalarMeasure.MAGNITUDE, "What is read from the field: |F|, ∇·F or the scalar curl"),
                    SettingSpecification.integer("resolution", 128, 8, 1024, "Samples per axis"),
                    SettingSpecification.number("slice", 0, -1e6, 1e6, "z of the plane a 3D field is read on"), PlaneWindow.specification());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * 3;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ScalarMeasure measure = settings.choice("measure", ScalarMeasure.class);
            FieldView field = FieldView.of(object);
            int resolution = settings.integer("resolution");
            double slice = settings.number("slice");
            int dimension = field.dimension();
            List<BoundFunction> partials = new ArrayList<>();
            if (measure == ScalarMeasure.DIVERGENCE) {
                for (int axis = 0; axis < dimension; axis++) {
                    partials.add(field.partial(axis, axis));
                }
            } else if (measure == ScalarMeasure.CURL) {
                partials.add(field.partial(1, 0));
                partials.add(field.partial(0, 1));
            }
            Interval[] region = PlaneWindow.region(settings, field.bounds().get(0), field.bounds().get(1));
            Interval x = region[0];
            Interval y = region[1];
            double[] point = new double[dimension];
            double[] value = new double[dimension];
            float[] values = Sampling.grid((first, second) -> {
                point[0] = first;
                point[1] = second;
                if (dimension == 3) {
                    point[2] = slice;
                }
                return switch (measure) {
                    case MAGNITUDE -> {
                        field.evaluate(point, value);
                        double sum = 0;
                        for (double component : value) {
                            sum += component * component;
                        }
                        yield Math.sqrt(sum);
                    }
                    case DIVERGENCE -> {
                        double sum = 0;
                        for (BoundFunction partial : partials) {
                            sum += field.partialAt(partial, point);
                        }
                        yield sum;
                    }
                    case CURL -> field.partialAt(partials.get(0), point) - field.partialAt(partials.get(1), point);
                };
            }, x, y, resolution, resolution, progress);
            double[] range = Sampling.range(values);
            return new Dataset.ScalarGrid(resolution, resolution, x, y, values, range[0], range[1],
                    measure.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    static void rungeKuttaStep(FieldView field, double[] state, double step, double[] k1, double[] k2, double[] k3,
                               double[] k4, double[] temporary) {
        int dimension = state.length;
        field.evaluate(state, k1);
        for (int axis = 0; axis < dimension; axis++) {
            temporary[axis] = state[axis] + step / 2 * k1[axis];
        }
        field.evaluate(temporary, k2);
        for (int axis = 0; axis < dimension; axis++) {
            temporary[axis] = state[axis] + step / 2 * k2[axis];
        }
        field.evaluate(temporary, k3);
        for (int axis = 0; axis < dimension; axis++) {
            temporary[axis] = state[axis] + step * k3[axis];
        }
        field.evaluate(temporary, k4);
        for (int axis = 0; axis < dimension; axis++) {
            state[axis] += step / 6 * (k1[axis] + 2 * k2[axis] + 2 * k3[axis] + k4[axis]);
        }
    }

    static Dataset.Polyline polyline(OdeIntegrator.Solution solution, int dimension, String label) {
        double[][] states = solution.states();
        float[] points = new float[states.length * dimension];
        float[] times = new float[states.length];
        for (int index = 0; index < states.length; index++) {
            for (int axis = 0; axis < dimension; axis++) {
                points[index * dimension + axis] = (float) states[index][axis];
            }
            times[index] = (float) solution.times()[index];
        }
        return new Dataset.Polyline(points, times, label, false);
    }

    static double escapeRadius(FieldView field) {
        double radius = 0;
        for (Interval bound : field.bounds()) {
            radius = Math.max(radius, Math.max(Math.abs(bound.minimum()), Math.abs(bound.maximum())));
        }
        return radius * 4 + 1;
    }

    static double[] minimum(FieldView field) {
        return field.bounds().stream().mapToDouble(Interval::minimum).toArray();
    }

    static double[] maximum(FieldView field) {
        return field.bounds().stream().mapToDouble(Interval::maximum).toArray();
    }
}
