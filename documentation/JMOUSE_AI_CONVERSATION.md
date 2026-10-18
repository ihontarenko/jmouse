# jmouse-ai-conversation — the bounded model/tool loop

> **Agent orientation file.** The source is authoritative.
> Module: `jmouse-ai-conversation` · Package: `org.jmouse.ai.conversation` · Java 21.

## What it is

The loop that asks a chat model, dispatches its requested tools and asks again. It composes
[jmouse-ai](JMOUSE_AI.md) and [jmouse-ai-provider](JMOUSE_AI_PROVIDER.md). System text and previous messages arrive
from the product; the completed transcript leaves in the result. This is execution, not conversation storage.

## The map

| Type | Responsibility |
|---|---|
| [ConversationRunner](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationRunner.java) | Execute turns and route every tool call through the supplied `ToolDispatcher` |
| [ConversationRequest](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationRequest.java) | Product-provided system text and preceding canonical messages |
| [ConversationBudget](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationBudget.java) | Maximum model rounds and aggregate reported tokens |
| [ConversationEnding](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationEnding.java) | Distinguish completed work from round/token exhaustion |
| [ConversationResult](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationResult.java) | Text, resumable transcript, ending, rounds, usage and tool-call count |
| [ProviderTools](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ProviderTools.java) | Convert published tool metadata to the model's name/description/schema shape |
| [SettingsProviderRegistry](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/SettingsProviderRegistry.java) | Expose provider read metadata from the actual settings source |

## Execution

```java
ConversationRunner runner = new ConversationRunner(model, dispatcher,
        new ConversationBudget(6, 120_000));
ConversationResult result = runner.run(request);
String display = result.describe();
```

`model`, `dispatcher` and `request` are supplied by the adopter. The two-argument runner uses the same default
budget as the example. On entry it copies request messages and obtains `dispatcher.reachable()` once. Each round
sends the growing transcript, records the assistant turn, and either finishes or dispatches each requested tool.
Tool results become a canonical user turn. Returned `ToolImage` bytes travel as sibling image blocks beside the
tool results so provider adapters can carry them in their supported message shape.

## Budget semantics

The check occurs after the requested tools have run and their results have entered the transcript. It bounds
whether another turn starts; it is not a preflight estimate, hard invoice ceiling or per-tool transaction limit.
The model can finish normally on its last permitted round. `maximumTokens` counts the sum of reported input and
output usage across rounds, including repeated transcript input. When both limits have been reached, the round
limit is checked first. The result includes unsent final tool results so a product can resume intentionally.

Always inspect `finished()` or `ending()`. `text()` during an exhausted run may be partial or empty;
`describe()` chooses usable answer text or the library's explicit stopping explanation.

## Identity and failures

There is no caller parameter: the dispatcher's `CallerResolver` supplies identity on the current thread.
The runner neither performs separate permission checks nor calls tool handlers directly. If moving the loop
to a worker, the adopter must establish the identity context the resolver expects there.

Tool refusals and runtime failures are rendered through the shared refusal renderer into error tool results
so the model can react. A domain failure may have partially changed state; a refusal means nothing was attempted.
Failure to reach the model itself escapes as `ProviderException`. The runner adds no retry scheduler, persistence
or domain transaction; use the returned transcript and counters when recording a conversation in the product.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-ai-conversation/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-ai` | compile | no |
| `org.jmouse:jmouse-ai-provider` | compile | no |
| `com.fasterxml.jackson.core:jackson-databind` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.ai.conversation`

| Type | Responsibility / contract |
|---|---|
| [ConversationBudget](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationBudget.java) | How far a conversation is allowed to run before somebody looks at it. |
| [ConversationEnding](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationEnding.java) | Why the loop stopped. |
| [ConversationRequest](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationRequest.java) | What to ask, and what was said before it. |
| [ConversationResult](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationResult.java) | What the loop produced, and what it cost to produce it. |
| [ConversationRunner](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ConversationRunner.java) | Ask, run what was asked for, ask again — and stop. |
| [ProviderTools](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/ProviderTools.java) | The catalogue, as a model is shown it. |
| [SettingsProviderRegistry](../jmouse-ai-conversation/src/main/java/org/jmouse/ai/conversation/SettingsProviderRegistry.java) | The provider read port, answered from the settings the model is actually called with. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [ConversationArchitectureTest.java](../jmouse-ai-conversation/src/test/java/org/jmouse/ai/conversation/ConversationArchitectureTest.java).
