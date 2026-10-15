# jmouse-ai-reranking

Reusable reranking capability: one query and a list of candidate documents go in, the documents' indices
come back best first with the provider's scores. It sits beside `jmouse-ai-embeddings` and is built the
same way — a contract callers use, an open protocol SPI for wire formats, an injectable bounded
transport — and, like it, holds no persistence, routing, retries or product policy. It depends on
provider settings and Jackson; no Spring, JPA or common dependency.

Coordinates: `org.jmouse:jmouse-ai-reranking:1.0.0-b20261005-reranking` (depends on
`jmouse-ai-provider:1.0.0-b20261004-voyage`). Tracked as `JMF-344`.

## Why a module of its own

Reranking is its own capability (`AiCapability.RERANK` already existed), its own wire formats, and its
own result — a scored order, not vectors. Folding it into `jmouse-ai-embeddings` would make every
embeddings adopter carry it and blur that module's contracts; folding it into `jmouse-ai-conversation`
would be wrong in kind. Reusing `jmouse-ai-embeddings`' transport was rejected too: its `CallLimits` and
`EmbeddingException` speak about embeddings, so borrowing them would couple the two modules by
vocabulary. The transport code is therefore repeated here — see *Shared transport* below.

## Used contracts

- `RerankModel` — `rerank(ProviderSettings, Request, RerankLimits)`.
  - `Request(query, documents, topResults)`: nonempty query and documents; `topResults` null asks for
    every document back, otherwise 1…document count.
  - `Response(ranked, inputTokens)`: `Ranked(index, score)` best first; `inputTokens` is the provider's
    reported usage or null — never zero for unknown.
  - Scores are the provider's own scale, comparable only within one response.
- `RerankModels` — immutable open registry of installed models by code, checking provider support.
- `RerankProtocol` / `AbstractRerankProtocol` — wire translation: code, supported providers, which of
  them need a key, headers, `encode`, `decode`.
- `RerankTransport` / `JdkRerankTransport` — bounded JSON POST: whole-response deadline with
  cancellation, response byte ceiling counted while streaming (before JSON parsing), redirects never
  followed, reusable client the adopter closes.
- `RerankLimits` — explicit per-call ceilings: documents, query/document/total Unicode code points,
  timeout, response bytes.
- `RerankException` — typed `Reason` (CONFIGURATION, TIMEOUT, TRANSPORT, PROVIDER_REFUSED,
  INVALID_RESPONSE, RESPONSE_LIMIT), safe HTTP status and `retryable()`; never the query, documents,
  credentials or response body.

`HttpRerankModel` checks before any I/O that the settings declare `RERANK`, the protocol speaks the
provider, a model is named, a required key is present, the limits admit the request and the endpoint is
an absolute http(s) address without credentials, query or fragment. After decoding it enforces the one
result contract every protocol shares: exactly `topResults` (or all) entries, each index present in the
request and none twice, finite scores — then orders them best first, keeping the provider's order on
equal scores.

## Installed protocols

| Code | Providers | Key | Request | Response |
|---|---|---|---|---|
| `VOYAGE` | `voyage` | required | `POST {endpoint}` — `query`, `documents`, `model`, `top_k` (only when asked), `return_documents: false`, `truncation: false` | `data[{index, relevance_score}]`, usage `usage.total_tokens` |
| `COHERE` | `cohere`, `jina`, `gateway` | required for cohere and jina | `model`, `query`, `documents`, `top_n` (only when asked) | `results[{index, relevance_score}]`; usage `usage.total_tokens` (Jina), else `meta.tokens.input_tokens` (Cohere), else unknown |
| `TEI` | `tei` | none | `query`, `texts`, `raw_scores: false`, `return_text: false`, `truncate: false` | bare array `[{index, score}]`; no usage |

- Truncation is switched off wherever the provider offers the switch: an input over the model's limit is
  refused rather than silently shortened, so a score always describes the whole document sent.
- Cohere's `meta.billed_units.search_units` counts searches, not tokens, and is never reported as usage.
- TEI has no top-N parameter; a request for the best few is cut after ordering.
- The endpoint is the full operation address the adopter configures (for example
  `https://api.voyageai.com/v1/rerank`, `https://api.cohere.com/v2/rerank`, `https://api.jina.ai/v1/rerank`,
  `http://host:port/rerank`); nothing is derived from a chat or embeddings address.

## Not in this module

- Provider catalogue entries: `ProviderCatalog` (in `jmouse-ai-provider`) still advertises Voyage as
  `EMBEDDINGS` only and has no Cohere or Jina rows. Adding `RERANK` there is a separate, additive change —
  left out because that module carries other uncommitted work at the time of writing. Until then an
  adopter declares `RERANK` on its own provider configuration's capabilities.
- Retries, concurrency, quotas, caching, pricing, candidate selection and fusion with other scores — the
  adopter's managed policy, as for embeddings.

## Shared transport — the restructuring question

`JdkRerankTransport`, `JdkEmbeddingTransport` and the chat body handler in `jmouse-ai-provider` all
implement the same thing: a timed, redirect-free, byte-capped JSON POST with safe errors. Extracting it
into `jmouse-ai-transport` (a `BoundedJsonTransport`, a neutral `TransportLimits`, a neutral
`TransportException` with the same reasons) would let the three modules share one implementation. It
would need: the new module; each capability module adapting the neutral exception to its own (keeping its
public `EmbeddingException`/`RerankException` contracts so adopters do not change); deprecating, not
removing, `EmbeddingTransport`'s current signatures; and new pins for embeddings, reranking and provider.
It touches three published contracts at once, so it is its own ticket, not part of this one.

## Verification

`RerankProtocolTest` (7 tests) checks each protocol's request fields and response mapping, best-first
ordering, Jina/Cohere usage precedence and search units never counted, TEI's own top-N cut, malformed and
duplicate/out-of-range results refused without their contents, configuration and capability refused before
any call, request and registry rules. `JdkRerankTransportTest` (5 tests) runs the real transport against a
loopback server: reading within the cap, refusing an over-cap body, a refusal carrying the status but not
the body, a redirect never followed, and a timeout. No external provider is called.

Verified primary references:

- [Voyage rerank API](https://docs.voyageai.com/reference/reranker-api)
- [Cohere rerank API (v2)](https://docs.cohere.com/reference/rerank)
- [Jina reranker API](https://jina.ai/reranker/)
- [TEI OpenAPI](https://huggingface.github.io/text-embeddings-inference/openapi.json)
