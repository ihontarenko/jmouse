package org.jmouse.parallax.verification;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Computations;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.StandardComputations;
import org.jmouse.parallax.circuit.ComponentCheck;
import org.jmouse.parallax.numerics.LinearAlgebra;
import org.jmouse.parallax.numerics.OdeIntegrator;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.verification.ConsistencyCheck.CheckResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The shipped consistency checks. */
public final class StandardChecks {

    private StandardChecks() {
    }

    public static List<ConsistencyCheck> all() {
        return List.of(new ParameterRange(), new DerivativeAgreement(), new SectionAgreement(), new CriticalGradient(),
                new TrajectoryReversibility(), new EigenResidual(), new MeshFiniteness(), new ComponentCheck(), new org.jmouse.parallax.circuit.CircuitExpectations());
    }

    /** Every parameter at its minimum and its maximum still gives a finite value at the centre of the domain. */
    static final class ParameterRange implements ConsistencyCheck {
        public String code() {
            return "PARAMETER_RANGE";
        }

        public String description() {
            return "The object evaluates to finite numbers at the ends of every parameter's range";
        }

        public boolean appliesTo(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.ExplicitCurve || definition instanceof ObjectDefinition.ExplicitSurface;
        }

        public CheckResult check(CompiledObject object) {
            if (object.parameters().isEmpty()) {
                return CheckResult.skipped("The object has no parameters.");
            }
            int failures = 0;
            int trials = 0;
            for (ParameterDefinition parameter : object.parameters()) {
                for (double value : new double[]{parameter.minimum(), parameter.maximum()}) {
                    CompiledObject variant = object.withParameters(Map.of(parameter.name(), value));
                    trials++;
                    if (!Double.isFinite(centreValue(variant))) {
                        failures++;
                    }
                }
            }
            return failures == 0 ? new CheckResult(Outcome.PASSED, 0.0, 0.0, trials + " parameter ends evaluated")
                    : new CheckResult(Outcome.WARNED, (double) failures, 0.0, failures + " of " + trials
                    + " parameter ends give a non-finite value at the centre of the domain");
        }

        private static double centreValue(CompiledObject object) {
            return switch (object.definition()) {
                case ObjectDefinition.ExplicitCurve curve -> object.function(curve.expression(), curve.variables()).at(curve.x().at(0.5));
                case ObjectDefinition.ExplicitSurface surface -> object.function(surface.expression(), surface.variables())
                        .at(surface.x().at(0.5), surface.y().at(0.5));
                default -> 0;
            };
        }
    }

    /** The symbolic derivative agrees with a central difference at points across the domain. */
    static final class DerivativeAgreement implements ConsistencyCheck {
        public String code() {
            return "DERIVATIVE_AGREEMENT";
        }

        public String description() {
            return "Exact derivatives agree with central differences";
        }

        public boolean appliesTo(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.ExplicitCurve || definition instanceof ObjectDefinition.ExplicitSurface;
        }

        public CheckResult check(CompiledObject object) {
            double worst = 0;
            int compared = 0;
            switch (object.definition()) {
                case ObjectDefinition.ExplicitCurve curve -> {
                    BoundFunction value = object.function(curve.expression(), curve.variables());
                    BoundFunction slope = object.function(object.derivativeExpression(curve.expression(), curve.variables().getFirst()),
                            curve.variables());
                    for (int index = 1; index < 10; index++) {
                        double x = curve.x().at(index / 10.0);
                        double step = 1e-5 * Math.max(1, Math.abs(x));
                        double numeric = (value.at(x + step) - value.at(x - step)) / (2 * step);
                        double exact = slope.at(x);
                        if (Double.isFinite(numeric) && Double.isFinite(exact)) {
                            worst = Math.max(worst, Math.abs(numeric - exact) / Math.max(1, Math.abs(exact)));
                            compared++;
                        }
                    }
                }
                case ObjectDefinition.ExplicitSurface surface -> {
                    BoundFunction value = object.function(surface.expression(), surface.variables());
                    BoundFunction partialX = object.function(object.derivativeExpression(surface.expression(), surface.variables().get(0)), surface.variables());
                    BoundFunction partialY = object.function(object.derivativeExpression(surface.expression(), surface.variables().get(1)), surface.variables());
                    for (int row = 1; row < 5; row++) {
                        for (int column = 1; column < 5; column++) {
                            double x = surface.x().at(column / 5.0);
                            double y = surface.y().at(row / 5.0);
                            double step = 1e-5;
                            double numericX = (value.at(x + step, y) - value.at(x - step, y)) / (2 * step);
                            double numericY = (value.at(x, y + step) - value.at(x, y - step)) / (2 * step);
                            double exactX = partialX.at(x, y);
                            double exactY = partialY.at(x, y);
                            if (Double.isFinite(numericX + numericY + exactX + exactY)) {
                                worst = Math.max(worst, Math.abs(numericX - exactX) / Math.max(1, Math.abs(exactX)));
                                worst = Math.max(worst, Math.abs(numericY - exactY) / Math.max(1, Math.abs(exactY)));
                                compared++;
                            }
                        }
                    }
                }
                default -> {
                    return CheckResult.skipped("not a function");
                }
            }
            if (compared == 0) {
                return CheckResult.skipped("No point of the domain gave finite values.");
            }
            return CheckResult.judge(worst, 1e-4, 1e-2, "Largest relative difference over " + compared + " points");
        }
    }

