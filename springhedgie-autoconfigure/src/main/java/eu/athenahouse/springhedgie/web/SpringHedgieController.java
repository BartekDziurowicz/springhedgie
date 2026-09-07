package eu.athenahouse.springhedgie.web;

import java.util.List;
import java.util.Map;

import eu.athenahouse.springhedgie.autoconfigure.SpringHedgieProperties;
import eu.athenahouse.springhedgie.discovery.McpToolInvoker;
import eu.athenahouse.springhedgie.discovery.SpringHedgieToolInfo;
import eu.athenahouse.springhedgie.discovery.ToolDiscoveryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the tool list and app metadata as JSON for the SpringHedgie UI
 * (static/springhedgie/index.html) to render, and powers "Try it out" by
 * proxying tool invocations through {@link McpToolInvoker} to the app's own
 * MCP endpoint. Kept as plain JSON, not the raw MCP protocol format, so the
 * UI itself has no dependency on the MCP transport.
 */
@RestController
public class SpringHedgieController {

	private final ToolDiscoveryService toolDiscoveryService;
	private final McpToolInvoker mcpToolInvoker;
	private final SpringHedgieProperties properties;

	@Value("${spring.application.name:Application}")
	private String applicationName;

	public SpringHedgieController(ToolDiscoveryService toolDiscoveryService, McpToolInvoker mcpToolInvoker,
			SpringHedgieProperties properties) {
		this.toolDiscoveryService = toolDiscoveryService;
		this.mcpToolInvoker = mcpToolInvoker;
		this.properties = properties;
	}

	@GetMapping("/springhedgie/api/tools")
	public List<SpringHedgieToolInfo> tools() {
		return toolDiscoveryService.discoverTools();
	}

	@GetMapping("/springhedgie/api/info")
	public Map<String, Object> info() {
		return Map.of("applicationName", applicationName);
	}

	@PostMapping("/springhedgie/api/tools/{name}/invoke")
	public ResponseEntity<Object> invoke(@PathVariable("name") String name, @RequestBody Map<String, Object> arguments,
			HttpServletRequest request) {
		try {
			String mcpUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
					+ properties.mcpPath();
			Object result = mcpToolInvoker.invoke(mcpUrl, name, arguments);
			return ResponseEntity.ok(result);
		}
		catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
		}
	}
}
