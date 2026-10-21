package org.jmouse.parallax.symbol;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The symbols of our library that are drawn from a count rather than by hand — connectors —, written as jMX documents
 * into their family folders. Committed like a drawn symbol, regenerated, never edited.
 *
 * <pre>{@code
 * java … org.jmouse.parallax.symbol.SymbolLibraryGenerator jmouse-parallax/src/main/resources/catalogue/electronics/symbols
 * }</pre>
 */
public final class SymbolLibraryGenerator {

    private static final String TABLE = "symbol-standards/connector-symbols.sht";

    private SymbolLibraryGenerator() {
    }

    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Name the symbols folder to write into");
        }
        Path root = Path.of(arguments[0]);
        List<ConnectorSymbols.GeneratedSymbol> symbols = new ConnectorSymbols().symbols(read());
        for (ConnectorSymbols.GeneratedSymbol symbol : symbols) {
            Path file = root.resolve(symbol.folder()).resolve(symbol.slug() + ".jmx");
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, symbol.document(), StandardCharsets.UTF_8);
            } catch (IOException unwritable) {
                throw new UncheckedIOException("The symbol " + file + " cannot be written", unwritable);
            }
        }
        System.out.println(symbols.size() + " symbols written under " + root.toAbsolutePath());
    }

    private static Sheet read() {
        try (InputStream stream = SymbolLibraryGenerator.class.getClassLoader().getResourceAsStream(TABLE)) {
            if (stream == null) {
                throw new IllegalArgumentException("The table " + TABLE + " is not on the classpath");
            }
            return SheetReader.read(TABLE, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }
}