    /** The CROSS_SECTION profile lies on the surface it was cut from. */
    static final class SectionAgreement implements ConsistencyCheck {
        private final Computations computations = StandardComputations.registry();

        public String code() {
            return "SECTION_AGREEMENT";
        }

        public String description() {
            return "A vertical section's profile lies on its surface";
        }

        public boolean appliesTo(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.ExplicitSurface;
        }

        public CheckResult check(CompiledObject object) {
            ObjectDefinition.ExplicitSurface surface = (ObjectDefinition.ExplicitSurface) object.definition();
            double offset = surface.x().at(0.37);
            var computation = computations.require(RepresentationKind.CROSS_SECTION);
            Dataset dataset = computation.compute(object, ComputationSettings.resolve(computation.settings(),
                    Map.of("axis", "X", "offset", offset, "resolution", 64)), ComputationProgress.NONE);
            Dataset.Polylines profile = (Dataset.Polylines) ((Dataset.Composite) dataset).parts().get("profile");
            BoundFunction value = object.function(surface.expression(), surface.variables());
            float[] points = profile.lines().getFirst().points();
            double worst = 0;
            for (int index = 0; index < points.length / 2; index++) {
                double y = points[index * 2];
                double z = points[index * 2 + 1];
                double expected = value.at(offset, y);
                if (Double.isFinite(expected) && Double.isFinite(z)) {
                    worst = Math.max(worst, Math.abs(expected - z) / Math.max(1, Math.abs(expected)));
                }
            }
            return CheckResult.judge(worst, 1e-5, 1e-3, "Largest relative distance of the profile from the surface");
        }
    }

    /** At every critical point found, the gradient vanishes. */
    static final class CriticalGradient implements ConsistencyCheck {
        private final Computations computations = StandardComputations.registry();

        public String code() {
            return "CRITICAL_GRADIENT";
        }

        public String description() {
            return "The gradient vanishes at the critical points that were found";
        }

        public boolean appliesTo(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.ExplicitSurface;
        }

        public CheckResult check(CompiledObject object) {
            ObjectDefinition.ExplicitSurface surface = (ObjectDefinition.ExplicitSurface) object.definition();
            var computation = computations.require(RepresentationKind.CRITICAL_POINTS);
            Dataset.FeaturePoints points = (Dataset.FeaturePoints) computation.compute(object,
                    ComputationSettings.resolve(computation.settings(), Map.of("seeds", 8)), ComputationProgress.NONE);
            if (points.features().isEmpty()) {
                return CheckResult.skipped("No critical point in the domain.");
            }
            org.jmouse.formula.Formula fx = object.derivativeExpression(surface.expression(), surface.variables().get(0));
            org.jmouse.formula.Formula fy = object.derivativeExpression(surface.expression(), surface.variables().get(1));
            BoundFunction gradientX = object.function(fx, surface.variables());
            BoundFunction gradientY = object.function(fy, surface.variables());
            double worst = 0;
            for (Dataset.Feature feature : points.features()) {
                worst = Math.max(worst, Math.hypot(gradientX.at(feature.position()[0], feature.position()[1]),
                        gradientY.at(feature.position()[0], feature.position()[1])));
            }
            return CheckResult.judge(worst, 1e-6, 1e-3, "Largest |∇f| over " + points.features().size() + " critical point(s)");
        }
    }

    /** A trajectory integrated forwards and then backwards returns to where it started. */
    static final class TrajectoryReversibility implements ConsistencyCheck {
        public String code() {
            return "TRAJECTORY_REVERSIBILITY";
        }

        public String description() {
            return "Integrating forwards then backwards returns to the start";
        }

        public boolean appliesTo(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.DifferentialSystem;
        }

