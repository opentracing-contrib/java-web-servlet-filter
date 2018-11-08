/*
 * Copyright 2016-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.web.servlet.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Filter;

import org.awaitility.Awaitility;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.tag.Tags;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * @author Gary Brown
 */
public class OverrideTracerTest extends AbstractJettyTest {

    @Override
    protected void initServletContext(ServletContextHandler servletContext) {
        // Set MockTracer in the servlet context which should override the NoopTracer
        // associated with the TracingFilter
        servletContext.setAttribute(Tracer.class.getName(), mockTracer);
    }

    @Override
    protected Filter tracingFilter() {
        // Initialize the filter with the NoopTracer
        return new TracingFilter(NoopTracerFactory.create(), Collections.singletonList(ServletFilterSpanDecorator.STANDARD_TAGS),
                Pattern.compile("/health"));
    }

    @Test
    public void testHelloRequest() throws IOException {
        {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(localRequestUrl("/hello"))
                    .build();

            client.newCall(request).execute();
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }

        // Even though the filter was initialized with the NoopTracer, a MockSpan
        // should be reported as the ServletContext was initialized with the MockTracer
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
}
