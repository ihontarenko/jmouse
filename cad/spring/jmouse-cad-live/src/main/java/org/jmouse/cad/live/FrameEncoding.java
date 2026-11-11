package org.jmouse.cad.live;

import org.jmouse.cad.simulation.engine.LiveCircuitSession;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A frame of samples as the socket sends it: binary. A frame is thousands of numbers, and writing each one out as
 * decimal text took two thirds of a live circuit's time — more than solving it. Little-endian:
 *
 * <pre>
 * int32    header length in bytes
 * bytes    header, UTF-8 JSON: { type: "frame" or "capture", channels: [names in order], channelUnits: [each one's unit, "V", "A"
 *                                    or "W"], levels: { node: volts }, load, played,
 *                                    parts: { part: [watts, °C, { terminal: amperes into it }, °C maximum, W rating,
 *                                                    "open" | "short" | null, { indicator: figure }] },
 *                                    trace: [column names in order: a node, a part, a part's pin],
 *                                    traceUnits: [each one's unit, "V" or "A"], peaks: [channels with peaks] }
 * …        zero padding to a multiple of 8
 * float64  the first sample's time;  float64 the time between samples;  int32 sample count;  int32 unused
 * float32  each channel's readings, count of them, in the header's order
 * float32  for each channel named in `peaks`: its lowest reading over each sample's interval, its highest, its mean
 * …        zero padding to a multiple of 8
 * int32    traced moment count;  int32 unused
 * float64  each moment's time — single precision cannot tell a microsecond apart a minute into a run
 * float32  each trace column's readings at those moments, in the header's order — on a trace that keeps one
 *          sample in several, each moment's mean over the stretch it stands for
 * float32  each trace column's RMS over that same stretch, in the same order
 * </pre>
 *
 * Samples are evenly spaced, so their times are the first and the step, not a column. Readings go as single precision:
 * seven digits is more than a screen or a CSV of a bench measurement shows.
 */
final class FrameEncoding {

    private FrameEncoding() {
    }

    /**
     * @param type     "frame", a frame of the run; "capture", a stretch replayed finely
     * @param interval the time between samples
     * @param load     the share of the frame's wall time the server spent computing it
     * @param played   the speed the server plays at: the one asked for, or the one it holds while it cannot keep up
     */
    static ByteBuffer encode(String type, LiveCircuitSession.Frame frame, double interval, double load, double played,
                             ObjectMapper mapper) {
        List<String> channels = List.copyOf(frame.channels().keySet());
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("type", type);
        header.put("channels", channels);
        header.put("channelUnits", channels.stream().map(channel -> frame.units().getOrDefault(channel, "V")).toList());
        header.put("levels", frame.levels());
        header.put("load", load);
        header.put("played", played);
        header.put("parts", parts(frame));
        List<String> traced = List.copyOf(frame.trace().columns().keySet());
        header.put("trace", traced);
        header.put("traceUnits", traced.stream().map(column -> frame.trace().units().getOrDefault(column, "")).toList());
        // Only channels whose peaks were read, and only when each has one per sample: a joined frame can lack them.
        List<String> peaked = channels.stream().filter(channel -> frame.peaks().containsKey(channel)
                && frame.peaks().get(channel).lows().length == frame.time().length).toList();
        header.put("peaks", peaked);
        byte[] headerBytes = mapper.writeValueAsString(header).getBytes(StandardCharsets.UTF_8);
        int count = frame.time().length;
        int samplesAt = align(Integer.BYTES + headerBytes.length);
        int moments = frame.trace().time().length;
        int traceAt = align(samplesAt + 2 * Double.BYTES + 2 * Integer.BYTES + (channels.size() + 3 * peaked.size()) * count * Float.BYTES);
        ByteBuffer buffer = ByteBuffer.allocate(traceAt + 2 * Integer.BYTES + moments * Double.BYTES + 2 * traced.size() * moments * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(headerBytes.length).put(headerBytes).position(samplesAt);
        buffer.putDouble(count > 0 ? frame.time()[0] : 0).putDouble(interval).putInt(count).putInt(0);
        for (String channel : channels) {
            for (double reading : frame.channels().get(channel)) {
                buffer.putFloat((float) reading);
            }
        }
        for (String channel : peaked) {
            for (double low : frame.peaks().get(channel).lows()) {
                buffer.putFloat((float) low);
            }
            for (double high : frame.peaks().get(channel).highs()) {
                buffer.putFloat((float) high);
            }
            for (double mean : frame.peaks().get(channel).means()) {
                buffer.putFloat((float) mean);
            }
        }
        buffer.position(traceAt).putInt(moments).putInt(0);
        for (double time : frame.trace().time()) {
            buffer.putDouble(time);
        }
        for (String column : traced) {
            for (double reading : frame.trace().columns().get(column)) {
                buffer.putFloat((float) reading);
            }
        }
        for (String column : traced) {
            double[] rms = frame.trace().rms().get(column);
            for (int moment = 0; moment < moments; moment++) {
                buffer.putFloat(rms == null ? Float.NaN : (float) rms[moment]);
            }
        }
        return buffer.flip();
    }

    /**
     * Each part as [watts, °C, { terminal: amperes into it }, maximum °C, rating W, failure, displays, cause, past,
     * pictures] — a number that is not known as null; {@code past} is the limit it is past now ({@link PartLoad#past});
     * {@code pictures} each picture's bytes in hexadecimal, by figure.
     */
    private static Map<String, List<Object>> parts(LiveCircuitSession.Frame frame) {
        Map<String, List<Object>> parts = new LinkedHashMap<>();
        frame.parts().forEach((part, load) -> parts.put(part, java.util.Arrays.asList(known(load.power()),
                known(load.temperature()), load.terminals(), known(load.maximum()), known(load.rating()), load.failure(),
                load.displays(), load.cause(), load.past(), load.pictures())));
        return parts;
    }

    private static Double known(double value) {
        return Double.isFinite(value) ? value : null;
    }

    private static int align(int position) {
        return (position + 7) & ~7;
    }
}
