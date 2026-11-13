package org.jmouse.cad.plugin.step;

import org.jmouse.core.process.PipedProgram;
import org.jmouse.core.process.PipedProgramException;
import org.jmouse.mesh.Mesh;
import org.jmouse.mesh.MeshReadException;
import org.jmouse.mesh.MeshReader;
import org.jmouse.mesh.vrml.VrmlReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * STEP as a mesh: the file handed to OpenCascade, run as a program ({@code docker run --rm -i jm_cad-step}), which cuts
 * every body into triangles and answers with VRML — in millimetres, Z up, as the STEP file is —, read here as any VRML
 * model is. Nothing of OpenCascade is in the server.
 */
public final class StepMeshReader implements MeshReader {

    /** How many of a failed conversion's last lines are said with the failure. */
    private static final int SAID_LINES = 4;

    private final PipedProgram program;
    private final VrmlReader   vrml = new VrmlReader();

    public StepMeshReader(PipedProgram program) {
        this.program = program;
    }

    @Override
    public String format() {
        return "step";
    }

    @Override
    public boolean available() {
        return program.available();
    }

    @Override
    public Mesh read(InputStream input, String name) throws IOException {
        PipedProgram.Ran ran;
        try {
            ran = program.run(input.readAllBytes());
        } catch (PipedProgramException failed) {
            throw new MeshReadException(name + ": " + failed.getMessage(), failed);
        }
        if (ran.exitCode() != 0 || ran.output().length == 0) {
            throw new MeshReadException(name + ": " + program.name() + " did not cut it into triangles (exit " + ran.exitCode()
                    + "): " + PipedProgram.lastLines(ran.errors(), SAID_LINES));
        }
        return vrml.read(new ByteArrayInputStream(ran.output()), name);
    }
}
