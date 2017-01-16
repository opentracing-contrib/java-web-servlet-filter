package io.opentracing.contrib.web.servlet.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.Before;

import io.opentracing.mock.MockTracer;
import io.opentracing.mock.HttpHeadersPropagator;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractJettyTest {

    // jetty starts on random port
    private int serverPort;

    protected Server jettyServer;
    protected MockTracer mockTracer;

    @Before
    public void beforeTest() throws Exception {
        mockTracer = new MockTracer(new HttpHeadersPropagator());

        ServletContextHandler servletContext = new ServletContextHandler();
        servletContext.setContextPath("/");
        servletContext.addServlet(TestServlet.class, "/hello");
        servletContext.addServlet(ExceptionServlet.class, "/servletException");

        servletContext.addFilter(new FilterHolder(tracingFilter()), "/*", EnumSet.of(DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.INCLUDE));
        servletContext.addFilter(ErrorFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

        jettyServer = new Server(0);
        jettyServer.setHandler(servletContext);
        jettyServer.start();
        serverPort = ((ServerConnector)jettyServer.getConnectors()[0]).getLocalPort();
    }

    @After
    public void afterTest() throws Exception {
        jettyServer.stop();
        jettyServer.join();
    }

    protected Filter tracingFilter() {
        return new TracingFilter(mockTracer, Arrays.asList(SpanDecorator.STANDARD_TAGS), TracingDecision.TRACE_ALL);
    }

    public String localRequestUrl(String path) {
        return "http://localhost:" + serverPort + path;
    }

    public static class TestServlet extends HttpServlet {

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

    public static class ExceptionServlet extends HttpServlet {

        public static final String EXCEPTION_MESSAGE = ExceptionServlet.class.getName() + "message";

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            throw new ServletException(EXCEPTION_MESSAGE);
        }
    }

    public static class ErrorFilter implements Filter {

        public static final String EXCEPTION_MESSAGE = ErrorFilter.class.getName() + "message";

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {}

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpServletRequest = (HttpServletRequest) request;

            if ("/filterException".equals(httpServletRequest.getServletPath())) {
                throw new RuntimeException(EXCEPTION_MESSAGE);
            }

            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {}
    }
}
