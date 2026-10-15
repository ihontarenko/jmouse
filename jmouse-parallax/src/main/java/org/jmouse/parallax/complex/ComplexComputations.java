package org.jmouse.parallax.complex;

import org.jmouse.formula.Complex;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.PlaneWindow;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.Sampling;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.numerics.GridMesh;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.ComplexMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** w = f(z) coloured by modulus and phase, and on the Riemann sphere; the grid it bends is a {@code TRANSFORMED_GRID}. */
public final class ComplexComputations {

    private ComplexComputations() {
    }

    static Dataset.ComplexGrid sample(CompiledObject object, ComplexMapping mapping, Interval real, Interval imaginary, int resolution,
                                      ComputationProgress progress) {
        CompiledObject.ComplexBinding function = object.complexFunction(mapping.expression(), mapping.variables());
        float[] modulus = new float[resolution * resolution];
        float[] phase = new float[resolution * resolution];
        for (int row = 0; row < resolution; row++) {
            if (row % 32 == 0) {
                progress.checkpoint((double) row / resolution, "sampling");
            }
            double imaginaryValue = imaginary.sample(row, resolution);
            for (int column = 0; column < resolution; column++) {
                Complex value = function.at(new Complex(real.sample(column, resolution), imaginaryValue));
                modulus[row * resolution + column] = (float) value.modulus();
                phase[row * resolution + column] = (float) value.argument();
            }
        }
        return new Dataset.ComplexGrid(resolution, resolution, real, imaginary, modulus, phase);
    }

    public static class DomainColoring implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.DOMAIN_COLORING;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ComplexMapping;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("resolution", 256, 16, 2048, "Samples per axis"), PlaneWindow.specification());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * 4;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ComplexMapping mapping = (ComplexMapping) object.definition();
            Interval[] region = PlaneWindow.region(settings, mapping.real(), mapping.imaginary());
            return sample(object, mapping, region[0], region[1], settings.integer("resolution"), progress);
        }
    }

    /**
     * The extended plane wrapped onto the unit sphere by inverse stereographic projection, coloured by f: poles and
     * the behaviour at infinity become points one can look at.
     */
    public static class RiemannSphere implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.RIEMANN_SPHERE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ComplexMapping;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("resolution", 96, 8, 512, "Samples in latitude and longitude"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * 4;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ComplexMapping mapping = (ComplexMapping) object.definition();
            CompiledObject.ComplexBinding function = object.complexFunction(mapping.expression(), mapping.variables());
            int resolution = settings.integer("resolution");
            Dataset.Mesh sphere = GridMesh.build((longitude, latitude, target) -> {
                target[0] = Math.cos(latitude) * Math.cos(longitude);
                target[1] = Math.cos(latitude) * Math.sin(longitude);
                target[2] = Math.sin(latitude);
            }, 3, Interval.of(-Math.PI, Math.PI), Interval.of(-Math.PI / 2 + 1e-6, Math.PI / 2 - 1e-6), resolution, resolution,
                    (longitude, latitude, point) -> {
                        // From the north pole: z = (X + iY) / (1 - Z).
                        double denominator = 1 - point[2];
                        Complex z = new Complex(point[0] / denominator, point[1] / denominator);
                        return function.at(z).argument();
                    }, "phase", List.of(), progress);
            return sphere;
        }
    }
}
