package io.opentracing.contrib.web.servlet.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
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
import io.opentracing.util.GlobalTracer;

/**
 * Tracing servlet filter.
 *
 * Filter can be programmatically added to {@link ServletContext} or initialized via web.xml.
 *
 * Following code examples show possible initialization:
 *
 * <pre>
 * {@code
  * TracingFilter filter = new TracingFilter(tracer);
 *  servletContext.addFilter("tracingFilter", filter);
  * }
 * </pre>
 *
 * Or include filter in web.xml and:
 * <pre>
 * {@code
 *  GlobalTracer.register(tracer);
 *  servletContext.setAttribute({@link TracingFilter#SPAN_DECORATORS}, listOfDecorators); // optional, if no present ServletFilterSpanDecorator.STANDARD_TAGS is applied
 * }
 * </pre>
 *
 * Current server span context is accessible via {@link HttpServletRequest#getAttribute(String)} with name
 * {@link TracingFilter#SERVER_SPAN_CONTEXT}.
 *
 * @author Pavol Loffay
 */
public class TracingFilter implements Filter {
    private static final Logger log = Logger.getLogger(TracingFilter.class.getName());

    /**
     * Use as a key of {@link ServletContext#setAttribute(String, Object)} to set span decorators
     */
    public static final String SPAN_DECORATORS = TracingFilter.class.getName() + ".spanDecorators";
    /**
     * Use as a key of {@link ServletContext#setAttribute(String, Object)} to skip pattern
     */
    public static final String SKIP_PATTERN = TracingFilter.class.getName() + ".skipPattern";

    /**
     * Used as a key of {@link HttpServletRequest#setAttribute(String, Object)} to inject server span context
     */
    public static final String SERVER_SPAN_CONTEXT = TracingFilter.class.getName() + ".activeSpanContext";
    /**
     * Key of {@link HttpServletRequest#setAttribute(String, Object)} with injected span wrapper.
     *
     * <p>This is meant to be used only in higher layers like Spring interceptor to add more data to the span.
     * <p>Do not use this as local span to trace business logic, instead use {@link #SERVER_SPAN_CONTEXT}.
     */
    public static final String SERVER_SPAN_WRAPPER = TracingFilter.class.getName() + ".activeServerSpan";

    private FilterConfig filterConfig;

    private Tracer tracer;
    private List<ServletFilterSpanDecorator> spanDecorators;
    private Pattern skipPattern;

    /**
     * Tracer instance has to be registered with {@link GlobalTracer#register(Tracer)}.
     */
    public TracingFilter() {
        this(GlobalTracer.get());
    }

    /**
     * @param tracer
     */
    public TracingFilter(Tracer tracer) {
        this(tracer, Collections.singletonList(ServletFilterSpanDecorator.STANDARD_TAGS), null);
    }

