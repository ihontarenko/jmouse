package org.jmouse.parallax.store.computation;

import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.store.support.ParallaxJson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** A dataset as stored: its JSON, gzip-compressed. Floats compress well, so the cache costs a fraction of the wire. */
public class DatasetCodec {

    public record Encoded(String json, byte[] compressed) {
    }

    private final ParallaxJson documents;

    public DatasetCodec(ParallaxJson documents) {
        this.documents = documents;
    }

    public Encoded encode(Dataset dataset) {
        String json = documents.write(dataset);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(json.length() / 4);
        try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException impossible) {
            throw new UncheckedIOException(impossible);
        }
        return new Encoded(json, buffer.toByteArray());
    }

    public String decode(byte[] compressed) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("A cached dataset is corrupt.", exception);
        }
    }
}
