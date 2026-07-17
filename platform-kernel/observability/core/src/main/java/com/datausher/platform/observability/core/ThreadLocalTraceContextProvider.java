package com.datausher.platform.observability.core;

import com.datausher.platform.observability.api.TraceContext;
import com.datausher.platform.observability.api.TraceContextManager;

import java.util.Objects;

public final class ThreadLocalTraceContextProvider implements TraceContextManager {
    private final ThreadLocal<Frame> current = new ThreadLocal<>();

    @Override
    public TraceContext currentTraceContext() {
        Frame frame = current.get();
        return frame == null ? TraceContext.EMPTY : frame.traceContext();
    }

    @Override
    public Scope attach(TraceContext traceContext) {
        TraceContext context = Objects.requireNonNull(traceContext, "traceContext must not be null");
        Frame frame = new Frame(context, current.get());
        current.set(frame);
        return new ThreadLocalScope(this, frame, Thread.currentThread());
    }

    private void close(Frame frame, Thread owner) {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("trace context scope must be closed by its owning thread");
        }
        if (current.get() != frame) {
            throw new IllegalStateException("trace context scopes must be closed in reverse order");
        }
        if (frame.previous() == null) {
            current.remove();
        } else {
            current.set(frame.previous());
        }
    }

    private record Frame(TraceContext traceContext, Frame previous) {
    }

    private static final class ThreadLocalScope implements Scope {
        private final ThreadLocalTraceContextProvider provider;
        private final Frame frame;
        private final Thread owner;
        private boolean closed;

        private ThreadLocalScope(ThreadLocalTraceContextProvider provider, Frame frame, Thread owner) {
            this.provider = provider;
            this.frame = frame;
            this.owner = owner;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            provider.close(frame, owner);
            closed = true;
        }
    }
}