    /**
     *
     * @param tracer tracer
     * @param spanDecorators decorators
     * @param skipPattern null or pattern to exclude certain paths from tracing e.g. "/health"
     */
    public TracingFilter(Tracer tracer, List<ServletFilterSpanDecorator> spanDecorators, Pattern skipPattern) {
        this.tracer = tracer;
        this.spanDecorators = new ArrayList<>(spanDecorators);
        this.spanDecorators.removeAll(Collections.singleton(null));
        this.skipPattern = skipPattern;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        ServletContext servletContext = filterConfig.getServletContext();

        // use decorators from context attributes
        Object contextAttribute = servletContext.getAttribute(SPAN_DECORATORS);
        if (contextAttribute instanceof Collection) {
            List<ServletFilterSpanDecorator> decorators = new ArrayList<>();
            for (Object decorator: (Collection)contextAttribute) {
                if (decorator instanceof ServletFilterSpanDecorator) {
                    decorators.add((ServletFilterSpanDecorator) decorator);
                } else {
                    log.severe(decorator + " is not an instance of " + ServletFilterSpanDecorator.class);
                }
            }
            this.spanDecorators = decorators.size() > 0 ? decorators : this.spanDecorators;
        }

        contextAttribute = servletContext.getAttribute(SKIP_PATTERN);
        if (contextAttribute instanceof Pattern) {
            skipPattern = (Pattern) contextAttribute;
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        if (!isTraced(httpRequest, httpResponse)) {
            httpRequest.setAttribute(SERVER_SPAN_WRAPPER, new SpanWrapper(null));
            chain.doFilter(httpRequest, httpResponse);
            return;
        }

        /**
         * If request is traced then do not start new span.
         */
        if (servletRequest.getAttribute(SERVER_SPAN_CONTEXT) != null) {
            chain.doFilter(servletRequest, servletResponse);
        } else {
            SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                    new HttpServletRequestExtractAdapter(httpRequest));

            final Span span = tracer.buildSpan(httpRequest.getMethod())
                    .asChildOf(extractedContext)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                    .start();

            final SpanWrapper spanWrapper = new SpanWrapper(span);
            httpRequest.setAttribute(SERVER_SPAN_WRAPPER, spanWrapper);
            httpRequest.setAttribute(SERVER_SPAN_CONTEXT, span.context());

            for (ServletFilterSpanDecorator spanDecorator: spanDecorators) {
                spanDecorator.onRequest(httpRequest, span);
            }

            try {
                chain.doFilter(servletRequest, servletResponse);
                if (!httpRequest.isAsyncStarted()) {
                    for (ServletFilterSpanDecorator spanDecorator : spanDecorators) {
                        spanDecorator.onResponse(httpRequest, httpResponse, span);
                    }
                }
                // catch all exceptions (e.g. RuntimeException, ServletException...)
            } catch (Throwable ex) {
                for (ServletFilterSpanDecorator spanDecorator : spanDecorators) {
                    spanDecorator.onError(httpRequest, httpResponse, ex, span);
                }
                throw ex;
            } finally {
                if (httpRequest.isAsyncStarted()) {
                    // what if async is already finished? This would not be called
                    httpRequest.getAsyncContext()
                            .addListener(new AsyncListener() {
                        @Override
                        public void onComplete(AsyncEvent event) throws IOException {
                            for (ServletFilterSpanDecorator spanDecorator: spanDecorators) {
                                spanDecorator.onResponse((HttpServletRequest) event.getSuppliedRequest(),
                                        (HttpServletResponse) event.getSuppliedResponse(), span);
                            }
                            spanWrapper.finish();
                        }

                        @Override
                        public void onTimeout(AsyncEvent event) throws IOException {
                            for (ServletFilterSpanDecorator spanDecorator: spanDecorators) {
                                spanDecorator.onTimeout((HttpServletRequest) event.getSuppliedRequest(),
                                        (HttpServletResponse) event.getSuppliedResponse(),
                                        event.getAsyncContext().getTimeout(), span);
                            }
                            spanWrapper.finish();
                        }

                        @Override
                        public void onError(AsyncEvent event) throws IOException {
                            for (ServletFilterSpanDecorator spanDecorator: spanDecorators) {
                                spanDecorator.onError((HttpServletRequest) event.getSuppliedRequest(),
                                        (HttpServletResponse) event.getSuppliedResponse(), event.getThrowable(), span);
                            }
                            spanWrapper.finish();
                        }

                        @Override
                        public void onStartAsync(AsyncEvent event) throws IOException {
                        }
                    });
                } else {
                    spanWrapper.finish();
                }
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
        // skip URLs matching skip pattern
        if (skipPattern != null) {
            String url = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());
            return !skipPattern.matcher(url).matches();
        }

        return true;
    }

    /**
     * Get context of server span.
     *
     * @param servletRequest request
     * @return server span context
     */
    public static SpanContext serverSpanContext(ServletRequest servletRequest) {
        return (SpanContext) servletRequest.getAttribute(SERVER_SPAN_CONTEXT);
    }
}
