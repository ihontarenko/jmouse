# jmouse-ai-model

Framework-free technical model vocabulary shared by administration and execution. AiCapability declares CHAT, EMBEDDINGS, RERANK, IMAGE and AUDIO. This is vocabulary, not an assertion that adapters for every operation are installed. A provider is who, a capability what, a model the concrete implementation, and purpose the adopter's own use case.

No Spring, tool catalogue, persistence, HTTP or deprecated common-library dependency. jmouse-ai and jmouse-ai-provider both depend on this tiny module without depending on each other.

## Compatibility and adoption

The affected AI modules and new modules use 1.0.0-b20261004-ai-capabilities. Existing products pinned to 1.0.0-b20260902 remain on the original artifacts. The parent and existing BOM entries retain their existing versions. New model/embeddings entries explicitly name the isolated version.

ProviderAdministration.Configuration, Draft and SupportedProvider append capability metadata and preserve their old constructor descriptors. ProviderSettings preserves its five-argument constructor, of and withApiUrl. Legacy drafts/settings and migrated rows declare CHAT. Explicit empty/unsupported model capabilities are rejected; RERANK/IMAGE/AUDIO remain vocabulary without shipped execution adapters.

Capabilities are configured per model row as a subset of ProviderCatalog's operation ceiling. The catalogue declares CHAT plus EMBEDDINGS for OpenAI/Ollama, EMBEDDINGS for TEI, and CHAT for the other existing entries. This ceiling describes the integrated protocol family; choosing a particular embedding-capable model remains an administrator's responsibility. Anthropic chat support is not inferred to support embeddings.

ProviderSettingsSource.settings(purpose, capability) is independent of application purpose. The JPA source filters by application, active state and capability, refusing ambiguity. An explicitly configured purpose missing the requested capability is refused rather than silently routed to general. settingsById selects one active application-owned configuration and fails closed for sources without managed identifiers. Old settings() and settings(purpose) keep CHAT behavior.

Activation only competes with rows having the same normalized purpose and an overlapping capability; disjoint CHAT/EMBEDDINGS configurations can both be active. A multi-capability row is activated/deactivated as a whole. Activation locks existing application rows in stable order. Changing an active row's purpose/capabilities requires taking it out of force. Keys and ordinary model settings retain existing live-edit behavior.

The append-only V000007 library migration adds capabilities VARCHAR(256), default CHAT, and optimistic row revision BIGINT, default 0. PostgreSQL and MySQL variants are provided; PostgreSQL adoption is integration-tested. JPA converter persists stable enum names, never ordinals; unknown names fail closed. Configuration revisions fence in-flight settings snapshots; they are not a replacement for a future expected-revision HTTP editing contract or immutable library history.

ProviderSettings and Draft diagnostics redact credentials. Configuration responses remain key-free. Management controllers and conversation/starter modules retain their existing API; adopter authorization still protects library routes. A populated capability set does not guarantee an endpoint is healthy or remotely verify a model revision.
