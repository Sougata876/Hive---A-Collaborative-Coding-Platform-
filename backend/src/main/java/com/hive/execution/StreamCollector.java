package com.hive.execution;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Drains a process stream on its own thread.
 *
 * <p>Reading stdout and stderr concurrently matters: a program that writes more than the pipe buffer
 * holds would otherwise block forever waiting for the parent to read.
 */
final class StreamCollector implements Runnable {

    private static final String TRUNCATION_NOTICE = "\n[output truncated]";

    private final InputStream stream;
    private final int maxLength;
    private final StringBuilder buffer = new StringBuilder();
    private final Thread thread;
    private boolean truncated;

    private StreamCollector(InputStream stream, int maxLength) {
        this.stream = stream;
        this.maxLength = maxLength;
        this.thread = new Thread(this, "hive-exec-stream");
        this.thread.setDaemon(true);
    }

    static StreamCollector start(InputStream stream, int maxLength) {
        StreamCollector collector = new StreamCollector(stream, maxLength);
        collector.thread.start();
        return collector;
    }

    @Override
    public void run() {
        byte[] chunk = new byte[8192];
        try (InputStream source = stream) {
            int read;
            while ((read = source.read(chunk)) != -1) {
                if (truncated) {
                    // Keep draining so the process never blocks, but stop retaining output.
                    continue;
                }
                String text = new String(chunk, 0, read, StandardCharsets.UTF_8);
                synchronized (buffer) {
                    int remaining = maxLength - buffer.length();
                    if (text.length() >= remaining) {
                        buffer.append(text, 0, Math.max(remaining, 0)).append(TRUNCATION_NOTICE);
                        truncated = true;
                    } else {
                        buffer.append(text);
                    }
                }
            }
        } catch (IOException exception) {
            // The stream closes when the container exits or is killed; whatever we read is kept.
        }
    }

    String join() {
        try {
            thread.join(5000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        synchronized (buffer) {
            return buffer.toString();
        }
    }
}
