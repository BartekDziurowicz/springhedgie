package eu.athenahouse.springhedgie.discovery;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import tools.jackson.databind.ObjectMapper;

/**
 * A minimal, self-contained MCP Streamable-HTTP client used purely to power
 * SpringHedgie's "Try it out" button - it calls the app's own MCP endpoint
 * as if it were any other MCP client (initialize -> notifications/initialized
 * -> tools/call), exactly the same handshake used to manually verify tools
 * during development with curl.
 * <p>
 * Forces HTTP/1.1: some backends (observed previously with a Python/ASGI
 * service in this project) misbehave when the JDK HttpClient attempts an
 * HTTP/2 upgrade, so we sidestep that entirely.
 */
public class McpToolInvoker {

	private final HttpClient httpClient = HttpClient.newBuilder()
		.version(HttpClient.Version.HTTP_1_1)
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	private final ObjectMapper objectMapper;
	private final AtomicInteger requestId = new AtomicInteger(1);

	public McpToolInvoker(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Object invoke(String mcpUrl, String toolName, Map<String, Object> arguments) throws IOException, InterruptedException {
		String sessionId = initialize(mcpUrl);
		sendInitializedNotification(mcpUrl, sessionId);
		return callTool(mcpUrl, sessionId, toolName, arguments);
	}

	private String initialize(String mcpUrl) throws IOException, InterruptedException {
		Map<String, Object> body = Map.of(
			"jsonrpc", "2.0",
			"id", requestId.getAndIncrement(),
			"method", "initialize",
			"params", Map.of(
				"protocolVersion", "2025-06-18",
				"capabilities", Map.of(),
				"clientInfo", Map.of("name", "springhedgie", "version", "0.1.0")));

		HttpResponse<String> response = post(mcpUrl, null, body);
		String sessionId = response.headers().firstValue("Mcp-Session-Id").orElse(null);
		if (sessionId == null) {
			throw new IOException("MCP server did not return an Mcp-Session-Id header during initialize");
		}
		return sessionId;
	}

	private void sendInitializedNotification(String mcpUrl, String sessionId) throws IOException, InterruptedException {
		Map<String, Object> body = Map.of(
			"jsonrpc", "2.0",
			"method", "notifications/initialized");
		post(mcpUrl, sessionId, body);
	}

	private Object callTool(String mcpUrl, String sessionId, String toolName, Map<String, Object> arguments)
			throws IOException, InterruptedException {
		Map<String, Object> body = Map.of(
			"jsonrpc", "2.0",
			"id", requestId.getAndIncrement(),
			"method", "tools/call",
			"params", Map.of("name", toolName, "arguments", arguments));

		HttpResponse<String> response = post(mcpUrl, sessionId, body);
		return extractResult(response.body());
	}

	private HttpResponse<String> post(String mcpUrl, String sessionId, Map<String, Object> body)
			throws IOException, InterruptedException {
		String json = objectMapper.writeValueAsString(body);
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(mcpUrl))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json, text/event-stream")
			.POST(HttpRequest.BodyPublishers.ofString(json));
		if (sessionId != null) {
			builder.header("Mcp-Session-Id", sessionId);
		}
		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	/**
	 * The Streamable-HTTP transport responds with Server-Sent-Events framing
	 * ("data: {...}") rather than a plain JSON body, so we pull the JSON-RPC
	 * payload out of the first matching "data:" line.
	 */
	@SuppressWarnings("unchecked")
	private Object extractResult(String rawBody) {
		String jsonPayload = rawBody;
		for (String line : rawBody.split("\n")) {
			if (line.startsWith("data:")) {
				jsonPayload = line.substring("data:".length()).trim();
				break;
			}
		}
		Map<String, Object> parsed = objectMapper.readValue(jsonPayload, Map.class);
		if (parsed.containsKey("error")) {
			return parsed.get("error");
		}
		return parsed.get("result");
	}
}
