package org.jmouse.el;

import org.jmouse.el.extension.FunctionContainer;
import org.jmouse.el.extension.Function;
import java.util.List;
import java.util.Set;
import org.jmouse.el.extension.function.MaxFunction;
import org.jmouse.el.extension.function.MinFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtensionSnapshotTest {

    @Test
    void registrationAndRemovalPublishNewImmutableSnapshots() {
        FunctionContainer functions = new FunctionContainer();
        MinFunction minimum = new MinFunction();
        MaxFunction maximum = new MaxFunction();
        functions.register(minimum);
        List<Function> first = functions.values();
        Set<String> firstKeys = functions.keys();
        functions.register(maximum);

        assertEquals(1, first.size());
        assertEquals(1, firstKeys.size());
        assertEquals(2, functions.values().size());
        assertThrows(UnsupportedOperationException.class, () -> functions.values().clear());
        assertThrows(UnsupportedOperationException.class, () -> functions.keys().clear());

        functions.remove(minimum);
        assertEquals(List.of(maximum), functions.values());
        assertSame(maximum, functions.get("max"));
    }
}
