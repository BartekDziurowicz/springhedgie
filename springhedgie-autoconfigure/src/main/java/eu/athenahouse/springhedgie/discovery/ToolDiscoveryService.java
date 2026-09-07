package eu.athenahouse.springhedgie.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.provider.tool.SyncMcpToolProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

/**
 * Finds every Spring bean that has at least one MCP-tool-annotated method and
 * turns it into a {@link SpringHedgieToolInfo} the UI can render.
 * <p>
 * Two annotation styles are supported, matching what Spring AI itself supports
 * for registering MCP tools:
 * <ul>
 *   <li>{@code @Tool} - the generic Spring AI function-calling annotation, read
 *       via {@link MethodToolCallbackProvider} (same class the MCP server
 *       autoconfiguration uses internally).</li>
 *   <li>{@code @McpTool} - the richer, MCP-native annotation from
 *       {@code spring-ai-mcp-annotations}, read via {@link SyncMcpToolProvider}.
 *       Exposes extra metadata plain {@code @Tool} has no equivalent for: a
 *       display {@code title} and MCP behavior hints (readOnly/destructive/
 *       idempotent/openWorld).</li>
 * </ul>
 * SpringHedgie deliberately never reimplements schema generation for either
 * style - it just asks the relevant upstream provider for the tool definition
 * it would use to register the tool, and renders that.
 */
public class ToolDiscoveryService {

	private final ApplicationContext applicationContext;
	private final ObjectMapper objectMapper;

	public ToolDiscoveryService(ApplicationContext applicationContext, ObjectMapper objectMapper) {
		this.applicationContext = applicationContext;
		this.objectMapper = objectMapper;
	}

	public List<SpringHedgieToolInfo> discoverTools() {
		List<Object> allBeans = List.copyOf(applicationContext.getBeansOfType(Object.class).values());

		List<SpringHedgieToolInfo> tools = new ArrayList<>();
		tools.addAll(discoverToolAnnotated(allBeans));
		tools.addAll(discoverMcpToolAnnotated(allBeans));
		return tools;
	}

	private List<SpringHedgieToolInfo> discoverToolAnnotated(List<Object> allBeans) {
		Object[] toolBeans = allBeans.stream()
			.filter(bean -> hasAnyMethodAnnotatedWith(bean, Tool.class))
			.toArray();

		if (toolBeans.length == 0) {
			return List.of();
		}

		ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
			.toolObjects(toolBeans)
			.build()
			.getToolCallbacks();

		List<SpringHedgieToolInfo> tools = new ArrayList<>();
		for (ToolCallback callback : callbacks) {
			var definition = callback.getToolDefinition();
			tools.add(new SpringHedgieToolInfo(definition.name(), null, definition.description(),
				parseSchema(definition.inputSchema()), "@Tool", null));
		}
		return tools;
	}

	private List<SpringHedgieToolInfo> discoverMcpToolAnnotated(List<Object> allBeans) {
		List<Object> mcpToolBeans = allBeans.stream()
			.filter(bean -> hasAnyMethodAnnotatedWith(bean, McpTool.class))
			.toList();

		if (mcpToolBeans.isEmpty()) {
			return List.of();
		}

		List<McpServerFeatures.SyncToolSpecification> specifications = new SyncMcpToolProvider(mcpToolBeans)
			.getToolSpecifications();

		List<SpringHedgieToolInfo> tools = new ArrayList<>();
		for (McpServerFeatures.SyncToolSpecification spec : specifications) {
			McpSchema.Tool tool = spec.tool();
			tools.add(new SpringHedgieToolInfo(tool.name(), tool.title(), tool.description(),
				tool.inputSchema() == null ? Map.of() : tool.inputSchema(), "@McpTool", toHintsMap(tool.annotations())));
		}
		return tools;
	}

	private Map<String, Object> toHintsMap(McpSchema.ToolAnnotations annotations) {
		if (annotations == null) {
			return Map.of();
		}
		Map<String, Object> hints = new LinkedHashMap<>();
		if (annotations.readOnlyHint() != null) hints.put("readOnlyHint", annotations.readOnlyHint());
		if (annotations.destructiveHint() != null) hints.put("destructiveHint", annotations.destructiveHint());
		if (annotations.idempotentHint() != null) hints.put("idempotentHint", annotations.idempotentHint());
		if (annotations.openWorldHint() != null) hints.put("openWorldHint", annotations.openWorldHint());
		if (annotations.returnDirect() != null) hints.put("returnDirect", annotations.returnDirect());
		return hints;
	}

	private boolean hasAnyMethodAnnotatedWith(Object bean, Class<? extends java.lang.annotation.Annotation> annotation) {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		return Arrays.stream(targetClass.getMethods()).anyMatch(m -> m.isAnnotationPresent(annotation));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseSchema(String rawJsonSchema) {
		try {
			return objectMapper.readValue(rawJsonSchema, Map.class);
		}
		catch (Exception e) {
			// If Spring AI ever changes the schema format, fail soft: show the raw
			// string rather than crashing the whole tools page.
			return Map.of("raw", rawJsonSchema);
		}
	}
}
