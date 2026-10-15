package org.jmouse.script;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopeValues;
import org.jmouse.script.host.BoundScript;
import org.jmouse.script.spi.ScriptStage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dispatches that remember: one evaluation context per script, kept for the session's lifetime.
 *
 * <p>{@link ScriptRuntime#dispatch} builds a context per handler run and drops it, so nothing a script
 * assigns outlives the event. A session keeps each script's context instead, which is what lets a host
 * judging many inputs in one run — every file of a folder, say — give its scripts a memory:</p>
 *
 * <pre>{@code
 * on scanRule when true do
 *     taken++
 *     if taken > 3 then @decision.refuse('Only the first three are taken') end
 * end
 * }</pre>
 *
 * <p>Dispatch itself is unchanged — the same documents, the same {@link AmendmentScope}, the same
 * budget per dispatch — and the stage's values are written into the context afresh every time, so a
 * variable can never be mistaken for the event's own data.</p>
 *
 * <h2>Persisting a session</h2>
 *
 * <p>{@link #variables()} exports what scripts created at their root scope, by script name, keeping only
 * numbers, strings and booleans: never a stage value, a declared function or anything else that is not
 * plain data. {@link ScriptRuntime#session(Map)} restores them, so a host can store a session beside its
 * own checkpoint and resume it without a counter starting over.</p>
 *
 * <p>⚠️ A session is not thread-safe; it belongs to the one run that opened it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ScriptSession {

    private final ScriptRuntime                    runtime;
    private final Map<String, Map<String, Object>> restored;
    private final Map<String, EvaluationContext>   contexts   = new HashMap<>();
    private final Set<String>                      stageNames = new HashSet<>();

    ScriptSession(ScriptRuntime runtime, Map<String, Map<String, Object>> restored) {
        this.runtime = runtime;
        this.restored = restored == null ? Map.of() : Map.copyOf(restored);
    }

    /**
     * Runs every rule assigned to one stage, each in its own remembered context.
     *
     * @see ScriptRuntime#dispatch(ScriptStage, String, Map)
     */
    public int dispatch(ScriptStage<?> stage, String scope, Map<String, Object> values) {
        stageNames.addAll(values.keySet());
        return runtime.dispatch(stage, scope, values, this::contextFor);
    }

    /**
     * Runs one named document at a stage, in its remembered context.
     *
     * @see ScriptRuntime#dispatchOne(ScriptStage, String, String, Map)
     */
    public int dispatchOne(ScriptStage<?> stage, String scope, String documentName, Map<String, Object> values) {
        stageNames.addAll(values.keySet());
        return runtime.dispatchOne(stage, scope, documentName, values, this::contextFor);
    }

    /**
     * The plain data scripts have created so far, by script name and then by variable name — including
     * whatever was restored for a script that has not run again yet.
     *
     * @return an unmodifiable snapshot
     */
    public Map<String, Map<String, Object>> variables() {
        Map<String, Map<String, Object>> variables = new LinkedHashMap<>(restored);

        contexts.forEach((script, context) -> variables.put(script, exported(context)));

        return Map.copyOf(variables);
    }

    private EvaluationContext contextFor(BoundScript script) {
        return contexts.computeIfAbsent(script.getName(), name -> {
            EvaluationContext context = runtime.getHost().newContext(script);

            restored.getOrDefault(name, Map.of()).forEach((variable, value) -> {
                if (isPlainData(value)) {
                    context.setValue(variable, value);
                }
            });

            return context;
        });
    }

    private Map<String, Object> exported(EvaluationContext context) {
        Map<String, Object> variables = new LinkedHashMap<>();
        ScopeValues         root      = context.getScopedChain().peek();

        root.getValues().forEach((name, value) -> {
            if (!stageNames.contains(name) && isPlainData(value)) {
                variables.put(name, value);
            }
        });

        return Map.copyOf(variables);
    }

    private static boolean isPlainData(Object value) {
        return value instanceof Number || value instanceof String || value instanceof Boolean;
    }

}
