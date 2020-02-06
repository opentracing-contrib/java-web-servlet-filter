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
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.awaitility.Awaitility;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class NoContextJettyTest extends AbstractJettyTest {

	private int serverPort;

	@Before
	public void beforeTest() throws Exception {
		mockTracer = Mockito.spy(new MockTracer(new ThreadLocalScopeManager(), MockTracer.Propagator.TEXT_MAP));

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(TestServlet.class, "/hello");

		servletHandler.addFilterWithMapping(new FilterHolder(tracingFilter()), "/*", EnumSet.of(DispatcherType.REQUEST,
				DispatcherType.FORWARD, DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.INCLUDE));

		jettyServer = new Server(0);
		jettyServer.setHandler(servletHandler);
		jettyServer.start();
		serverPort = ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
	}

	@Test
	public void testHelloRequest() throws IOException {
		{
			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder().url(localRequestUrl("/hello")).build();

			client.newCall(request).execute();
			Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
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

	public String localRequestUrl(String path) {
		return "http://localhost:" + serverPort + path;
	}
}
