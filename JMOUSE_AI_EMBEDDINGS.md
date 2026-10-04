# jmouse-ai-embeddings

Reusable vectorization and token-count capability, separate from chat conversations and tool dispatch. It depends on provider settings and Jackson for the wire; it has no Spring/JPA, product documents, queue, index, pgvector or deprecated common dependency.

## Used contracts

- EmbeddingModel: typed input list and expected native dimensions; immutable ordered vectors and nullable actual input-token usage.
- EmbeddingModels: open installed-adapter registry, validating protocol/provider support independently of declared model capabilities.
- TokenCounter: count planned inputs without generating vectors; EXACT, APPROXIMATE and UNKNOWN are explicit.
- EmbeddingTransport: injectable JSON exchange; JdkEmbeddingTransport enforces the whole response deadline, byte ceiling and no redirects, with a reusable client that the adopter closes.
- CallLimits: explicit per-input/batch Unicode, input-count, timeout and response-byte ceilings.

HttpEmbeddingModel composes EmbeddingProtocol with EmbeddingTransport. OpenAiEmbeddingProtocol, OllamaEmbeddingProtocol, TeiEmbeddingProtocol and VoyageEmbeddingProtocol supply the installed wire strategies. Settings must declare EMBEDDINGS. All use an explicit operation endpoint: chat-completions addresses are not guessed or rewritten into embeddings addresses. OPENAI requires a key; local Ollama/TEI can omit it. Protocol responses must have the expected count, finite coordinates and dimensions. OpenAI indexes restore original input order and duplicates/missing entries are refused; OpenAI/Ollama response model identifiers must match the configured model. Dimensions are an expected native size, not automatically sent as a vendor-specific projection parameter. TEI/Ollama explicitly disable truncation. TEI native vectors report usage unknown unless a later adapter supplies it; zero is never substituted for absent usage.

ManagedTokenCounter sends one TEI /tokenize request per batch with add_special_tokens=true and counts the nested token lists, preserving input order. Its configured tokenizer must match the configured model and endpoint must have the same origin as embeddings, preventing credential forwarding to another tokenizer host. EXACT describes the returned tokenizer count for supplied inputs, not proof of the remote model revision or a future invoice. Tokenizer identity/revision are explicit operator declarations: pin the server model and tokenizer, keep prompt behavior consistent with embedding execution, and configure any prefixes in the adopter's input policy. Model-server attestation is not implemented.

CODE_POINT_ESTIMATE uses an explicit positive finite ratio and per-input overhead, always APPROXIMATE. UNKNOWN returns nullable per-input counts without any network call. No universal characters/4 rule is presented as exact. Input prefixes, overlap, cache/reuse selection and pricing are the adopter's responsibilities. Actual usage is separate from predictions.

EmbeddingException exposes typed reasons, safe HTTP status and retryability, never response bodies, corpus text or keys. No automatic retries/concurrency scheduling: these belong to the adopter's bounded worker policy, which must account for repeated-call cost and cancellation. HTTP call interruption preserves the interrupt flag.

## Voyage retrieval roles

Request accepts typed InputType.UNSPECIFIED, DOCUMENT or QUERY, retaining the existing two-argument constructor as UNSPECIFIED. EmbeddingModel.supportsInputType is a default contract so existing implementations remain compatible; unsupported non-default roles are refused rather than silently ignored. VOYAGE sends input_type only for an explicit role, truncation=false and output_dtype=float. Native expected dimensions are validated without implicitly projecting vectors. Response data indexes restore order; missing, duplicate or invalid entries are refused. Voyage usage.total_tokens is actual reported input usage; absent usage stays null. The response model is optional for Voyage, but a conflicting reported model is refused. Voyage is advertised as EMBEDDINGS only and cannot resolve as a ChatModel.

New endpoint/model/key registrations for installed provider protocols need configuration only; a vendor with a different wire contract needs an installed adapter/catalog entry. This module has no product source persistence or key store.

## Verification and wire references

