package io.opentracing.contrib.web.servlet.filter.decorator;


import io.opentracing.Span;
import io.opentracing.contrib.web.servlet.filter.decorator.ServletFilterHeaderSpanDecorator.HeaderEntry;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServletFilterHeaderSpanDecoratorTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private Span span;

    private List<HeaderEntry> headerEntries = new ArrayList<>();

    private ServletFilterHeaderSpanDecorator decorator;

    @Before
    public void init() {
        headerEntries.add(new HeaderEntry("If-Match", "if-match"));
        decorator = new ServletFilterHeaderSpanDecorator(headerEntries);
    }

    @Test
    public void givenMatchingHeaderEntry_whenOnRequest_thenItShouldAddTag() {
        Mockito.when(httpServletRequest.getHeader("If-Match")).thenReturn("10");

        decorator.onRequest(httpServletRequest, span);
        Mockito.verify(span).setTag("http.header.if-match", "10");
    }

    @Test
    public void givenNonMatchingHeaderEntry_whenOnRequest_thenItShouldNotAddTag() {
        decorator.onRequest(httpServletRequest, span);
        Mockito.verifyZeroInteractions(span);
    }

    @Test
    public void givenEmptyMatchingHeaderEntry_whenOnRequest_thenItShouldNotAddTag() {
        Mockito.when(httpServletRequest.getHeader("If-Match")).thenReturn("");

        decorator.onRequest(httpServletRequest, span);
        Mockito.verifyZeroInteractions(span);
    }

    @Test
    public void givenCustomTag_whenOnRequest_thenItShouldNotAddTag() {
        decorator = new ServletFilterHeaderSpanDecorator(headerEntries, "HEADER.");
        Mockito.when(httpServletRequest.getHeader("If-Match")).thenReturn("10");

        decorator.onRequest(httpServletRequest, span);
        Mockito.verify(span).setTag("HEADER.if-match", "10");
    }
    
    @Test
    public void givenEmptyCustomTag_whenOnRequest_thenItShouldNotAddTag() {
        decorator = new ServletFilterHeaderSpanDecorator(headerEntries, "");
        Mockito.when(httpServletRequest.getHeader("If-Match")).thenReturn("10");

        decorator.onRequest(httpServletRequest, span);
        Mockito.verify(span).setTag("if-match", "10");
    }

}
