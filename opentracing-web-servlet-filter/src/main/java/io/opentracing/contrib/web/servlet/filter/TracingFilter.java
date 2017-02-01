package io.opentracing.contrib.web.servlet.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * Tracing servlet filter.
 *
 * Filter can be programmatically added to {@link ServletContext} or initialized via web.xml.
 *
 * Following code examples show possible initialization.
 *
 * <pre>
 * {@code
  * TracingFilter filter = new TracingFilter(tracer, Arrays.asList(SpanDecorator.STANDARD_TAGS))
 *  servletContext.addFilter("tracingFilter", filter);
  * }
 * </pre>
 *
 * Or include filter in web.xml and:
 * <pre>
 * {@code
 *  servletContext.setAttribute({@link TracingFilter#TRACER}, tracer);
 *  servletContext.setAttribute({@link TracingFilter#SPAN_DECORATORS}, decorators); // optional, if no present SpanDecorator.STANDARD_TAGS is applied
 * }
 * </pre>
 *
 * Current server span is accessible via {@link HttpServletRequest#getAttribute(String)} with name
 * {@link TracingFilter#ACTIVE_SPAN}.
 *
 * @author Pavol Loffay
 */
public class TracingFilter implements Filter {

    private static final Logger log = Logger.getLogger(TracingFilter.class.getName());

    /**
     * Use as a key of {@link ServletContext#setAttribute(String, Object)} to set Tracer
     */
    public static final String TRACER = TracingFilter.class.getName() + ".tracer";
    /**
     * Use as a key of {@link ServletContext#setAttribute(String, Object)} to set span decorators
     */
    public static final String SPAN_DECORATORS = TracingFilter.class.getName() + ".spanDecorators";
    /**
     * Used as a key of {@link HttpServletRequest#setAttribute(String, Object)} to inject current server span
     */
    public static final String ACTIVE_SPAN = TracingFilter.class.getName() + ".activeSpan";

    private FilterConfig filterConfig;
    private boolean skipFilter;

    private Tracer tracer;
    private List<SpanDecorator> spanDecorators;

    /**
     * When using this constructor one has to provide required ({@link TracingFilter#TRACER}
     * attribute in {@link ServletContext#setAttribute(String, Object)}.
     */
    public TracingFilter() {}

    /**
     *
     * @param tracer tracer
     * @param spanDecorators decorators
     */
    public TracingFilter(Tracer tracer, List<SpanDecorator> spanDecorators) {
        this.tracer = tracer;
        this.spanDecorators = new ArrayList<>(spanDecorators);
        this.spanDecorators.removeAll(Collections.singleton(null));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;

        // init if the tracer is null
        if (tracer == null) {
            ServletContext servletContext = filterConfig.getServletContext();

            Object contextAttribute = servletContext.getAttribute(TRACER);
            if (contextAttribute == null) {
                log.severe("Tracer was not found in `ServletContext.getAttribute(TRACER)`, skipping tracing filter");
                this.skipFilter = true;
                return;
            }
            if (!(contextAttribute instanceof Tracer)) {
                log.severe("Tracer from `ServletContext.getAttribute(TRACER)`, is not an instance of " +
                        "io.opentracing.Tracer, skipping tracing filter");
                this.skipFilter = true;
                return;
            }
            this.tracer = (Tracer)contextAttribute;

            this.spanDecorators = (List<SpanDecorator>)servletContext.getAttribute(SPAN_DECORATORS);
            if (this.spanDecorators == null) {
                this.spanDecorators = Arrays.asList(SpanDecorator.STANDARD_TAGS);
            }
            this.spanDecorators.removeAll(Collections.singleton(null));
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        if (skipFilter) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        /**
         * Check if request has been already traced do not proceed.
         */
        if (servletRequest.getAttribute(ACTIVE_SPAN) != null) {
            chain.doFilter(servletRequest, servletResponse);
        } else if (isTraced(httpRequest, httpResponse)){
            SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                    new HttpServletRequestExtractAdapter(httpRequest));

            Span span = tracer.buildSpan(httpRequest.getMethod())
                    .asChildOf(extractedContext)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                    .start();

            httpRequest.setAttribute(ACTIVE_SPAN, span);

            for (SpanDecorator spanDecorator: spanDecorators) {
                spanDecorator.onRequest(httpRequest, span);
            }

            try {
                chain.doFilter(servletRequest, servletResponse);
                for (SpanDecorator spanDecorator: spanDecorators) {
                    spanDecorator.onResponse(httpRequest, httpResponse, span);
                }
                // catch all exceptions (e.g. RuntimeException, ServletException...)
            } catch (Throwable ex) {
                for (SpanDecorator spanDecorator : spanDecorators) {
                    spanDecorator.onError(httpRequest, ex, span);
                }
                throw ex;
            } finally {
                span.finish();
            }
        }
    }

    @Override
    public void destroy() {
        this.filterConfig = null;
    }

    /**
     * It checks whether a request should be traced or not.
     *
     * @param httpServletRequest request
     * @param httpServletResponse response
     * @return whether request should be traced or not
     */
    protected boolean isTraced(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return true;
    }
}
