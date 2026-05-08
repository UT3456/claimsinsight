package com.claims.config;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drops Eureka heartbeat spans before they're shipped to Zipkin so the
 * Dependency graph shows business calls only. See identical bean in
 * api-gateway's TracingConfig for the full rationale.
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
