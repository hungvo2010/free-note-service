package com.freenote.app.server.core.v2;

import io.opentelemetry.api.trace.Span;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TracingContext {
    private Span span;
}