        public CheckResult check(CompiledObject object) {
            ObjectDefinition.DifferentialSystem system = (ObjectDefinition.DifferentialSystem) object.definition();
            List<String> arguments = new ArrayList<>(system.variables());
            arguments.add("t");
            List<BoundFunction> equations = object.functions(system.equations(), arguments);
            int dimension = system.variables().size();
            double[] buffer = new double[dimension + 1];
            OdeIntegrator.OrdinarySystem flow = (time, state, derivative) -> {
                System.arraycopy(state, 0, buffer, 0, dimension);
                buffer[dimension] = time;
                for (int index = 0; index < dimension; index++) {
                    derivative[index] = equations.get(index).at(buffer);
                }
            };
            double[] start = new double[dimension];
            for (int index = 0; index < dimension; index++) {
                start[index] = system.bounds().isEmpty() ? 0.5 : system.bounds().get(index).at(0.6);
            }
            double span = Math.min(1, system.time().length());
            double from = system.time().minimum();
            OdeIntegrator.Solution forward = OdeIntegrator.integrate(flow, start, from, from + span, 2, 1e-11, 200_000, 1e12,
                    ComputationProgress.NONE);
            double[] end = forward.states()[forward.states().length - 1];
            OdeIntegrator.Solution backward = OdeIntegrator.integrate(flow, end, from + span, from, 2, 1e-11, 200_000, 1e12,
                    ComputationProgress.NONE);
            double[] back = backward.states()[backward.states().length - 1];
            double distance = 0;
            double scale = 1;
            for (int index = 0; index < dimension; index++) {
                distance += (back[index] - start[index]) * (back[index] - start[index]);
                scale = Math.max(scale, Math.abs(start[index]));
            }
            return CheckResult.judge(Math.sqrt(distance) / scale, 1e-6, 1e-3, "Relative distance from the start after "
                    + span + " time units there and back");
        }
    }

    /** Every real eigenpair satisfies A v = λ v. */
    static final class EigenResidual implements ConsistencyCheck {
        public String code() {
            return "EIGEN_RESIDUAL";
        }

        public String description() {
            return "Real eigenpairs satisfy A v = λ v";
        }

        public boolean appliesTo(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.MatrixObject matrix
                    && matrix.entries().size() == matrix.entries().getFirst().size();
        }

        public CheckResult check(CompiledObject object) {
            List<List<String>> entries = ((ObjectDefinition.MatrixObject) object.definition()).entries();
            double[][] matrix = new double[entries.size()][entries.size()];
            for (int row = 0; row < matrix.length; row++) {
                for (int column = 0; column < matrix.length; column++) {
                    matrix[row][column] = object.constant(entries.get(row).get(column));
                }
            }
            double worst = 0;
            int real = 0;
            for (Dataset.Eigenpair pair : LinearAlgebra.eigenpairs(matrix)) {
                if (pair.vector() == null) {
                    continue;
                }
                real++;
                double[] image = LinearAlgebra.apply(matrix, pair.vector());
                for (int index = 0; index < image.length; index++) {
                    worst = Math.max(worst, Math.abs(image[index] - pair.real() * pair.vector()[index]));
                }
            }
            if (real == 0) {
                return CheckResult.skipped("The matrix has no real eigenvalue.");
            }
            return CheckResult.judge(worst, 1e-8, 1e-5, "Largest |Av − λv| over " + real + " real eigenpair(s)");
        }
    }

    /** A surface's mesh at its default parameters is finite almost everywhere. */
    static final class MeshFiniteness implements ConsistencyCheck {
        private final Computations computations = StandardComputations.registry();

        public String code() {
            return "MESH_FINITENESS";
        }

        public String description() {
            return "The surface mesh is finite almost everywhere";
        }

        public boolean appliesTo(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.ExplicitSurface || definition instanceof ObjectDefinition.ParametricSurface;
        }

        public CheckResult check(CompiledObject object) {
            var computation = computations.require(RepresentationKind.SURFACE);
            Dataset.Mesh mesh = (Dataset.Mesh) computation.compute(object, ComputationSettings.resolve(computation.settings(),
                    Map.of("resolution", 32)), ComputationProgress.NONE);
            int total = mesh.positions().length;
            int finite = 0;
            for (float value : mesh.positions()) {
                if (Float.isFinite(value)) {
                    finite++;
                }
            }
            double missing = 1 - (double) finite / total;
            return CheckResult.judge(missing, 0.0, 0.05, "Share of non-finite coordinates in a 32×32 mesh");
        }
    }
}
