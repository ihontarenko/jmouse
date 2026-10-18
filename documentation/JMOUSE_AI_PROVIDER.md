# jmouse-ai-provider — stateless chat requests and provider adapters

> **Agent orientation file.** Code wins over this copy.
> Module: `jmouse-ai-provider` · Package: `org.jmouse.ai.provider` · Java 21.

## What it is

A common chat call and HTTP implementations for provider wire formats. This layer receives the complete message
history for one turn and returns content blocks, a stop reason and token usage. It does not own a conversation,
tool dispatch, a retry queue, billing policy or credential persistence. Embeddings and reranking have their own
contracts in [JMOUSE_AI_EMBEDDINGS.md](JMOUSE_AI_EMBEDDINGS.md) and [JMOUSE_AI_RERANKING.md](JMOUSE_AI_RERANKING.md).

## The map

| Type | Responsibility |
|---|---|
| [ChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatModel.java) | `providerName()` and `converse(ChatRequest)` caller contract |
| [ChatRequest](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatRequest.java) / [ChatResponse](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatResponse.java) | System text, messages and tool schema in; original blocks, normalized stop reason and usage out |
| [ContentBlock](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ContentBlock.java) | Typed access to canonical text/tool/image blocks while retaining their map representation |
| [HttpChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/HttpChatModel.java) | Shared HTTP execution, bounded response consumption and failure handling |
| [AnthropicChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/AnthropicChatModel.java) | Messages API mapping |
| [OpenAiChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/OpenAiChatModel.java) | Chat Completions mapping, also used for configured compatible providers |
| [GatewayChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/GatewayChatModel.java) | An endpoint speaking the library's canonical request/response shape |
| [ProviderSettings](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ProviderSettings.java) / [ProviderSettingsSource](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ProviderSettingsSource.java) | Immutable settings snapshot and live settings read seam |
| [ProviderCatalog](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ProviderCatalog.java) | Shipped provider metadata, capability declarations and chat adapter factories |
| [RoutingChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/RoutingChatModel.java) | Select a reused adapter from the currently active settings |
| [ChatHttpPolicy](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatHttpPolicy.java) / [ChatResponseLimitException](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatResponseLimitException.java) | Positive connect/response deadlines and maximum response bytes; typed early cancellation |
| [ProviderException](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ProviderException.java) / [StopReason](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/StopReason.java) / [TokenUsage](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/TokenUsage.java) | Provider failures, normalized continuation decision and reported consumption |

## Wiring one model

```java
ProviderSettingsSource source = () -> ProviderSettings.of(
        "ollama", "installed-model", null, 2048);
ChatModel model = RoutingChatModel.overShippedProviders(source);
```

`ProviderSettings.of` uses the legacy CHAT-only constructor. The full record constructor requires an explicit
nonempty capability set and nonnegative revision and also carries configuration provenance. `withApiUrl(...)`
selects an endpoint explicitly; `requireCapability(...)` refuses a mismatched use before the adapter can proceed.
`ProviderCatalog.modelFor(...)` returns empty for providers without CHAT support even when they are shipped for
another capability. Factory overloads accept HTTP deadlines or a complete `ChatHttpPolicy`.

The caller builds a `ChatRequest` with its whole transcript and calls `converse`. Append the returned raw content
blocks to preserve provider data the current application does not interpret. Use `StopReason`/`wantsTools()`
rather than comparing vendor-specific reason strings. The conversation module supplies the loop.

## Configuration and lifecycle

`ProviderSettingsSource` is read during calls so key rotation and model changes can take effect without rebuilding
the application. `RoutingChatModel` keeps delegates rather than allocating a new HTTP client every turn.
Credentials belong in the source's secure configuration; `ProviderSettings.toString()` deliberately omits them.
The catalogue is metadata about installed support, not proof that an arbitrary endpoint speaks the selected wire
format or that a server has the named model installed.

HTTP ceilings count response bytes before JSON decoding. A cancelled or timed-out HTTP exchange can still have
consumed provider usage. Do not automatically replay paid or tool-producing calls merely because their local
answer was lost. A provider failure escapes as `ProviderException`; tool failures belong to the dispatcher and
are a different layer. The module has no Spring property binding; use
[JMOUSE_AI_SPRING_BOOT.md](JMOUSE_AI_SPRING_BOOT.md) or construct the model directly.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-ai-provider/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-ai-model` | compile | no |
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-http` | compile | no |
| `com.fasterxml.jackson.core:jackson-databind` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.ai.provider`

| Type | Responsibility / contract |
|---|---|
| [AnthropicChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/AnthropicChatModel.java) | The Messages API, which the canonical types already look like. |
| [ChatHttpPolicy](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatHttpPolicy.java) | Immutable HTTP admission policy. Response bytes count all delivered body bytes before decoding/JSON parsing. |
| [ChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatModel.java) | A language model, as the thing calling it holds it. |
| [ChatRequest](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatRequest.java) | One turn, sent whole. |
| [ChatResponse](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatResponse.java) | What came back from one turn. |
| [ChatResponseLimitException](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ChatResponseLimitException.java) | The response was cancelled before body decoding. The remote operation/usage may already have happened. |
| [ContentBlock](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ContentBlock.java) | One piece of what a model said, read without casting. |
| [GatewayChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/GatewayChatModel.java) | A model that lives behind somebody else's HTTP endpoint. |
| [HttpChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/HttpChatModel.java) | Everything that is the same about talking to any of them over HTTP. |
| [OpenAiChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/OpenAiChatModel.java) | The Chat Completions API — the translation that proves the canonical shape is canonical. |
| [ProviderCatalog](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ProviderCatalog.java) | Which providers this library ships, in one place. |
| [ProviderException](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ProviderException.java) | A call to a language model that did not come back with an answer. |
| [ProviderSettings](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ProviderSettings.java) | One call's immutable settings snapshot. Credentials must never enter diagnostics. |
| [ProviderSettingsSource](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/ProviderSettingsSource.java) | Where the settings come from, asked once per call. |
| [RoutingChatModel](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/RoutingChatModel.java) | Whichever provider the settings in force name, asked afresh on every turn. |
| [StopReason](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/StopReason.java) | Why the model stopped talking. |
| [TokenUsage](../jmouse-ai-provider/src/main/java/org/jmouse/ai/provider/TokenUsage.java) | What one turn cost. |

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [BoundedChatResponseTest.java](../jmouse-ai-provider/src/test/java/org/jmouse/ai/provider/BoundedChatResponseTest.java).
- [ProviderArchitectureTest.java](../jmouse-ai-provider/src/test/java/org/jmouse/ai/provider/ProviderArchitectureTest.java).
- [ProviderCatalogTest.java](../jmouse-ai-provider/src/test/java/org/jmouse/ai/provider/ProviderCatalogTest.java).
