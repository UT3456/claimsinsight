package com.claiminsight.gateway.config;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drops Eureka heartbeat spans before they're shipped to Zipkin.
 *
 * Every service polls Eureka every 5–10 s for service discovery and lease
 * renewal. The Brave HTTP-client instrumentation tags those polls as spans
 * with `http.path` containing `/eureka/...`. Without this filter, the
 * Zipkin Dependency graph shows a thick "→ eureka-server" edge from every
 * service, drowning out the actual business call paths.
 *
 * Returning {@code false} from {@link SpanHandler#end} prevents the span
 * from being reported. The trace is otherwise unaffected; spans for
 * business calls (Feign, gateway routing, etc.) flow through normally.
 */
@Configuration
public class TracingConfig {

    @Bean
    public SpanHandler eurekaSpanFilter() {
        return new SpanHandler() {
            @Override
            public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                String path = span.tag("http.path");
                if (path != null && path.contains("/eureka/")) return false;
                String url = span.tag("http.url");
                if (url != null && url.contains("/eureka/")) return false;
                String name = span.name();
                if (name != null && name.toLowerCase().contains("eureka")) return false;
                return true;
            }
        };
    }
}
