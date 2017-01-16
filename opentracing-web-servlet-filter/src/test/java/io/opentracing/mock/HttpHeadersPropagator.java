package io.opentracing.mock;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import io.opentracing.contrib.web.servlet.filter.HttpServletRequestExtractAdapter;
import io.opentracing.propagation.Format;

public class HttpHeadersPropagator implements MockTracer.Propagator {

    public static final String HEADER_SPAN_ID = "spanid";
    public static final String HEADER_TRACE_ID = "traceid";

    @Override
    public <C> void inject(MockSpan.MockContext ctx, Format<C> format, C carrier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <C> MockSpan.MockContext extract(Format<C> format, C carrier) {
        Long traceId = null;
        Long spanId = null;

        if (format.equals(Format.Builtin.HTTP_HEADERS)) {
            HttpServletRequestExtractAdapter extractAdapter = (HttpServletRequestExtractAdapter) carrier;
            Iterator<Map.Entry<String, String>> iterator = extractAdapter.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                if (HEADER_TRACE_ID.equals(entry.getKey())) {
                    traceId = Long.valueOf(entry.getValue());
                } else if (HEADER_SPAN_ID.equals(entry.getKey())) {
                    spanId = Long.valueOf(entry.getValue());
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown format");
        }

        if (traceId != null && spanId != null) {
            return new MockSpan.MockContext(traceId, spanId, Collections.<String, String>emptyMap());
        }

        return null;
    }
}