EmbeddingProtocolTest uses a real loopback HTTP fixture to check Ukrainian/emoji inputs, vector ordering/immutability, capability refusal before I/O, tokenization without vector calls, unknown/estimate behavior, malformed responses, response ceilings and redirects. This verifies protocols, not retrieval quality against downloaded weights or an external paid account.

VoyageProtocolTest validates retrieval roles, request options, indexed responses, usage and malformed responses without external requests. RAGcoon separately verified a real Voyage account with a bounded short corpus; that does not benchmark retrieval quality.

Verified primary references:

- [Voyage embeddings API](https://docs.voyageai.com/reference/embeddings-api)
- [Voyage authentication](https://docs.voyageai.com/docs/api-key-and-installation)
- [OpenAI embeddings API](https://developers.openai.com/api/reference/resources/embeddings/methods/create)
- [Ollama embed API](https://docs.ollama.com/api/embed)
- [TEI request/response types](https://github.com/huggingface/text-embeddings-inference/blob/main/router/src/http/types.rs)
- [TEI server endpoints](https://github.com/huggingface/text-embeddings-inference/blob/main/router/src/http/server.rs)

Build this isolated version after model/provider modules: mvn -o -f jmouse-ai-embeddings/pom.xml -Dgpg.skip=true -Dmaven.javadoc.skip=true install. Use artifact org.jmouse:jmouse-ai-embeddings:1.0.0-b20261004-embedding-spi with jmouse-ai-provider:1.0.0-b20261004-voyage; AI/model/JPA remain at 1.0.0-b20261004-ai-capabilities when adopting database management. It does not require upgrading unrelated jMouse modules.

## Open protocol architecture (2026-10-04)

The decomposition follows the HttpChatModel principle: shared checked execution, provider-owned authentication/request/response translation. Embedding DTOs/capability differ from chat, so the embedding execution composes an injected protocol and transport rather than inheriting ChatRequest/ChatResponse or changing conversation code.

- EmbeddingModel is the caller contract: input text/role/native dimension expectation in, immutable ordered vectors/nullable reported usage out.
- EmbeddingProtocol is the open wire SPI: code, provider support, credential requirement, headers, encode and decode. It owns no network client, scheduling, key store or persistence.
- AbstractEmbeddingProtocol holds immutable identity/provider/credential sets and exports protected indexedResponse decoding for compatible external adapters.
- HttpEmbeddingModel validates capability/settings/role/limits, performs one injected bounded exchange and validates the final decoded result. No provider-name switch or closed protocol enum appears in this execution path.
- EmbeddingResponseReader is internal checked JSON decoding. It rejects wrong types, duplicate/out-of-range indexes, model conflicts, incorrect count/dimensions, nonfinite coordinates and malformed usage with typed safe errors; it does not broadly catch programming exceptions.
- JdkEmbeddingTransport owns HTTP/JSON/deadline/body-byte/redirect behavior. Protocol headers allow different authentication schemes. Existing four-argument transports retain their functional contract; the header-aware default overload refuses unsupported custom headers before I/O. Header validation errors redact credentials.
- ManagedTokenCounter separately orchestrates its existing declared token strategies, using checked response decoding with no dependency on the compatibility facade. Additional tokenizers implement TokenCounter.

For a future Jina integration, install its EmbeddingProtocol and register new HttpEmbeddingModel(protocol, transport) with the existing EmbeddingModels collection/DI. Shared execution, validation and a central protocol enum remain untouched. If the wire contract is exactly OpenAI-compatible, bind OpenAiEmbeddingProtocol with an explicit registry code/provider set/credential subset. Wire compatibility must be verified, not inferred from a vendor name. The shared provider management catalogue still needs the new provider metadata/capability registration; this refactor does not claim arbitrary vendor names are automatically admitted.

Seven library tests cover existing HTTP formats through the public execution contract, Voyage roles/usage, independently registered wire formats, cross-package protocol installation/reused decoding, malformed responses and custom authentication over real loopback HTTP. Existing request/response contracts remain available; earlier artifacts are not overwritten. RAGcoon uses the new composed models and opts in only to the new embeddings artifact. No external model quota is required for these checks.
