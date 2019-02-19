package io.opentracing.contrib.web.servlet.filter.decorator;

import io.opentracing.Span;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import io.opentracing.tag.StringTag;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletFilterHeaderSpanDecorator implements ServletFilterSpanDecorator {

    private final String baseTagKey;
    private final List<HeaderEntry> headers;

    public ServletFilterHeaderSpanDecorator(List<HeaderEntry> headers) {
        this(headers, "http.header");
    }

    public ServletFilterHeaderSpanDecorator(List<HeaderEntry> headers, String baseTagKey) {
        this.headers = new ArrayList<>(headers);
        this.baseTagKey = (baseTagKey != null && !baseTagKey.isEmpty()) ? (baseTagKey + ".") : null;
    }

    @Override
    public void onRequest(HttpServletRequest httpServletRequest, Span span) {
        for (HeaderEntry headerEntry : headers) {
            String headerValue = httpServletRequest.getHeader(headerEntry.getHeader());
            if (headerValue != null && !headerValue.isEmpty()) {
                buildTag(headerEntry.getTag()).set(span, headerValue);
            }
        }
    }

    @Override
    public void onResponse(HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, Span span) {
    }

    @Override
    public void onError(HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, Throwable exception, Span span) {
    }

    @Override
    public void onTimeout(HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, long timeout, Span span) {
    }

    private StringTag buildTag(String tag) {
        if (baseTagKey == null) {
            return new StringTag(tag);
        }
        return new StringTag(baseTagKey + tag);
    }

    public String getBaseTagKey() {
        return this.baseTagKey;
    }

    public List<HeaderEntry> getHeaders() {
        return this.headers;
    }

    public static class HeaderEntry {
        private String header;
        private String tag;

        public HeaderEntry(String header, String tag) {
            this.header = header;
            this.tag = tag;
        }

        public HeaderEntry() {
        }

        public String getHeader() {
            return this.header;
        }

        public String getTag() {
            return this.tag;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

    }

}
