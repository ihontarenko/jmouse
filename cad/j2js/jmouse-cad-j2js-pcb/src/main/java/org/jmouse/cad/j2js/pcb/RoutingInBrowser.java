package org.jmouse.cad.j2js.pcb;

import org.jmouse.cad.pcb.routing.RoutingEngine;
import org.jmouse.core.access.TypedValue;
import org.jmouse.cad.pcb.protocol.RoutingBoardJson;
import org.jmouse.cad.pcb.protocol.RoutingGesture;
import org.jmouse.cad.pcb.protocol.RoutingRefused;
import org.jmouse.j2js.Export;
import org.jmouse.json.JsonMapper;
import org.jmouse.json.JsonMappingException;
import org.jmouse.json.JsonSyntaxException;
import org.jmouse.json.reading.ChangedPaths;
import org.jmouse.json.reading.JsonTreeAccess;
import org.jmouse.json.reading.JsonTreePatch;
import org.jmouse.json.reading.StructuralReading;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🛤 The router in a page: a board the server worked out is opened here, and every gesture after is answered here, by the same
 * protocol the server's routes answer — so an editor that asks it, rather than the server, is answered alike, with no request.
 *
 * <p>A gesture's layouts — the pcb as it stands, before and after a part carried — are kept here by slot, as the server's live
 * session keeps them: sent whole once, then named by their slot alone while they stand unchanged — a drag's every frame asks of
 * the same pcb, and reading it again is most of what a frame costs —, or sent as the change made to it, a JSON Patch: applied to
 * the tree kept, and only what it reached read again ({@link StructuralReading}). A slot not held, or a change that does not fit
 * it, is answered {@code layout-unknown}, and the editor sends it whole.</p>
 *
 * <p>An answer is JSON: {@code {"answer": …}}, the gesture's answer as the server writes it; or {@code {"refusal": {"status",
 * "code", "detail"}}}, as the server writes a problem. A few boards are kept, the least recently asked of let go first, as the
 * server keeps them.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RoutingInBrowser {

    /** How many boards are kept open. */
    private static final int KEPT_BOARDS = 8;

    private static final JsonMapper JSON = RoutingBoardJson.mapper();

    private static final Map<String, RoutingEngine> BOARDS = new LinkedHashMap<>(KEPT_BOARDS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, RoutingEngine> eldest) {
            return size() > KEPT_BOARDS;
        }
    };

    /** How many layouts are kept read, by slot: a board's pcb, and before and after a carry, for a few boards. */
    private static final int KEPT_LAYOUTS = 32;

    /** The member of a layout sent naming its slot. */
    private static final String SLOT = "slot";

    /** The member of a layout sent whole. */
    private static final String WHOLE = "whole";

    /** The member of a layout sent as the change to the one its slot holds: a JSON Patch. */
    private static final String PATCH = "patch";

    /** A layout kept: the tree it was sent as, kept up to date by the patches after, and what was read from it. */
    private record KeptLayout(Object tree, Object read) {
    }

    /** Each layout kept, by its slot. */
    private static final Map<String, KeptLayout> LAYOUTS = new LinkedHashMap<>(KEPT_LAYOUTS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, KeptLayout> eldest) {
            return size() > KEPT_LAYOUTS;
        }
    };

    /** A layout read again only where a patch changed it. */
    private static final StructuralReading<Object> READING = new StructuralReading<>(new JsonTreeAccess(JSON));

    private static int opened;

    private RoutingInBrowser() {
    }

    /** Opens a board — the server's, as {@code GET /pcb/routing/{board}/board} writes it —: the name it is asked of here by. */
    @Export
    public static String open(String board) {
        RoutingEngine engine = new RoutingEngine(RoutingBoardJson.read(board));
        String        name   = "board-" + ++opened;
        BOARDS.put(name, engine);
        return name;
    }

    /**
     * A gesture — {@code leg}, {@code segment}, {@code tune-track} … — asked of an open board: its request as JSON, and its layouts by
     * the field they fill — {@code {"drawing": {"slot": "b/drawing", "whole": {…}}}}, or {@code {"slot": …}} alone for one held here.
     */
    @Export
    public static String ask(String board, String gesture, String request, String layouts) {
        RoutingEngine engine = BOARDS.get(board);

        if (engine == null) {
            return refusal(AskRefusal.FORGOTTEN, "routing board " + board);
        }

        Optional<RoutingGesture> asked = RoutingGesture.of(gesture);

        if (asked.isEmpty()) {
            return refusal(AskRefusal.INVALID, "No gesture " + gesture);
        }

        Object read;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = new LinkedHashMap<>((Map<String, Object>) JSON.read(request));
            Optional<String> unknown = withLayouts(fields, asked.get().request(), layouts);
            if (unknown.isPresent()) {
                return refusal(AskRefusal.LAYOUT_UNKNOWN, "No layout held in slot " + unknown.get());
            }
            read = JSON.convert(fields, TypedValue.of(asked.get().request()));
        } catch (JsonSyntaxException | JsonMappingException | ClassCastException exception) {
            return refusal(AskRefusal.INVALID, exception.getMessage());
        }

        try {
            Map<String, Object> answer = new LinkedHashMap<>();
            answer.put("answer", asked.get().answer(engine, read));
            return JSON.write(answer);
        } catch (RoutingRefused refused) {
            return refusal(AskRefusal.REFUSED, refused.getMessage());
        } catch (RuntimeException failure) {
            return refusal(AskRefusal.FAILED, failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName());
        }
    }

    /**
     * The request's fields with each layout in its field: one sent whole read into the field's own type and kept by its slot, one named
     * by its slot taken as kept — the same object, read once. The slot of a layout not held, where one is not.
     */
    @SuppressWarnings("unchecked")
    private static Optional<String> withLayouts(Map<String, Object> fields, Class<?> request, String layouts) {
        if (layouts == null || layouts.isBlank()) {
            return Optional.empty();
        }
        for (Map.Entry<String, Object> layout : ((Map<String, Object>) JSON.read(layouts)).entrySet()) {
            Map<String, Object> written = (Map<String, Object>) layout.getValue();
            String              slot    = (String) written.get(SLOT);
            Type                type    = fieldType(request, layout.getKey());
            if (written.containsKey(WHOLE)) {
                Object tree = written.get(WHOLE);
                LAYOUTS.put(slot, new KeptLayout(tree, READING.read(null, tree, type, ChangedPaths.none())));
            } else if (written.containsKey(PATCH)) {
                KeptLayout kept = LAYOUTS.get(slot);
                if (kept == null || !patched(slot, kept, (List<?>) written.get(PATCH), type)) {
                    return Optional.of(slot);
                }
            }
            KeptLayout held = LAYOUTS.get(slot);
            if (held == null) {
                return Optional.of(slot);
            }
            fields.put(layout.getKey(), held.read());
        }
        return Optional.empty();
    }

    /**
     * The layout a slot holds changed by a patch, read again where the patch reached; false — the slot let go — where the patch does
     * not fit the tree kept, and the editor sends the layout whole.
     */
    private static boolean patched(String slot, KeptLayout kept, List<?> patch, Type type) {
        try {
            Object tree = JsonTreePatch.applied(kept.tree(), patch);
            LAYOUTS.put(slot, new KeptLayout(tree, READING.read(kept.read(), tree, type, JsonTreePatch.changedBy(patch))));
            return true;
        } catch (JsonMappingException misfit) {
            LAYOUTS.remove(slot);
            return false;
        }
    }

    /** The type of a request's field: the component of that name. */
    private static Type fieldType(Class<?> request, String field) {
        for (RecordComponent component : request.getRecordComponents()) {
            if (component.getName().equals(field)) {
                return component.getGenericType();
            }
        }
        throw new JsonMappingException("A " + request.getSimpleName() + " has no layout " + field);
    }

    /** Lets a board go. */
    @Export
    public static void close(String board) {
        BOARDS.remove(board);
    }

    private static String refusal(AskRefusal refusal, String detail) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("status", refusal.status());
        problem.put("code", refusal.code());
        problem.put("detail", detail);
        return JSON.write(Map.of("refusal", problem));
    }
}
