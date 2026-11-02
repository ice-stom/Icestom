package io.gitlab.icestom.icestom.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class BatchQueue<T> {

    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    public void add(T item) {
        queue.offer(item);
    }

    public List<T> drain() {
        List<T> batch = new ArrayList<>();

        T item;
        while ((item = queue.poll()) != null) {
            batch.add(item);
        }

        return batch;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}