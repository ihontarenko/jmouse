package org.jmouse.parallax.spice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ngspice as a process: the command starts it in batch mode reading the deck from its standard input — {@code
 * ngspice_con -b} on this machine, or {@code docker run --rm -i parallax-ngspice} where the image's entry point is
 * {@code ngspice -b}. One process a run; it is killed when it outlives its time.
 */
public final class NgspiceProcess implements SpiceSimulator {

    /** A printout is at most this long: a run that prints more is refused rather than held in memory. */
    private static final int MAXIMUM_OUTPUT = 64 * 1024 * 1024;

    private final List<String> command;
    private final Duration     timeout;

    public NgspiceProcess(List<String> command, Duration timeout) {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("ngspice needs a command to start it.");
        }
        this.command = List.copyOf(command);
        this.timeout = timeout;
    }

    @Override
    public String run(String deck) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException refused) {
            throw new SpiceRunException("ngspice could not be started (" + String.join(" ", command) + "): "
                    + refused.getMessage(), refused);
        }
        // The output is read while the deck is written: a large printout would otherwise fill the pipe and stall both.
        // On a thread of its own, since it blocks on the pipe: in the common pool it held a worker every parallel stream needs.
        CompletableFuture<String> printed = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()),
                reader -> Thread.ofVirtual().name("ngspice-output").start(reader));
        try (OutputStream input = process.getOutputStream()) {
            input.write(deck.getBytes(StandardCharsets.UTF_8));
        } catch (IOException refused) {
            process.destroyForcibly();
            throw new SpiceRunException("ngspice did not take the deck: " + refused.getMessage(), refused);
        }
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new SpiceRunException("ngspice did not finish within " + timeout.toSeconds() + " s.");
            }
            String output = printed.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            // ngspice in batch mode with a control block may exit with 1 after printing everything asked: a failure is
            // a non-zero exit with no table printed.
            if (process.exitValue() != 0 && !output.contains("\nIndex")) {
                throw new SpiceRunException("ngspice failed (exit " + process.exitValue() + "): " + lastLines(output));
            }
            return output;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new SpiceRunException("The ngspice run was interrupted.", interrupted);
        } catch (ExecutionException | TimeoutException failed) {
            process.destroyForcibly();
            throw new SpiceRunException("ngspice's output could not be read: " + failed.getMessage(), failed);
        }
    }

    private static String readAll(InputStream stream) {
        try (stream) {
            byte[] bytes = stream.readNBytes(MAXIMUM_OUTPUT + 1);
            if (bytes.length > MAXIMUM_OUTPUT) {
                throw new SpiceRunException("ngspice printed more than " + MAXIMUM_OUTPUT / (1024 * 1024) + " MB.");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException failed) {
            throw new SpiceRunException("ngspice's output could not be read: " + failed.getMessage(), failed);
        }
    }

    /** The end of what a failed run printed: ngspice says why last. */
    private static String lastLines(String output) {
        String[] lines = output.strip().split("\\R");
        return String.join(" | ", List.of(lines).subList(Math.max(0, lines.length - 4), lines.length));
    }
}
