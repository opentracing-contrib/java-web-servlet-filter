package io.opentracing.contrib.web.servlet.filter;

import io.opentracing.Span;

/**
 * openTracing span context. can get the current thread's span.
 * <pre>
 * {@code
 * 	TrracingSpanContext.add(Span);
 * Span span = TrracingSpanContext.get();
 * TrracingSpanContext.release();
 * }
 * </pre>
 */
public class TrracingSpanContext {
    private static ThreadLocal<Span> spans = new ThreadLocal<Span>();

    public static void add(Span span) {
        spans.set(span);
    }

    public static Span get() {
        return spans.get();
    }

    public static void release() {
        spans.remove();
    }
}
