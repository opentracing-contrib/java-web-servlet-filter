package io.opentracing.contrib.web.servlet.filter;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;

/**
 * This is passed to {@link ServletContext#setAttribute(String, Object)} holding server span.
 *
 * <p>This wrapper is necessary for higher levels (e.g. spring interceptor, jax-rs) to find out
 * if the span was finished or not.
 *
 * @author Pavol Loffay
 */
public class SpanWrapper {

    private final SpanManager.ManagedSpan managedSpan;
    private final AtomicBoolean finished = new AtomicBoolean();

    /**
     * @param span server span or null when request is not being traced
     */
    protected SpanWrapper(Span span) {
        managedSpan = DefaultSpanManager.getInstance().activate(span);
    }

    /**
     * @return whether request is being traced or not
     */
    public boolean isTraced() {
        return managedSpan.getSpan() != null;
    }

    /**
     * @return server span
     */
    public Span get() {
        return managedSpan.getSpan();
    }

    /**
     * @return true if span has been finished
     */
    public boolean isFinished() {
        return finished.get();
    }

    /**
     * Idempotent finish
     */
    protected void finish() {
        if (managedSpan.getSpan() != null && finished.compareAndSet(false,true)) {
            managedSpan.getSpan().finish();
            managedSpan.deactivate();
        }
    }
}
