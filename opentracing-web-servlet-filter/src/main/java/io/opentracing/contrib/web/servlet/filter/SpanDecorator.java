package io.opentracing.contrib.web.servlet.filter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

/**
 * SpanDecorator to decorate span at different stages in filter processing (before filterChain.doFilter(), after and
 * if exception is thrown).
 *
 * @author Pavol Loffay
 */
public interface SpanDecorator {

    /**
     * Decorate span before {@link javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)} is
     * called. This is called right after span in created. Span is already present in request attributes with name
     * {@link TracingFilter#ACTIVE_SPAN}.
     *
     * @param httpServletRequest request
     * @param span span to decorate
     */
    void onRequest(HttpServletRequest httpServletRequest, Span span);

    /**
     * Decorate span after {@link javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)} is
     * called.
     *
     * @param httpServletRequest request
     * @param httpServletResponse response
     * @param span span to decorate
     */
    void onResponse(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Span span);

    /**
     * Decorate span when an exception is thrown during processing in
     * {@link javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)}.
     * It can be {@link RuntimeException} from {@link javax.servlet.Filter} or any checked exception from
     * {@link javax.servlet.http.HttpServlet}.
     *
     * @param httpServletRequest request
     * @param exception exception
     * @param span span to decorate
     */
    void onError(HttpServletRequest httpServletRequest, Throwable exception, Span span);

    /**
     * Adds standard tags to span. {@link Tags#HTTP_URL}, {@link Tags#HTTP_STATUS}, {@link Tags#HTTP_METHOD} and
     * {@link Tags#COMPONENT}. If an exception during
     * {@link javax.servlet.Filter#doFilter(ServletRequest, ServletResponse, FilterChain)} is thrown tag
     * {@link Tags#ERROR} is added and {@link Tags#HTTP_STATUS} not because at this point it is not known.
     */
    SpanDecorator STANDARD_TAGS = new SpanDecorator() {
        @Override
        public void onRequest(HttpServletRequest httpServletRequest, Span span) {
            Tags.COMPONENT.set(span, "java-web-servlet");

            Tags.HTTP_METHOD.set(span, httpServletRequest.getMethod());
            //without query params
            Tags.HTTP_URL.set(span, httpServletRequest.getRequestURL().toString());
        }

        @Override
        public void onResponse(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                               Span span) {
                Tags.HTTP_STATUS.set(span, httpServletResponse.getStatus());
        }

        @Override
        public void onError(HttpServletRequest httpServletRequest, Throwable exception, Span span) {
            Tags.ERROR.set(span, Boolean.TRUE);
            span.log(logsForException(exception));
        }

        private Map<String, String> logsForException(Throwable throwable) {
            Map<String, String> errorLog = new HashMap<>();
            errorLog.put("event", Tags.ERROR.getKey());
            if (throwable.getMessage() != null) {
                errorLog.put("message", throwable.getLocalizedMessage());
            }
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            errorLog.put("stack", sw.toString());

            return errorLog;
        }
    };
}
