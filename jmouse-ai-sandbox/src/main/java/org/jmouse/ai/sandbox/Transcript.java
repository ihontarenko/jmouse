package org.jmouse.ai.sandbox;

import org.jmouse.ai.CallVerdict;
import org.jmouse.ai.RefusalRendering;
import org.jmouse.ai.ToolOutcome;
import org.jmouse.ai.ToolRefusedException;

import java.util.List;
import java.util.Map;

/**
 * What the run says it did.
 *
 * <p>Formatting kept out of {@link SandboxApplication} so that the scenarios read as the sequence of
 * calls they are. Everything here is plain ASCII: the library's own sentences carry em dashes and
 * arrows of their own, and a driver that added more would be unreadable on the first console that does
 * not speak UTF-8.
 *
 * <p>⚠️ Prints rather than logs, deliberately. A transcript interleaved with the library's INFO lines
 * is neither, and the library's logging is exercised by the fact that it happens — not by being read
 * here.
 */
public final class Transcript {

    private static final String RULE  = "-".repeat(100);
    private static final String INDENT = "        ";

    private int scenarioNumber;

    /**
     * @param claim what this scenario is here to prove — written down because a scenario whose point is
     *              only in somebody's head is one nobody can tell has stopped proving it
     */
    public void scenario(String title, String claim) {
        System.out.println();
        System.out.println(RULE);
        System.out.printf("%02d  %s%n", ++scenarioNumber, title.toUpperCase());
        System.out.println("    " + claim);
        System.out.println(RULE);
    }

    public void heading(String title) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("    " + title.toUpperCase());
        System.out.println(RULE);
    }

    public void calling(String caller, String publishedName, Map<String, Object> arguments) {
        System.out.println();
        System.out.println("  > " + caller + " calls " + publishedName + " " + arguments);
    }

    public void ran(ToolOutcome outcome) {
        System.out.println("    " + verdictOf(outcome) + "  " + outcome.describe());
        System.out.println(INDENT + outcome.payload());
    }

    public void refused(ToolRefusedException refusal) {
        System.out.println("    REFUSED");
        System.out.println(INDENT + RefusalRendering.render(refusal));
    }

    public void note(String line) {
        System.out.println("    " + line);
    }

    public void trail(List<RecordingTrace.Entry> entries) {
        System.out.printf("    %-24s %-16s %-16s %-22s %s%n",
                "CALLER", "SCOPE", "ACTION", "OUTCOME", "DETAIL");

        entries.forEach(entry -> System.out.printf("    %-24s %-16s %-16s %-22s %s%n",
                entry.caller(), entry.scope(), entry.action(), entry.outcome(), entry.detail()));
    }

    public void totals(Map<String, Long> outcomeTotals) {
        System.out.println();
        outcomeTotals.forEach((outcome, count) -> System.out.printf("    %-22s %d%n", outcome, count));
    }

    /**
     * Two of the three verdicts mean nothing happened, so the word is what a reader scans for.
     *
     * <p>The same reason {@link ToolOutcome#describe()} leads with it: a preview and a suppressed
     * duplicate both come back as ordinary successful results.
     */
    private String verdictOf(ToolOutcome outcome) {
        return outcome.verdict() == CallVerdict.CARRIED_OUT
                ? "DONE   "
                : outcome.verdict().name();
    }
}
