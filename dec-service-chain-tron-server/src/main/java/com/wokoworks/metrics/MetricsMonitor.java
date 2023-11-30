package com.wokoworks.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Metrics Monitor.
 *
 */
public class MetricsMonitor {

    public MetricsMonitor() {
    }

    public static void exceptionCounter(String type, String error) {
        Metrics.counter("exception", "type", type, "error", error == null?"":error).increment();
    }
}