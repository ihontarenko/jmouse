package org.jmouse.cad.spice;

import org.jmouse.core.process.PipedProgram;
import org.jmouse.core.process.PipedProgramException;
import org.jmouse.plugin.PluginSetting;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * A SPICE program as a process that reads a deck from its standard input and prints its answer on its output — {@code
 * ngspice_con -b} on this machine, {@code docker run --rm -i jmcad-ngspice} or {@code jmcad-xyce} in a container. One
 * process a run; it is killed when it outlives its time. What the deck asks it to print is its dialect's business
 * ({@link SpiceDialect}); this only starts it, feeds it and says why it failed, in its own name.
 */
public final class SpiceProcess implements SpiceSimulator {

    /** A printout is at most this long: a run that prints more is refused rather than held in memory. */
    private static final int MAXIMUM_OUTPUT = 64 * 1024 * 1024;

    /** What a SPICE program says when a run cannot go on. */
    private static final String FATAL = "fatal error";

    /** How many of a failed run's last lines are said with the failure. */
    private static final int SAID_LINES = 6;

    /** The setting of how long one run may take, by the key a plugin running a SPICE program keeps it under. */
    public static final String TIMEOUT = "timeout";

    /** The setting of how the program is started, by the key a plugin running a SPICE program keeps it under. */
    public static final String COMMAND = "command";

    private final PipedProgram program;

    /**
     * @param program its name, as the messages say it: {@code ngspice}, {@code Xyce}
     * @param command how it is started, word by word, read as each run starts; empty while there is no such program
     * @param timeout how long one run may take, read as each run starts
     */
    public SpiceProcess(String program, Supplier<List<String>> command, Supplier<Duration> timeout) {
        this.program = new PipedProgram(program, command, timeout, MAXIMUM_OUTPUT);
    }

    /**
     * The setting a plugin running a SPICE program declares for how it is started; {@code usual} is what the server is
     * configured with.
     */
    public static PluginSetting commandSetting(String program, String answer, List<String> usual) {
        return PluginSetting.command(COMMAND, "Program", "How " + program + " is started on the server, one word to a line. "
                + "It is handed a deck on its standard input and prints " + answer + ". Empty turns the engine off.", usual);
    }

    /**
     * The setting a plugin running a SPICE program declares for how long one run may take, from a second to half an
     * hour; {@code usual} is what the server is configured with.
     */
    public static PluginSetting timeoutSetting(String program, Duration usual) {
        return PluginSetting.duration(TIMEOUT, "Longest run", "How long one " + program + " run may take before it is "
                + "stopped.", usual, Duration.ofSeconds(1), Duration.ofMinutes(30));
    }

    @Override
    public boolean isSet() {
        return program.available();
    }

    @Override
    public String run(String deck) {
        PipedProgram.Ran ran;
        try {
            ran = program.run(deck.getBytes(StandardCharsets.UTF_8));
        } catch (PipedProgramException failed) {
            throw new SpiceRunException(failed.getMessage(), failed);
        }
        String output = ran.outputText();
        String errors = ran.errors();
        // ngspice in batch mode with a control block exits with 1 after printing everything asked, its last word a note
        // that no .print line ran: a failure is a fatal error, or a run that printed nothing.
        boolean fatal = errors.contains(FATAL) || output.contains(FATAL);
        if (fatal || (ran.exitCode() != 0 && output.isBlank())) {
            throw new SpiceRunException(program.name() + " failed (exit " + ran.exitCode() + "): "
                    + PipedProgram.lastLines(errors.isBlank() ? output : errors, SAID_LINES));
        }
        // Its complaints after what it printed, never inside it: a reader that misses what it asked for says why.
        return errors.isBlank() ? output : output + System.lineSeparator() + errors;
    }
}
