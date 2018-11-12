package io.opentracing.contrib.web.servlet.filter;

import io.opentracing.Span;
import io.opentracing.contrib.mdc.MDCSpanDecorator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletFilterMDCSpanDecorator implements ServletFilterSpanDecorator {

    private final MDCSpanDecorator spanDecorator;

    public ServletFilterMDCSpanDecorator(MDCSpanDecorator spanDecorator) {
        this.spanDecorator = spanDecorator;
    }

    @Override
    public void onRequest(HttpServletRequest httpServletRequest, Span span) {
        spanDecorator.decorate(span);
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

    public MDCSpanDecorator getSpanDecorator() {
        return spanDecorator;
    }

}
