# ReguLens — Engineering Standards

RAG-based tool: Spring Boot monolith, pgvector, LangChain4j, Neon DB (no Docker, no local
Postgres — all DB work targets Neon). Goal: top-notch code quality and architecture, not a
prototype. Apply the rules below to every class written or reviewed in this repo.

## Architecture: hexagonal, inside one deployable

Even as a monolith (not microservices), keep dependencies pointing inward:

```
domain/          — entities, value objects, domain services. Zero Spring/JPA/Jackson imports.
application/     — use cases + port interfaces (in/out). Depends only on domain.
infrastructure/  — REST controllers, JPA adapters, LangChain4j integration, pgvector queries,
                   Neon datasource config. The only layer allowed to know about frameworks.
```
Rule: infrastructure implements interfaces defined in `application`; `application` and
`domain` never import from `infrastructure`. If a domain class needs `@Entity` or
`jakarta.persistence.*`, that logic has leaked out of the domain layer — split it into a
domain model + a separate JPA entity connected by a mapper.

RAG-specific note: treat the embedding/retrieval pipeline (chunking, embedding calls,
pgvector similarity search, prompt assembly) as **application-layer orchestration** calling
**driven ports** (`EmbeddingProvider`, `VectorStore`, `LlmClient`) — never let a use case
depend on LangChain4j classes directly. This is what lets you swap embedding models or the
LLM provider without touching business logic, and what makes the retrieval logic testable
without hitting Neon or a real model.

## OOP — no anemic models

- Business rules live on the object that owns the data, not in a "Service" that just reads/
  writes another class's fields. If a service method only calls getters/setters on one entity,
  move that method onto the entity.
- Protect invariants inside the object (named methods like `document.markIndexed()`, not a
  public `setStatus()` any caller can misuse).
- Favor composition over inheritance; reach for an interface + Strategy when behavior varies
  by type (e.g. different chunking strategies per document type, different retrieval strategies).
- Value objects (`DocumentId`, `EmbeddingVector`, `ChunkMetadata`) are immutable — use Java
  `record`s.
- SOLID, applied practically: one reason to change per class; extend via new
  implementations of an interface, not by editing an existing `switch`; depend on interfaces
  the domain/application layer defines, never directly on a concrete infra class.

## Separation of concerns

- Cross-cutting concerns (logging, metrics, auth checks) go in AOP/filters/annotations, not
  copy-pasted inside business methods.
- Exceptions: named domain exceptions → a shared hierarchy → translated to HTTP responses in
  one `@RestControllerAdvice`. Never leak a raw stack trace or internal message externally.
- Three separate validation layers, don't conflate them: request-shape validation
  (`jakarta.validation` on DTOs) vs. business-rule validation (domain code) vs. cross-field
  consistency.
- Config via typed `@ConfigurationProperties`, not scattered `@Value`. Neon connection
  details and any API keys (embedding/LLM providers) come from environment variables or a
  secrets store — never committed to `application.yml`.

## Scalability & resilience (still matters in a monolith)

- Every outbound call — to Neon, to the embedding provider, to the LLM — gets an explicit
  timeout. No bare `restTemplate`/`webClient` calls with default settings.
- Retry + circuit breaker (Resilience4j) around embedding/LLM calls specifically: these are
  the slowest, most failure-prone dependencies in a RAG pipeline. Always pair a circuit
  breaker with a sensible fallback (cached response, clear degraded-mode error) rather than
  a bare exception.
- Idempotency: if any endpoint can be retried by a client (e.g. re-submitting a document for
  indexing), make sure processing it twice doesn't create duplicate chunks/embeddings —
  upsert by a natural key (document hash, source ID) rather than blind insert.
- Paginate everything that can grow — document lists, chunk results, search results. No
  unbounded `findAll()`.
- Watch for N+1 queries when loading a document's chunks/embeddings; use a join fetch or a
  dedicated projection query.
- Connection pool (HikariCP) sized sensibly against Neon's connection limits — Neon's serverless
  connection model makes this worth checking explicitly rather than assuming defaults are fine.

## Non-negotiables

- No domain class importing `jakarta.persistence.*`, `org.springframework.*`, or LangChain4j
  classes.
- No outbound call (Neon, embedding provider, LLM provider) without a timeout.
- No raw exception details returned from the API.
- No unbounded queries on data that grows with usage (documents, chunks, embeddings).
- No business logic embedded in a controller — controllers call a use case and translate the
  result; nothing else.

## When it's fine to simplify

A trivial CRUD endpoint (e.g. managing a lookup/reference table) doesn't need the full
domain/port ceremony — a thin `@Service` over Spring Data directly is fine there. State that
trade-off explicitly when you take it. Never simplify on the ingestion/retrieval pipeline or
anything touching document state — that's exactly where the invariant protection matters.