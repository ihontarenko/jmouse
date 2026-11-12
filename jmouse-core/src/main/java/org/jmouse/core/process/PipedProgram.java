package org.jmouse.core.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A program run as a pipe: what it is handed goes to its standard input, what it prints comes back — a SPICE deck run
 * by ngspice, a STEP model cut into triangles by OpenCascade, each often {@code docker run --rm -i IMAGE}. One process a
 * run, killed when it outlives its time; what its answer means is its caller's.
 *
 * <p>Its output and its errors are read on threads of their own while its input is written, so a large answer never
 * fills a pipe and stalls both; they are kept apart, so a note on the error stream never lands inside the answer.</p>
 */
public final class PipedProgram {

    private final String       name;
    /** How it is started, read as each run starts: it may be changed while the program is kept; empty is no program. */
    private final Supplier<List<String>> command;
    /** How long a run may take, read as each run starts: it may be changed while the program is kept. */
    private final Supplier<Duration> timeout;
    private final int          largestOutput;

    /**
     * What a run left: how it ended, what it printed, and what it complained of.
     *
     * @param exitCode its exit status
     * @param output   its standard output, whole
     * @param errors   its standard error, as text
     */
    public record Ran(int exitCode, byte[] output, String errors) {

        public String outputText() {
            return new String(output, StandardCharsets.UTF_8);
        }
    }

    /**
     * @param name          the program's name, as a refusal says it: {@code ngspice}, {@code OpenCascade}
     * @param command       how it is started, word by word
     * @param timeout       how long one run may take before it is killed
     * @param largestOutput how many bytes it may print: a run that prints more is refused rather than held in memory
     */
    public PipedProgram(String name, List<String> command, Duration timeout, int largestOutput) {
        this(name, requireCommand(name, command), () -> timeout, largestOutput);
    }

    /**
     * @param timeout how long one run may take before it is killed, read as each run starts
     */
    public PipedProgram(String name, List<String> command, Supplier<Duration> timeout, int largestOutput) {
        this(name, requireCommand(name, command), timeout, largestOutput);
    }

    /**
     * @param command how it is started, word by word, read as each run starts; empty while there is no such program
     * @param timeout how long one run may take before it is killed, read as each run starts
     */
    public PipedProgram(String name, Supplier<List<String>> command, Supplier<Duration> timeout, int largestOutput) {
        this.name = name;
        this.command = command;
        this.timeout = timeout;
        this.largestOutput = largestOutput;
    }

    private static Supplier<List<String>> requireCommand(String name, List<String> command) {
        if (command.isEmpty()) {
            throw new IllegalArgumentException(name + " needs a command to start it.");
        }
        List<String> kept = List.copyOf(command);
        return () -> kept;
    }

    public String name() {
        return name;
    }

    public List<String> command() {
        return command.get();
    }

    /** Whether there is a program to run: its command names one now. */
    public boolean available() {
        return !command.get().isEmpty();
    }

    /**
     * Runs the program once on this input.
     *
     * @throws PipedProgramException when it cannot be started, does not take its input, outlives its time, prints too
     *                               much or cannot be read — each said in the program's name
     */
    public Ran run(byte[] input) {
        List<String> command = this.command.get();
        if (command.isEmpty()) {
            throw new PipedProgramException("No program is set to run " + name + ".");
        }
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException refused) {
            throw new PipedProgramException(name + " could not be started (" + String.join(" ", command) + "): "
                    + refused.getMessage(), refused);
        }
        CompletableFuture<byte[]> printed = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()),
                reader -> Thread.ofVirtual().name(name + "-output").start(reader));
        CompletableFuture<byte[]> complained = CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()),
                reader -> Thread.ofVirtual().name(name + "-errors").start(reader));
        try (OutputStream written = process.getOutputStream()) {
            written.write(input);
        } catch (IOException refused) {
            process.destroyForcibly();
            throw new PipedProgramException(name + " did not take its input: " + refused.getMessage(), refused);
        }
        Duration timeout = this.timeout.get();
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new PipedProgramException(name + " did not finish within " + timeout.toSeconds() + " s.");
            }
            byte[] output = printed.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            byte[] errors = complained.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return new Ran(process.exitValue(), output, new String(errors, StandardCharsets.UTF_8));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new PipedProgramException("The " + name + " run was interrupted.", interrupted);
        } catch (ExecutionException failed) {
            process.destroyForcibly();
            if (failed.getCause() instanceof PipedProgramException said) {
                throw said;
            }
            throw new PipedProgramException(name + "'s output could not be read: " + failed.getCause().getMessage(), failed);
        } catch (TimeoutException failed) {
            process.destroyForcibly();
            throw new PipedProgramException(name + "'s output was not read within " + timeout.toSeconds() + " s.", failed);
        }
    }

    private byte[] readAll(InputStream stream) {
        try (stream) {
            byte[] bytes = stream.readNBytes(largestOutput + 1);
            if (bytes.length > largestOutput) {
                throw new PipedProgramException(name + " printed more than " + largestOutput / (1024 * 1024) + " MB.");
            }
            return bytes;
        } catch (IOException failed) {
            throw new PipedProgramException(name + "'s output could not be read: " + failed.getMessage(), failed);
        }
    }

    /** The end of what a program printed — where a failing one says why —, its last lines on one line. */
    public static String lastLines(String printed, int lines) {
        String[] all = printed.strip().split("\\R");
        return String.join(" | ", List.of(all).subList(Math.max(0, all.length - lines), all.length)).strip();
    }
}
