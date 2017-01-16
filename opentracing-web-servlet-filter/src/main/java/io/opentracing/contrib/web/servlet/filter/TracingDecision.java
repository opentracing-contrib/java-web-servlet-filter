package io.opentracing.contrib.web.servlet.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface to allow disabling/enabling tracing for specific requests.
 * By default use {@link TracingDecision#TRACE_ALL} which enables tracing for all requests.
 *
 * @author Pavol Loffay
 */
public interface TracingDecision {

    /**
     * This method is called as the first thing in tracing interceptor. It decides whether server request will be
     * traced or not (e.g. tracer.buildSpan() will be called).
     *
     * @param httpServletRequest request
     * @param httpServletResponse response
     * @return whether span should be started or not
     */
    boolean isTraced(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);

    /**
     * Trace all requests.
     */
    TracingDecision TRACE_ALL = new TracingDecision() {
        @Override
        public boolean isTraced(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
            return true;
        }
    };
}
