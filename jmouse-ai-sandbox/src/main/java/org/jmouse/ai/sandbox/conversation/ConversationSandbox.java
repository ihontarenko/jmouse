package org.jmouse.ai.sandbox.conversation;

import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.ToolOutcome;
import org.jmouse.ai.conversation.ConversationBudget;
import org.jmouse.ai.conversation.ConversationRequest;
import org.jmouse.ai.conversation.ConversationResult;
import org.jmouse.ai.conversation.ConversationRunner;
import org.jmouse.ai.provider.ChatRequest;
import org.jmouse.ai.sandbox.SandboxCallers;
import org.jmouse.ai.sandbox.SandboxWorkshop;

import java.util.List;
import java.util.Map;

/**
 * The second entry point: the same tools, now reached by a model instead of by hand.
 *
 * <p>What this is here to show is that <strong>nothing changes</strong>. The dispatcher is the same
 * object, the guards are the same guards, the caller is the same caller, and a call the first entry
 * point refuses is refused identically here — with the identical sentence, because the sentence is
 * written once in {@code jmouse-ai} and both transports read it.
 *
 * <p>Driven by a script rather than a model, for the reason on {@link ScriptedChatModel}: a real model
 * cannot be made to ask for the awkward thing on purpose, and the awkward things are the point.
 *
 * <p>⚠️ Its own {@link SandboxWorkshop}, so its counts are its own. Sharing one with
 * {@code SandboxApplication} would mean this file's numbers depended on how far the other one had got.
 */
public final class ConversationSandbox {

    private static final String RULE = "-".repeat(100);

    private final SandboxWorkshop workshop  = new SandboxWorkshop();
    private final ToolDispatcher dispatcher = workshop.dispatcher();

    public static void main(String[] arguments) {
        new ConversationSandbox().run();
    }

    private ConversationSandbox() {
        workshop.callers().actAs(SandboxCallers.ASSISTANT_FOR_OWNER);
    }

    private void run() {
        aTurnThatCallsTwoToolsAndFinishes();
        aToolCallThatIsRefused();
        aConversationThatDoesNotStop();
        theSameCallBothWays();
    }

    // ── The ordinary shape ───────────────────────────────────────────────────────

    private void aTurnThatCallsTwoToolsAndFinishes() {
        heading("A turn that calls two tools, then finishes",
                "Two calls in one turn, both dispatched, both answered on one user turn - and the "
              + "assistant turn appended above them, or the model has no idea what it is reading.");

        ScriptedChatModel model = new ScriptedChatModel(List.of(
                ScriptedChatModel.Turn.calling("Let me look at both shelves.",
                        ScriptedChatModel.Turn.call("call-1", "parts_list", Map.of("shelf", "A")),
                        ScriptedChatModel.Turn.call("call-2", "parts_list", Map.of("shelf", "B"))),
                ScriptedChatModel.Turn.saying("Shelf A has the fasteners; shelf B has the bearings.")));

        report(runnerOver(model).run(ConversationRequest.opening("What is on each shelf?")), model);
    }

    // ── A refusal is a result, not an exception ──────────────────────────────────

    private void aToolCallThatIsRefused() {
        heading("A tool call that is refused",
                "The conversation does not end. The model is handed the refusal, marked as an error, "
              + "in the same words a Model Context Protocol client would read - and carries on.");

        ScriptedChatModel model = new ScriptedChatModel(List.of(
                ScriptedChatModel.Turn.calling("Clearing shelf Z.",
                        ScriptedChatModel.Turn.call("call-1", "parts_discard", Map.of("shelf", "Z"))),
                ScriptedChatModel.Turn.saying("There is nothing on shelf Z, so I have not removed anything.")));

        ConversationResult answer = runnerOver(model).run(
                ConversationRequest.opening("Clear shelf Z."));

        report(answer, model);
        System.out.println("    what the model was handed back:");
        System.out.println("      " + toolResultsIn(model.lastRequest()));
    }

    // ── The budget ───────────────────────────────────────────────────────────────

    private void aConversationThatDoesNotStop() {
        heading("A conversation that does not stop",
                "A model that keeps asking for tools forever, against a budget of three rounds. It "
              + "ends with a stated reason rather than an exception carrying a magic number - and the "
              + "last round's results are in the conversation, unsent, so it can be continued.");

        ScriptedChatModel model = new ScriptedChatModel(
                List.of(),
                ScriptedChatModel.Turn.calling("Still looking.",
                        ScriptedChatModel.Turn.call("call-n", "workshops_list", Map.of())));

        ConversationRunner runner = new ConversationRunner(
                model, dispatcher, ConversationBudget.defaults().withMaximumRounds(3));

        ConversationResult answer = runner.run(ConversationRequest.opening("Find everything."));

        report(answer, model);
        System.out.println("    what a caller shows instead of an answer:");
        System.out.println("      " + answer.describe());
    }

    // ── The comparison the whole sandbox exists for ──────────────────────────────

    private void theSameCallBothWays() {
        heading("The same call, both ways",
                "One action, reached by hand and reached by a model. Same dispatcher, same guards, "
              + "same caller - so the two answers have to be the same answer, or something is wrong "
              + "in one of the two paths rather than in the tool.");

        ToolOutcome byHand = dispatcher.dispatch("parts_list", Map.of("shelf", "A"));

        ScriptedChatModel model = new ScriptedChatModel(List.of(
                ScriptedChatModel.Turn.calling("Checking.",
                        ScriptedChatModel.Turn.call("call-1", "parts_list", Map.of("shelf", "A"))),
                ScriptedChatModel.Turn.saying("Done.")));

        runnerOver(model).run(ConversationRequest.opening("What is on shelf A?"));

        String throughTheModel = toolResultsIn(model.lastRequest());
        String throughTheHand  = byHand.asStructuredContent().toString();

        System.out.println("    by hand        " + throughTheHand);
        System.out.println("    through a model " + throughTheModel);
        System.out.println();
        System.out.println("    the tool result the model was given contains the same records: "
                         + throughTheModel.contains("M3 bolt"));
    }

    // ── Plumbing ─────────────────────────────────────────────────────────────────

    private ConversationRunner runnerOver(ScriptedChatModel model) {
        return new ConversationRunner(model, dispatcher);
    }

    private void report(ConversationResult answer, ScriptedChatModel model) {
        System.out.println("    ending      " + answer.ending());
        System.out.println("    rounds      " + answer.rounds() + " (the model was asked "
                                              + model.asked().size() + " time(s))");
        System.out.println("    tool calls  " + answer.toolCalls());
        System.out.println("    tokens      " + answer.usage().totalTokens());
        System.out.println("    text        " + (answer.text().isBlank() ? "(none)" : answer.text()));
        System.out.println("    tools shown " + model.asked().getFirst().tools().size());
    }

    /** The last user turn of a conversation, which is where a round's tool results end up. */
    private String toolResultsIn(ChatRequest request) {
        return request.messages().getLast().toString();
    }

    private void heading(String title, String claim) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("  " + title.toUpperCase());
        System.out.println("    " + claim);
        System.out.println(RULE);
    }
}
