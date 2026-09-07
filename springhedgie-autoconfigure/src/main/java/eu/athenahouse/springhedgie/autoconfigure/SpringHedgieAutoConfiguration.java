package eu.athenahouse.springhedgie.autoconfigure;

import tools.jackson.databind.ObjectMapper;
import eu.athenahouse.springhedgie.discovery.McpToolInvoker;
import eu.athenahouse.springhedgie.discovery.ToolDiscoveryService;
import eu.athenahouse.springhedgie.web.SpringHedgieController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Auto-activates SpringHedgie the moment {@code springhedgie-core} is on the classpath,
 * the same way {@code springdoc-openapi-starter-webmvc-ui} activates Swagger UI.
 * No manual {@code @Import} or config required - just add the dependency.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "springhedgie", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SpringHedgieProperties.class)
public class SpringHedgieAutoConfiguration {

	@Bean
	public ToolDiscoveryService toolDiscoveryService(ApplicationContext applicationContext,
			ObjectMapper objectMapper) {
		return new ToolDiscoveryService(applicationContext, objectMapper);
	}

	@Bean
	public McpToolInvoker mcpToolInvoker(ObjectMapper objectMapper) {
		return new McpToolInvoker(objectMapper);
	}

	@Bean
	public SpringHedgieController springHedgieController(ToolDiscoveryService toolDiscoveryService,
			McpToolInvoker mcpToolInvoker, SpringHedgieProperties properties) {
		return new SpringHedgieController(toolDiscoveryService, mcpToolInvoker, properties);
	}
}
