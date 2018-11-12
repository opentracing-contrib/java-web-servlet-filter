package io.opentracing.contrib.mdc;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import io.opentracing.Span;
import io.opentracing.contrib.mdc.MDCSpanDecorator.MDCEntry;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.MDC;

@RunWith(MockitoJUnitRunner.class)
public class MDCSpanDecoratorTest {

    @Mock
    private Span span;

    private List<MDCEntry> mdcEntries = new ArrayList<>();
    private MDCSpanDecorator decorator;

    @Before
    public void init() {
        MDC.clear();
        mdcEntries.add(new MDCEntry("key1", "tag"));
        decorator = new MDCSpanDecorator(mdcEntries);
    }

    @After
    public void after() {
        MDC.clear();
    }

    @Test
    public void givenMdcEntry_whenDecorathingSpan_thenItShouldAddTag() {
        MDC.put("key1", "val1");
        decorator.decorate(span);
        verify(span).setTag("mdc.tag", "val1");
    }

    @Test
    public void givenMdcEntry_whenDecorathingSpan_thenItShouldIgnoreUnpappedTag() {
        MDC.put("key1", "val1");
        MDC.put("key2", "val2");
        decorator.decorate(span);
        verify(span).setTag(anyString(), anyString());
    }
    
    @Test
    public void givenUnmappedMdcEntry_whenDecorathingSpan_thenItShouldIgnoreUnmappedTag() {
        MDC.put("key2", "val2");
        decorator.decorate(span);
        verifyZeroInteractions(span);
    }
    
    @Test
    public void givenEmptyMdcEntry_whenDecorathingSpan_thenItShouldIgnoreUnMappedTag() {
        decorator = new MDCSpanDecorator(new ArrayList<MDCEntry>());
        decorator.decorate(span);
        verifyZeroInteractions(span);
    }
    
    @Test
    public void givenCustomTag_whenDecorathingSpan_thenItShouldTag() {
        MDC.put("key1", "val1");
        decorator = new MDCSpanDecorator(mdcEntries, "");
        decorator.decorate(span);
        verify(span).setTag("tag", "val1");
    }

}
