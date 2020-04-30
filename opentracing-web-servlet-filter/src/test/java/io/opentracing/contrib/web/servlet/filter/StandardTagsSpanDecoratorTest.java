package io.opentracing.contrib.web.servlet.filter;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StandardTagsSpanDecoratorTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private Span span;

    private ServletFilterSpanDecorator standardDecorator = ServletFilterSpanDecorator.STANDARD_TAGS;


    @Test
    public void given4XXResponse_whenOnResponse_thenTagSpanWithError() {
        when(httpServletResponse.getStatus()).thenReturn(HttpServletResponse.SC_NOT_FOUND);

        standardDecorator.onResponse(httpServletRequest, httpServletResponse, span);

        verify(span).setTag(Tags.ERROR.getKey(), true);
    }

    @Test
    public void given5XXResponse_whenOnResponse_thenTagSpanWithError() {
        when(httpServletResponse.getStatus()).thenReturn(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        standardDecorator.onResponse(httpServletRequest, httpServletResponse, span);

        verify(span).setTag(Tags.ERROR.getKey(), true);
    }

    @Test
    public void given3XXResponse_whenOnResponse_thenDontTagSpanWithError() {
        when(httpServletResponse.getStatus()).thenReturn(HttpServletResponse.SC_SEE_OTHER);

        standardDecorator.onResponse(httpServletRequest, httpServletResponse, span);

        verify(span, times(0)).setTag(Tags.ERROR.getKey(), true);
    }

    @Test
    public void given2XXResponse_whenOnResponse_thenDontTagSpanWithError() {
        when(httpServletResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

        standardDecorator.onResponse(httpServletRequest, httpServletResponse, span);

        verify(span, times(0)).setTag(Tags.ERROR.getKey(), true);
    }

}
