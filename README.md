# SpringHedgie 🦔

> Swagger-like, browsable documentation UI — with a working **"Try it out"** — for Spring AI's `@Tool` and `@McpTool` annotated MCP tools, embedded directly in your own Spring Boot app.

[![Maven Central](https://img.shields.io/maven-central/v/eu.athenahouse.springhedgie/springhedgie-spring-boot-starter)](https://central.sonatype.com/artifact/eu.athenahouse.springhedgie/springhedgie-spring-boot-starter)
[![CI](https://github.com/BartekDziurowicz/springhedgie/actions/workflows/ci.yml/badge.svg)](https://github.com/BartekDziurowicz/springhedgie/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

## The problem

Spring AI already generates everything an MCP tool needs from your `@Tool`/`@ToolParam` (or the richer, MCP-native `@McpTool`) annotations — name, description, JSON schema — and serves it over the MCP protocol via `tools/list`. But there's no easy way to *see* that from inside your own running app. Today the only real option is [MCP Inspector](https://modelcontextprotocol.io/docs/tools/inspector), a separate Node.js process you run *outside* your app — not a dependency you simply add to it.

Swagger (`springdoc-openapi`) solved this problem for REST APIs. AsyncAPI (`springwolf`) solved it for Kafka/RabbitMQ listeners. Nothing solved it for MCP tools yet — likely because MCP is barely two years old and its Java tooling ecosystem hasn't caught up. **SpringHedgie fills that gap.**

## What you get

Add one dependency, nothing else. On startup, SpringHedgie:

1. Scans your Spring `ApplicationContext` for beans exposing `@Tool` and/or `@McpTool` annotated methods.
2. Reuses Spring AI's **own** discovery machinery — `MethodToolCallbackProvider` for `@Tool`, `SyncMcpToolProvider` for `@McpTool` — the exact classes Spring AI's MCP server autoconfiguration uses internally. SpringHedgie never re-implements schema generation, so it can never drift out of sync with what your MCP server actually exposes.
3. Serves the merged result as JSON at `GET /springhedgie/api/tools`.
4. Renders a browsable, interactive UI at `GET /springhedgie/index.html` — tool cards with descriptions, JSON schemas, MCP behavior hints (`readOnlyHint`, `destructiveHint`, `idempotentHint`, `openWorldHint`), and a **"Try it out"** panel that executes the tool for real, through your app's own MCP endpoint, and shows you the result.

## Quick start

Add the starter to your Spring Boot application:

```xml
<dependency>
    <groupId>eu.athenahouse.springhedgie</groupId>
    <artifactId>springhedgie-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Start your app, then open:

```
http://localhost:<port>/springhedgie/index.html
```

That's it — no configuration required. SpringHedgie auto-detects your app's name (from `spring.application.name`) and every `@Tool`/`@McpTool` method already registered in your Spring context.

### Configuration (optional)

```properties
# Disable SpringHedgie entirely (e.g. in production)
springhedgie.enabled=false

# Must match spring.ai.mcp.server.streamable-http.mcp-endpoint if you've customized it
springhedgie.mcp-path=/mcp
```

## How "Try it out" works

Unlike a typical OpenAPI doc UI, MCP tools aren't plain REST endpoints — they're invoked through the MCP JSON-RPC protocol (`initialize` → `notifications/initialized` → `tools/call`, over Streamable HTTP with SSE-framed responses). SpringHedgie ships a small, self-contained MCP client (`McpToolInvoker`) that performs this handshake as a *loopback* call to your own app's `/mcp` endpoint, so clicking "Try it out" genuinely executes your tool exactly as any real MCP client would — no mocking, no duplicated logic.

## Module structure

This repository follows the standard Spring Boot starter convention:

| Module | Purpose |
|---|---|
| `springhedgie-autoconfigure` | All the real code: discovery, the MCP loopback client, the REST controller, and the static UI. |
| `springhedgie-spring-boot-starter` | Empty aggregator — the single dependency you actually add to your project. |

## Known limitations

- Base path (`/springhedgie`) is currently hardcoded, not configurable.
- No security/auth story yet — SpringHedgie assumes the same trust boundary as the rest of your app. Consider disabling it (`springhedgie.enabled=false`) in production, the same way you would `springdoc-openapi`'s Swagger UI.
- MCP-level errors (`isError: true` inside an otherwise successful response) aren't specially highlighted in the UI yet — only transport-level JSON-RPC errors are.
- No structured "possible error responses" documentation — MCP has no OpenAPI-style schema for this, so it would have to be authored manually (like Javadoc `@throws`), which isn't implemented yet.

## Roadmap

- [ ] Configurable base path
- [ ] Optional Spring Security integration
- [ ] Dedicated styling for `isError: true` tool responses
- [x] Publish to Maven Central under `eu.athenahouse.springhedgie`
- [x] GitHub Actions CI + tagged releases

## License

Apache License 2.0 — see [LICENSE](LICENSE).
