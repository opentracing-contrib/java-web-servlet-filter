package io.opentracing.contrib.web.servlet.filter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.AsyncEvent;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.opentracing.BaseSpan;
import io.opentracing.tag.Tags;

/**
 * SpanDecorator to decorate span at different stages in filter processing (before filterChain.doFilter(), after and
 * if exception is thrown).
 *
 * @author Pavol Loffay
 */
public interface ServletFilterSpanDecorator {

    /**
     * Decorate span before {@link javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)} is
     * called. This is called right after span in created. Span is already present in request attributes with name
     * {@link TracingFilter#SERVER_SPAN_CONTEXT}.
     *
     * @param httpServletRequest request
     * @param span span to decorate
     */
    void onRequest(HttpServletRequest httpServletRequest, BaseSpan<?> span);

    /**
     * Decorate span after {@link javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)}. When it
     * is an async request this will be called in {@link javax.servlet.AsyncListener#onComplete(AsyncEvent)}.
     *
     * @param httpServletRequest request
     * @param httpServletResponse response
     * @param span span to decorate
     */
    void onResponse(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, BaseSpan<?> span);

    /**
     * Decorate span when an exception is thrown during processing in
     * {@link javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)}. This is
     * also called in {@link javax.servlet.AsyncListener#onError(AsyncEvent)}.
     *
     * @param httpServletRequest request
     * @param exception exception
     * @param span span to decorate
     */
    void onError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                 Throwable exception, BaseSpan<?> span);

    /**
     * Decorate span on asynchronous request timeout. It is called in
     * {@link javax.servlet.AsyncListener#onTimeout(AsyncEvent)}.
     *
     * @param httpServletRequest request
     * @param httpServletResponse response
     * @param timeout timeout
     * @param span span to decorate
     */
    void onTimeout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                 long timeout, BaseSpan<?> span);

    /**
     * Adds standard tags to span. {@link Tags#HTTP_URL}, {@link Tags#HTTP_STATUS}, {@link Tags#HTTP_METHOD} and
     * {@link Tags#COMPONENT}. If an exception during
     * {@link javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)} is thrown tag
     * {@link Tags#ERROR} is added and {@link Tags#HTTP_STATUS} not because at this point it is not known.
     */
    ServletFilterSpanDecorator STANDARD_TAGS = new ServletFilterSpanDecorator() {
        @Override
        public void onRequest(HttpServletRequest httpServletRequest, BaseSpan<?> span) {
            Tags.COMPONENT.set(span, "java-web-servlet");

            Tags.HTTP_METHOD.set(span, httpServletRequest.getMethod());
            //without query params
            Tags.HTTP_URL.set(span, httpServletRequest.getRequestURL().toString());
        }

        @Override
        public void onResponse(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                            BaseSpan<?> span) {
                Tags.HTTP_STATUS.set(span, httpServletResponse.getStatus());
        }

        @Override
        public void onError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                            Throwable exception, BaseSpan<?> span) {
            Tags.ERROR.set(span, Boolean.TRUE);
            span.log(logsForException(exception));

            if (httpServletResponse.getStatus() == HttpServletResponse.SC_OK) {
                // exception is thrown in filter chain, but status code is incorrect
                Tags.HTTP_STATUS.set(span, 500);
            }
        }

        @Override
        public void onTimeout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                              long timeout, BaseSpan<?> span) {
            Tags.ERROR.set(span, Boolean.TRUE);

            Map<String, Object> timeoutLogs = new HashMap<>();
            timeoutLogs.put("event", Tags.ERROR.getKey());
            timeoutLogs.put("message", "timeout");
            timeoutLogs.put("timeout", timeout);
        }

        private Map<String, String> logsForException(Throwable throwable) {
            Map<String, String> errorLog = new HashMap<>(3);
            errorLog.put("event", Tags.ERROR.getKey());

            String message = throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();
            if (message != null) {
                errorLog.put("message", message);
            }
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            errorLog.put("stack", sw.toString());

            return errorLog;
        }
    };
}
