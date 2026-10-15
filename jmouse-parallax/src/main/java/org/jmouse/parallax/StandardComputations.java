package org.jmouse.parallax;

import org.jmouse.parallax.analysis.AnalysisComputations;
import org.jmouse.parallax.circuit.CircuitComputations;
import org.jmouse.parallax.program.ProgramComputations;
import org.jmouse.parallax.complex.ComplexComputations;
import org.jmouse.parallax.dimension.HigherDimensionComputations;
import org.jmouse.parallax.dynamics.DynamicsComputations;
import org.jmouse.parallax.dynamics.FlowImageComputation;
import org.jmouse.parallax.field.FieldComputations;
import org.jmouse.parallax.fractal.FractalComputations;
import org.jmouse.parallax.geometry.GeometryComputations;
import org.jmouse.parallax.linear.LinearComputations;
import org.jmouse.parallax.parameter.ParameterComputations;
import org.jmouse.parallax.probability.ProbabilityComputations;
import org.jmouse.parallax.region.RegionComputations;
import org.jmouse.parallax.value.ValueComputations;

import java.util.List;

/** Every computation this library ships, one per {@link RepresentationKind}: the registry a product starts from. */
public final class StandardComputations {

    private StandardComputations() {
    }

    public static List<Computation> all() {
        return List.of(
                new AnalysisComputations.CriticalPoints(),
                new AnalysisComputations.CrossSection(),
                new AnalysisComputations.Curvature(),
                new AnalysisComputations.Derivative(),
                new AnalysisComputations.Gradient(),
                new AnalysisComputations.Integral(),
                new AnalysisComputations.LevelSet(),
                new AnalysisComputations.Normals(),
                new AnalysisComputations.Projection(),
                new AnalysisComputations.TangentPlane(),
                new ComplexComputations.DomainColoring(),
                new ComplexComputations.RiemannSphere(),
                new DynamicsComputations.Bifurcation(),
                new DynamicsComputations.Equilibria(),
                new DynamicsComputations.Orbit(),
                new DynamicsComputations.PhasePortrait(),
                new DynamicsComputations.Trajectory(),
                new FlowImageComputation(),
                new FieldComputations.Arrows(),
                new FieldComputations.Particles(),
                new FieldComputations.ScalarField(),
                new FieldComputations.Streamlines(),
                new FractalComputations.ChaosGame(),
                new FractalComputations.EscapeTime(),
                new FractalComputations.NewtonBasins(),
                new GeometryComputations.Construction(),
                new GeometryComputations.Curve(),
                new GeometryComputations.ImplicitSurface(),
                new GeometryComputations.Points(),
                new GeometryComputations.Surface(),
                new GeometryComputations.TurtlePath(),
                new HigherDimensionComputations.PolytopeProjection(),
                new HigherDimensionComputations.PolytopeSlice(),
                new LinearComputations.MatrixAnalysis(),
                new LinearComputations.SvdDecomposition(),
                new LinearComputations.TransformedGrid(),
                new ParameterComputations.GradientDescent(),
                new ParameterComputations.ParameterSweep(),
                new ProbabilityComputations.DensityView(),
                new ProbabilityComputations.RandomWalk(),
                new ProbabilityComputations.Samples(),
                new ProbabilityComputations.SequenceTerms(),
                new CircuitComputations.TransientResponse(),
                new CircuitComputations.FrequencyResponse(),
                new CircuitComputations.NoiseResponse(),
                new CircuitComputations.PhasorDiagram(),
                new CircuitComputations.OperatingPoint(),
                new CircuitComputations.DirectCurrentSweep(),
                new ProgramComputations.Output(),
                new RegionComputations.Picture(),
                new ValueComputations.Evaluation()
        );
    }

    /** The registry of every shipped computation; it refuses to exist if a representation has none. */
    public static Computations registry() {
        return new Computations(all());
    }
}
