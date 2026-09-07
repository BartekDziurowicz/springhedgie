package eu.athenahouse.springhedgie.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for SpringHedgie.
 * <p>
 * {@code mcpPath} must match {@code spring.ai.mcp.server.streamable-http.mcp-endpoint}
 * of the app it's embedded in - SpringHedgie calls that endpoint as a loopback
 * MCP client to power "Try it out". Everything else is kept intentionally tiny
 * for the prototype (just an on/off switch); more knobs (custom base path,
 * security, tool object include/exclude filters) are future work.
 */
@ConfigurationProperties(prefix = "springhedgie")
public record SpringHedgieProperties(@DefaultValue("true") boolean enabled, @DefaultValue("/mcp") String mcpPath) {
}
