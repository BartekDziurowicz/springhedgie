package eu.athenahouse.springhedgie.discovery;

import java.util.Map;

/**
 * Read-only view of a single MCP tool, built purely from metadata Spring AI/the
 * MCP SDK already derive from either {@code @Tool} (generic Spring AI function-calling
 * annotation, also used for MCP) or {@code @McpTool} (the richer, MCP-native annotation
 * from {@code spring-ai-mcp-annotations}).
 * <p>
 * SpringHedgie never re-implements schema generation - it just asks the relevant
 * upstream provider (Spring AI's {@code MethodToolCallbackProvider} for {@code @Tool},
 * or the MCP SDK's {@code SyncMcpToolProvider} for {@code @McpTool}) for the tool
 * definition it would use to register the tool, and renders that.
 *
 * @param name            the tool name as exposed over MCP (e.g. "searchKnowledge")
 * @param title           human-friendly title; only ever populated for {@code @McpTool}
 *                        methods, {@code null} for plain {@code @Tool} methods
 * @param description     the tool description shown to the calling agent/LLM
 * @param inputSchema     the JSON schema of the tool's parameters, already parsed
 *                        into a Map so it serializes as real JSON (not an escaped string)
 * @param annotationStyle which annotation declared this tool: {@code "@Tool"} or {@code "@McpTool"}
 * @param hints           MCP tool behavior hints (readOnlyHint/destructiveHint/idempotentHint/
 *                        openWorldHint) - only ever populated for {@code @McpTool} methods,
 *                        since plain {@code @Tool} has no equivalent concept
 */
public record SpringHedgieToolInfo(String name, String title, String description, Map<String, Object> inputSchema,
		String annotationStyle, Map<String, Object> hints) {
}
