package org.jmouse.odb;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An ODB++ job of one step: the matrix of its layers, top to bottom, the step's profile, and each layer's features — as the
 * tree of files the product model keeps ({@code matrix/matrix}, {@code misc/info}, {@code steps/<step>/profile},
 * {@code steps/<step>/layers/<layer>/features} …), and that tree as one tgz, the job's folder at its top.
 */
public final class OdbJob {

    /**
     * Who wrote a job, as its {@code misc/info} says.
     *
     * @param software the software that wrote the job
     * @param user     who it was written by
     */
    public record Origin(String software, String user) {
    }

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss");

    private final String                   name;
    private final String                   step;
    private final Origin                   origin;
    private final OffsetDateTime           written;
    private final List<OdbLayer>           layers   = new ArrayList<>();
    private final Map<String, OdbFeatures> features = new LinkedHashMap<>();
    private       OdbFeatures              profile  = new OdbFeatures();
    private       OdbEda                   eda;
    private final List<OdbComponents>      components = new ArrayList<>();

    /**
     * @param name    the job's name: the folder at the top of its tree
     * @param step    the step's name: {@code pcb}
     * @param written when it is written, as its history says
     */
    public OdbJob(String name, String step, Origin origin, OffsetDateTime written) {
        this.name = Objects.requireNonNull(name, "name").toLowerCase().replaceAll("[^a-z0-9_+.-]+", "-");
        this.step = Objects.requireNonNull(step, "step");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.written = Objects.requireNonNull(written, "written");
    }

    /** The next layer of the matrix, below those given before it, and what it holds. */
    public OdbJob layer(OdbLayer layer, OdbFeatures held) {
        if (features.containsKey(layer.name())) {
            throw new IllegalArgumentException("The job already has a layer '" + layer.name() + "'.");
        }
        layers.add(layer);
        features.put(layer.name(), held);
        return this;
    }

    /** The step's profile: the board's outline as a surface, its cut-outs as holes. */
    public OdbJob profile(OdbFeatures outline) {
        this.profile = Objects.requireNonNull(outline, "outline");
        return this;
    }

    /** The step's nets and packages. */
    public OdbJob eda(OdbEda data) {
        this.eda = Objects.requireNonNull(data, "data");
        return this;
    }

    /** A side's components, on the layer of the matrix given for them. */
    public OdbJob components(OdbComponents side) {
        if (!features.containsKey(side.side().layer())) {
            throw new IllegalArgumentException("The job has no layer '" + side.side().layer() + "' for its components.");
        }
        components.add(side);
        return this;
    }

    /** Every file of the job by its path from the job's folder, in the order an archive lists them. */
    public Map<String, String> files() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("matrix/matrix", matrix());
        files.put("misc/info", info());
        files.put("fonts/standard", "XSIZE 0.302000\nYSIZE 0.302000\nOFFSET 0.000000\n");
        files.put("steps/" + step + "/stephdr", "UNITS=MM\nX_DATUM=0\nY_DATUM=0\nX_ORIGIN=0\nY_ORIGIN=0\n");
        files.put("steps/" + step + "/profile", profile.text());
        for (OdbLayer layer : layers) {
            files.put("steps/" + step + "/layers/" + layer.name() + "/features", features.get(layer.name()).text());
        }
        for (OdbComponents side : components) {
            files.put("steps/" + step + "/layers/" + side.side().layer() + "/components", side.text());
        }
        if (eda != null) {
            files.put("steps/" + step + "/eda/data", eda.text(origin.software()));
        }
        return files;
    }

    /** The job as one tgz: its folder at the top, every file beneath it. */
    public byte[] archive() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            Date modified = Date.from(written.toInstant());
            for (Map.Entry<String, String> file : files().entrySet()) {
                byte[] content = file.getValue().getBytes(StandardCharsets.US_ASCII);
                TarArchiveEntry entry = new TarArchiveEntry(name + "/" + file.getKey());
                entry.setSize(content.length);
                entry.setModTime(modified);
                tar.putArchiveEntry(entry);
                tar.write(content);
                tar.closeArchiveEntry();
            }
        } catch (IOException unwritable) {
            throw new UncheckedIOException("The ODB++ job could not be archived.", unwritable);
        }
        return bytes.toByteArray();
    }

    private String matrix() {
        StringBuilder text = new StringBuilder("STEP {\n   COL=1\n   NAME=" + step.toUpperCase() + "\n}\n\n");
        for (int index = 0; index < layers.size(); index++) {
            OdbLayer layer = layers.get(index);
            text.append("LAYER {\n")
                    .append("   ROW=").append(index + 1).append('\n')
                    .append("   CONTEXT=BOARD\n")
                    .append("   TYPE=").append(layer.type()).append('\n')
                    .append("   NAME=").append(layer.name().toUpperCase()).append('\n')
                    .append("   POLARITY=POSITIVE\n")
                    .append("   START_NAME=").append(layer.start() == null ? "" : layer.start().toUpperCase()).append('\n')
                    .append("   END_NAME=").append(layer.end() == null ? "" : layer.end().toUpperCase()).append('\n')
                    .append("   OLD_NAME=\n")
                    .append("   ADD_TYPE=\n")
                    .append("   COLOR=0\n")
                    .append("}\n\n");
        }
        return text.toString();
    }

    private String info() {
        String stamp = STAMP.format(written);
        return "JOB_NAME=" + name + "\n"
               + "ODB_VERSION_MAJOR=8\n"
               + "ODB_VERSION_MINOR=1\n"
               + "ODB_SOURCE=" + origin.software() + "\n"
               + "CREATION_DATE=" + stamp + "\n"
               + "SAVE_DATE=" + stamp + "\n"
               + "SAVE_APP=" + origin.software() + "\n"
               + "SAVE_USER=" + origin.user() + "\n"
               + "UNITS=MM\n"
               + "MAX_UID=0\n";
    }
}
