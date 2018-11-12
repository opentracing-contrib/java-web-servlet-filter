package io.opentracing.contrib.web.servlet.filter.decorator;

import io.opentracing.Span;
import io.opentracing.contrib.mdc.MDCSpanDecorator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServletFilterMDCSpanDecoratorTest {

    @Mock
    private MDCSpanDecorator mdcDecorator;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private Exception exception;
    @Mock
    private Span span;

    private ServletFilterMDCSpanDecorator decorator;

    @Before
    public void init() {
        decorator = new ServletFilterMDCSpanDecorator(mdcDecorator);
    }

    @Test
    public void givenMdcDecorator_whenOnRequestIsCalled_thenDecoratorIsCalled() {
        decorator.onRequest(httpServletRequest, span);

        Mockito.verify(mdcDecorator).decorate(Matchers.same(span));
    }

    @Test
    public void givenMdcDecorator_whenOnRequestIsCalled__thenParamAreNeverUsed() {
        decorator.onRequest(httpServletRequest, span);

        Mockito.verifyZeroInteractions(span);
        Mockito.verifyZeroInteractions(httpServletRequest);
    }

    @Test
    public void givenMdcDecorator_whenOnResponseIsCalled_thenParamAreNeverUsed() {
        decorator.onResponse(httpServletRequest, httpServletResponse, span);

        Mockito.verifyZeroInteractions(span);
        Mockito.verifyZeroInteractions(httpServletResponse);
        Mockito.verifyZeroInteractions(httpServletRequest);
    }

    @Test
    public void givenMdcDecorator_whenOnErrorIsCalled_thenParamAreNeverUsed() {
        decorator.onError(httpServletRequest, httpServletResponse, exception, span);

        Mockito.verifyZeroInteractions(span);
        Mockito.verifyZeroInteractions(httpServletResponse);
        Mockito.verifyZeroInteractions(exception);
        Mockito.verifyZeroInteractions(httpServletRequest);
    }

    @Test
    public void givenMdcDecorator_whenOnTimeoutdIsCalled_thenMdcDecoratorIsNeverUsed() {
        decorator.onTimeout(httpServletRequest, httpServletResponse, 1, span);

        Mockito.verifyZeroInteractions(mdcDecorator);
    }

}
