package org.jmouse.parallax.file;

import org.jmouse.parallax.Dataset;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** The shipped dataset formats, and the one place a dataset is written to text in one of them. */
public final class DatasetFormats {

    private final List<DatasetFormat> formats;

    public DatasetFormats(List<DatasetFormat> formats) {
        this.formats = List.copyOf(formats);
    }

    public static DatasetFormats standard() {
        return new DatasetFormats(List.of(new WavefrontObject(), new StereolithographyText(), new PolygonFileText(),
                new CommaSeparatedValues()));
    }

    public List<DatasetFormat> all() {
        return formats;
    }

    public Optional<DatasetFormat> byName(String name) {
        return formats.stream().filter(format -> format.name().equalsIgnoreCase(name)).findFirst();
    }

    /** The formats a dataset can be written in, parts of a composite included. */
    public List<DatasetFormat> applicableTo(Dataset dataset) {
        return formats.stream().filter(format -> carrier(format, dataset).isPresent()).toList();
    }

    public String write(DatasetFormat format, Dataset dataset) {
        Dataset carried = carrier(format, dataset).orElseThrow(() -> new IllegalArgumentException(
                "A " + dataset.getClass().getSimpleName() + " cannot be written as " + format.name().toUpperCase(Locale.ROOT) + "."));
        StringWriter writer = new StringWriter();
        try {
            format.write(carried, writer);
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
        return writer.toString();
    }

    private static Optional<Dataset> carrier(DatasetFormat format, Dataset dataset) {
        if (format.accepts(dataset)) {
            return Optional.of(dataset);
        }
        if (dataset instanceof Dataset.Composite composite) {
            for (Map.Entry<String, Dataset> part : composite.parts().entrySet()) {
                Optional<Dataset> found = carrier(format, part.getValue());
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    static String number(double value) {
        return Double.isFinite(value) ? Double.toString(value) : "nan";
    }

    /** A triangle mesh in R³ (a surface in R⁴ is dropped to its first three coordinates). */
    static final class WavefrontObject implements DatasetFormat {

        public String name() {
            return "obj";
        }

        public String extension() {
            return "obj";
        }

        public String mediaType() {
            return "model/obj";
        }

        public boolean accepts(Dataset dataset) {
            return dataset instanceof Dataset.Mesh mesh && mesh.dimension() >= 3;
        }

        public void write(Dataset dataset, Writer target) throws IOException {
            Dataset.Mesh mesh = (Dataset.Mesh) dataset;
            target.write("# Parallax mesh\n");
            int vertices = mesh.positions().length / mesh.dimension();
            for (int vertex = 0; vertex < vertices; vertex++) {
                int base = vertex * mesh.dimension();
                target.write("v " + number(mesh.positions()[base]) + " " + number(mesh.positions()[base + 1]) + " "
                        + number(mesh.positions()[base + 2]) + "\n");
            }
            if (mesh.normals() != null) {
                for (int vertex = 0; vertex < vertices; vertex++) {
                    target.write("vn " + mesh.normals()[vertex * 3] + " " + mesh.normals()[vertex * 3 + 1] + " "
                            + mesh.normals()[vertex * 3 + 2] + "\n");
                }
            }
            int[] indices = mesh.indices();
            for (int index = 0; index < indices.length; index += 3) {
                int a = indices[index] + 1;
                int b = indices[index + 1] + 1;
                int c = indices[index + 2] + 1;
                target.write(mesh.normals() != null ? "f " + a + "//" + a + " " + b + "//" + b + " " + c + "//" + c + "\n"
                        : "f " + a + " " + b + " " + c + "\n");
            }
        }
    }

    /** ASCII STL: what a 3D printer takes. Each facet carries its own normal. */
    static final class StereolithographyText implements DatasetFormat {

        public String name() {
            return "stl";
        }

        public String extension() {
            return "stl";
        }

        public String mediaType() {
            return "model/stl";
        }

        public boolean accepts(Dataset dataset) {
            return dataset instanceof Dataset.Mesh mesh && mesh.dimension() >= 3;
        }

        public void write(Dataset dataset, Writer target) throws IOException {
            Dataset.Mesh mesh = (Dataset.Mesh) dataset;
            int dimension = mesh.dimension();
            float[] positions = mesh.positions();
            target.write("solid parallax\n");
            int[] indices = mesh.indices();
            for (int index = 0; index < indices.length; index += 3) {
                double[][] corner = new double[3][3];
                for (int vertex = 0; vertex < 3; vertex++) {
                    for (int axis = 0; axis < 3; axis++) {
                        corner[vertex][axis] = positions[indices[index + vertex] * dimension + axis];
                    }
                }
                double ux = corner[1][0] - corner[0][0];
                double uy = corner[1][1] - corner[0][1];
                double uz = corner[1][2] - corner[0][2];
                double vx = corner[2][0] - corner[0][0];
                double vy = corner[2][1] - corner[0][1];
                double vz = corner[2][2] - corner[0][2];
                double nx = uy * vz - uz * vy;
                double ny = uz * vx - ux * vz;
                double nz = ux * vy - uy * vx;
                double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length > 0) {
                    nx /= length;
                    ny /= length;
                    nz /= length;
                }
                target.write("  facet normal " + number(nx) + " " + number(ny) + " " + number(nz) + "\n    outer loop\n");
                for (double[] point : corner) {
                    target.write("      vertex " + number(point[0]) + " " + number(point[1]) + " " + number(point[2]) + "\n");
                }
                target.write("    endloop\n  endfacet\n");
            }
            target.write("endsolid parallax\n");
        }
    }

    /** ASCII PLY, which keeps the per-vertex scalar (height, curvature, phase) as a property. */
    static final class PolygonFileText implements DatasetFormat {

        public String name() {
            return "ply";
        }

        public String extension() {
            return "ply";
        }

        public String mediaType() {
            return "model/x-ply";
        }

        public boolean accepts(Dataset dataset) {
            return dataset instanceof Dataset.Mesh mesh && mesh.dimension() >= 3;
        }

        public void write(Dataset dataset, Writer target) throws IOException {
            Dataset.Mesh mesh = (Dataset.Mesh) dataset;
            int dimension = mesh.dimension();
            int vertices = mesh.positions().length / dimension;
            boolean scalars = mesh.scalars() != null;
            target.write("ply\nformat ascii 1.0\ncomment Parallax mesh\nelement vertex " + vertices + "\n");
            target.write("property float x\nproperty float y\nproperty float z\n");
            if (scalars) {
                target.write("property float " + (mesh.scalarName() == null ? "value" : mesh.scalarName().replace('-', '_')) + "\n");
            }
            target.write("element face " + mesh.indices().length / 3 + "\nproperty list uchar int vertex_indices\nend_header\n");
            for (int vertex = 0; vertex < vertices; vertex++) {
                int base = vertex * dimension;
                target.write(number(mesh.positions()[base]) + " " + number(mesh.positions()[base + 1]) + " "
                        + number(mesh.positions()[base + 2]) + (scalars ? " " + number(mesh.scalars()[vertex]) : "") + "\n");
            }
            int[] indices = mesh.indices();
            for (int index = 0; index < indices.length; index += 3) {
                target.write("3 " + indices[index] + " " + indices[index + 1] + " " + indices[index + 2] + "\n");
            }
        }
    }

    /** A table: series by their axis, points by their coordinates, polylines with a line number, grids as x,y,value. */
    static final class CommaSeparatedValues implements DatasetFormat {

        public String name() {
            return "csv";
        }

        public String extension() {
            return "csv";
        }

        public String mediaType() {
            return "text/csv";
        }

        public boolean accepts(Dataset dataset) {
            return dataset instanceof Dataset.TimeSeries || dataset instanceof Dataset.PointCloud
                    || dataset instanceof Dataset.Polylines || dataset instanceof Dataset.ScalarGrid
                    || dataset instanceof Dataset.FeaturePoints || dataset instanceof Dataset.Measurements;
        }

        public void write(Dataset dataset, Writer target) throws IOException {
            switch (dataset) {
                case Dataset.TimeSeries series -> {
                    target.write(series.axis() + "," + String.join(",", series.columns()) + "\n");
                    for (int row = 0; row < series.axisValues().length; row++) {
                        StringBuilder line = new StringBuilder(number(series.axisValues()[row]));
                        for (float[] column : series.values()) {
                            line.append(',').append(number(column[row]));
                        }
                        target.write(line.append('\n').toString());
                    }
                }
                case Dataset.PointCloud cloud -> {
                    target.write(axes(cloud.dimension()) + (cloud.values() == null ? "" : "," + (cloud.valueName() == null ? "value" : cloud.valueName())) + "\n");
                    int count = cloud.points().length / cloud.dimension();
                    for (int point = 0; point < count; point++) {
                        StringBuilder line = new StringBuilder();
                        for (int axis = 0; axis < cloud.dimension(); axis++) {
                            line.append(axis == 0 ? "" : ",").append(number(cloud.points()[point * cloud.dimension() + axis]));
                        }
                        if (cloud.values() != null) {
                            line.append(',').append(number(cloud.values()[point]));
                        }
                        target.write(line.append('\n').toString());
                    }
                }
                case Dataset.Polylines lines -> {
                    target.write("line," + axes(lines.dimension()) + "\n");
                    for (int line = 0; line < lines.lines().size(); line++) {
                        float[] points = lines.lines().get(line).points();
                        for (int point = 0; point < points.length / lines.dimension(); point++) {
                            StringBuilder row = new StringBuilder(Integer.toString(line));
                            for (int axis = 0; axis < lines.dimension(); axis++) {
                                row.append(',').append(number(points[point * lines.dimension() + axis]));
                            }
                            target.write(row.append('\n').toString());
                        }
                    }
                }
                case Dataset.ScalarGrid grid -> {
                    target.write("x,y," + (grid.quantity() == null ? "value" : grid.quantity()) + "\n");
                    for (int row = 0; row < grid.rows(); row++) {
                        for (int column = 0; column < grid.columns(); column++) {
                            target.write(number(grid.x().sample(column, grid.columns())) + ","
                                    + number(grid.y().sample(row, grid.rows())) + ","
                                    + number(grid.values()[row * grid.columns() + column]) + "\n");
                        }
                    }
                }
                case Dataset.FeaturePoints features -> {
                    target.write("classification,value,coordinates\n");
                    for (Dataset.Feature feature : features.features()) {
                        StringBuilder coordinates = new StringBuilder();
                        if (feature.position() != null) {
                            for (double coordinate : feature.position()) {
                                coordinates.append(coordinates.isEmpty() ? "" : " ").append(number(coordinate));
                            }
                        }
                        target.write(feature.classification() + "," + (feature.value() == null ? "" : number(feature.value()))
                                + "," + coordinates + "\n");
                    }
                }
                case Dataset.Measurements measurements -> {
                    target.write("name,value\n");
                    for (Map.Entry<String, Double> entry : measurements.values().entrySet()) {
                        target.write(entry.getKey() + "," + number(entry.getValue()) + "\n");
                    }
                }
                default -> throw new IllegalArgumentException("not a table");
            }
        }

        private static String axes(int dimension) {
            String[] names = {"x", "y", "z", "w", "v5", "v6", "v7", "v8"};
            StringBuilder header = new StringBuilder();
            for (int axis = 0; axis < dimension; axis++) {
                header.append(axis == 0 ? "" : ",").append(names[axis]);
            }
            return header.toString();
        }
    }
}
