package io.opentracing.contrib.web.servlet.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.opentracing.mock.MockSpan;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * @author Pavol Loffay
 */
public class TracingFilterTest extends AbstractJettyTest {

    @Test
    public void testHelloRequest() throws IOException {
        {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(localRequestUrl("/hello"))
                    .build();

            client.newCall(request).execute();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(5, mockSpan.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(localRequestUrl("/hello"), mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(202, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals("java-web-servlet", mockSpan.tags().get(Tags.COMPONENT.getKey()));
    }

    @Test
    public void testLocalSpan() throws IOException {
        {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(localRequestUrl("/localSpan"))
                    .build();

            client.newCall(request).execute();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
        assertOnErrors(mockSpans);

        Assert.assertEquals(mockSpans.get(0).context().traceId(), mockSpans.get(1).context().traceId());
        Assert.assertEquals(mockSpans.get(0).parentId(), mockSpans.get(1).context().spanId());
    }

    @Test
    public void testNotExistingUrl() throws IOException {
        {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(localRequestUrl("/doesNotExist"))
                    .build();

            client.newCall(request).execute();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(5, mockSpan.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(localRequestUrl("/doesNotExist"), mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(404, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals("java-web-servlet", mockSpan.tags().get(Tags.COMPONENT.getKey()));
    }

    @Test
    public void testFilterException() throws IOException {
        {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(localRequestUrl("/filterException"))
                    .build();

            client.newCall(request).execute();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(6, mockSpan.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(localRequestUrl("/filterException"), mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(500, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals("java-web-servlet", mockSpan.tags().get(Tags.COMPONENT.getKey()));

        Assert.assertEquals(1, mockSpan.logEntries().size());
        Assert.assertEquals(3, mockSpan.logEntries().get(0).fields().size());
        Assert.assertEquals(Tags.ERROR.getKey(), mockSpan.logEntries().get(0).fields().get("event"));
        Assert.assertEquals(ErrorFilter.EXCEPTION_MESSAGE,
                mockSpan.logEntries().get(0).fields().get("message"));
        Assert.assertNotNull(mockSpan.logEntries().get(0).fields().get("stack"));
    }

    @Test
    public void testServletException() throws IOException {
        {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(localRequestUrl("/servletException"))
                    .build();

            client.newCall(request).execute();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(6, mockSpan.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(localRequestUrl("/servletException"), mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(500, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals("java-web-servlet", mockSpan.tags().get(Tags.COMPONENT.getKey()));

        Assert.assertEquals(1, mockSpan.logEntries().size());
        Assert.assertEquals(3, mockSpan.logEntries().get(0).fields().size());
        Assert.assertEquals(Tags.ERROR.getKey(), mockSpan.logEntries().get(0).fields().get("event"));
        Assert.assertEquals(ExceptionServlet.EXCEPTION_MESSAGE,
                mockSpan.logEntries().get(0).fields().get("message"));
        Assert.assertNotNull(mockSpan.logEntries().get(0).fields().get("stack"));
    }

    @Test
    public void testSpanContextPropagation() throws IOException {
        MockSpan foo = (MockSpan) mockTracer.buildSpan("foo").start();
        {
            Map<String, String> injectMap = new HashMap<>();
            mockTracer.inject(foo.context(), Format.Builtin.HTTP_HEADERS, new TextMapInjectAdapter(injectMap));

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(localRequestUrl("/hello"))
                    .headers(Headers.of(injectMap))
                    .build();

            client.newCall(request).execute();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals(foo.context().spanId(), mockSpan.parentId());
        Assert.assertEquals(foo.context().traceId(), mockSpan.context().traceId());
    }

    public static void assertOnErrors(List<MockSpan> spans) {
        for (MockSpan mockSpan: spans) {
            Assert.assertEquals(mockSpan.generatedErrors().toString(), 0, mockSpan.generatedErrors().size());
        }
    }
}
